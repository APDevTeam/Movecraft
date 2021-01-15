/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.async.detection;


import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftBlock;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class DetectionTask extends AsyncTask {
    @NotNull private final MovecraftLocation startLocation;
    private final int minSize;
    private final int maxSize;
    @NotNull private final Stack<MovecraftLocation> blockStack = new Stack<>();
    @NotNull private final BitmapHitBox fluidBox = new BitmapHitBox();
    @NotNull private final BitmapHitBox hitBox = new BitmapHitBox();
    @NotNull private final HashSet<MovecraftLocation> visited = new HashSet<>();
    @NotNull private final HashMap<Set<MovecraftBlock>, Integer> blockTypeCount = new HashMap<>();
    @NotNull private final HashMap<List<String>, Integer> signWithStringCount = new HashMap<>();
    @NotNull private final HashMap<List<String>, Integer> cannonsCount = new HashMap<>();
    @NotNull private final World world;
    @Nullable private final Player player;
    @NotNull private final Player notificationPlayer;
    @NotNull private final BlockContainer allowedBlocks;
    @NotNull private final BlockContainer forbiddenBlocks;
    @NotNull private final String[] forbiddenSignStrings;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int minY;
    @NotNull private BlockLimitManager dFlyBlocks;
    private int foundDynamicFlyBlock = 0;
    private double dynamicFlyBlockSpeedMultiplier;
    private boolean failed;
    private boolean waterContact;
    @NotNull private String failMessage = "";

    public DetectionTask(Craft c, @NotNull MovecraftLocation startLocation, @Nullable Player player, @NotNull Player notificationPlayer) {
        super(c);
        this.startLocation = startLocation;
        this.minSize = craft.getType().getMinSize();
        this.maxSize = craft.getType().getMaxSize();
        this.world = craft.getWorld();
        this.player = player;
        this.notificationPlayer = notificationPlayer;
        this.allowedBlocks = craft.getType().getAllowedBlocks();
        this.forbiddenBlocks = craft.getType().getForbiddenBlocks();
        this.forbiddenSignStrings = craft.getType().getForbiddenSignStrings();
    }

    @Override
    public void execute() throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        BlockLimitManager flyBlocks = getCraft().getType().getFlyBlocks();
        dFlyBlocks = flyBlocks;
        blockStack.push(startLocation);
        do {
            detectSurrounding(blockStack.pop());
        } while (!blockStack.isEmpty());
        if (failed) {
            return;
        }
        if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
            int totalBlocks = hitBox.size();
            double ratio = (double) foundDynamicFlyBlock / totalBlocks;
            double foundMinimum = 0.0;
            for (BlockLimitManager.Entry entry : flyBlocks.getEntries()){
                for (MovecraftBlock block : entry.getBlocks()){
                    if (!getCraft().getType().getDynamicFlyBlocks().contains(block.getType())){
                        continue;
                    }
                    foundMinimum = entry.getLowerLimit();
                }
            }
            ratio = ratio - (foundMinimum / 100.0);
            ratio = ratio * getCraft().getType().getDynamicFlyBlockSpeedFactor();
            dynamicFlyBlockSpeedMultiplier = ratio;
        }
        if (!isWithinLimit(hitBox.size(), minSize, maxSize)) {
            return;
        }
        if (!confirmStructureRequirements(flyBlocks, blockTypeCount)) {
            return;
        }
        checkMaxSignLimits();
        long endTime = System.currentTimeMillis();
        if (Settings.Debug){
            Bukkit.broadcastMessage("Detection took (ms): " + (endTime - startTime));
        }
    }

    private void detectBlock(int x, int y, int z) throws ExecutionException, InterruptedException {

        MovecraftLocation workingLocation = new MovecraftLocation(x, y, z);

        if (notVisited(workingLocation, visited)) {

            Material testType;
            int testData;
            Block testBlock;
            try {
                testBlock = world.getBlockAt(x, y, z);
                testData = testBlock.getData();
                testType = testBlock.getType();
            } catch (Exception e) {
                fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), maxSize));
                return;
            }

            if ((testType == Material.WATER) || (testType == LegacyUtils.STATIONARY_WATER)) {
                waterContact = true;
            }
            if (testType.name().endsWith("SIGN") || testType == LegacyUtils.SIGN_POST) {
                Sign sign = Bukkit.getScheduler().callSyncMethod(Movecraft.getInstance(), () -> (Sign) testBlock.getState()).get();
                if (sign.getLine(0).equalsIgnoreCase("Pilot:") && player != null) {
                    String playerName = player.getName();
                    boolean foundPilot = false;
                    final String[] lines = sign.getLines();
                    for (int i = 1 ; i < lines.length ; i++) {
                        if (!ChatColor.stripColor(lines[i]).equalsIgnoreCase(playerName))
                            continue;
                        foundPilot = true;
                        break;
                    }
                    if (!foundPilot && (!player.hasPermission("movecraft.bypasslock"))) {
                        fail(I18nSupport.getInternationalisedString(
                                "Detection - Not Registered Pilot"));
                    }
                    String previous = "";
                    for (String line : lines) {
                        if (line.length() == 0) {
                            continue;
                        }
                        line = ChatColor.stripColor(line);
                        if (isForbiddenSignString(line)) {
                            fail(I18nSupport.getInternationalisedString("Detection - Forbidden sign string found"));
                            return;
                        }
                        if (previous.equals(line)) {
                            continue;
                        }
                        for (List<String> limitedStrings : craft.getType().getMaxSignsWithString().keySet()) {
                            if (!limitedStrings.contains(line.toLowerCase())) {
                                continue;
                            }
                            int count = signWithStringCount.getOrDefault(limitedStrings, 0);
                            count++;
                            signWithStringCount.put(limitedStrings, count);
                            previous = line;
                        }

                    }
                }
            }
            if (isForbiddenBlock(testType, testData)) {
                fail(I18nSupport.getInternationalisedString("Detection - Forbidden block found"));
            } else if (isAllowedBlock(testType, testData)) {
                Location loc = new Location(world, x, y, z);
                Player p;
                if (player == null) {
                    p = notificationPlayer;
                } else {
                    p = player;
                }
                if (p != null) {
                    if (testType.name().endsWith("WATER") || testType.name().endsWith("LAVA")) {
                        fluidBox.add(workingLocation);
                    }
                    addToBlockList(workingLocation);
                    Material blockType = testType;
                    byte dataID = (byte) testData;
                    if (dFlyBlocks.hasMetaData(blockType) && dFlyBlocks.contains(blockType, dataID)){
                        addToBlockCount(dFlyBlocks.get(blockType, dataID).getBlocks());
                    } else if (dFlyBlocks.contains(blockType)) {
                        addToBlockCount(dFlyBlocks.get(blockType).getBlocks());
                    }
                    if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
                        if (getCraft().getType().getDynamicFlyBlocks().contains(blockType)) {
                            foundDynamicFlyBlock++;
                        }
                    }

                    if (isWithinLimit(hitBox.size(), 0, maxSize)) {

                        addToDetectionStack(workingLocation);

                        calculateBounds(workingLocation);

                    }
                }
            }
        }
    }

    private boolean isAllowedBlock(Material test, int testData) {
        return allowedBlocks.contains(test) || allowedBlocks.contains(test, (byte) testData);
    }

    private boolean isForbiddenBlock(Material test, int testData) {
        return forbiddenBlocks.contains(test) || forbiddenBlocks.contains(test, (byte) testData);
    }

    private boolean isForbiddenSignString(String testString) {

        for (String s : forbiddenSignStrings) {
            if (testString.equalsIgnoreCase(s)) {
                return true;
            }
        }

        return false;
    }

    private boolean notVisited(MovecraftLocation l, HashSet<MovecraftLocation> locations) {
        if (locations.contains(l)) {
            return false;
        } else {
            locations.add(l);
            return true;
        }
    }

    private void addToBlockList(MovecraftLocation l) {
        hitBox.add(l);
    }

    private void addToDetectionStack(MovecraftLocation l) {
        blockStack.push(l);
    }

    private void addToBlockCount(Set<MovecraftBlock> id) {
        Integer count = blockTypeCount.get(id);

        if (count == null) {
            count = 0;
        }

        blockTypeCount.put(id, count + 1);
    }

    private void detectSurrounding(MovecraftLocation l) throws ExecutionException, InterruptedException {
        int x = l.getX();
        int y = l.getY();
        int z = l.getZ();

        for (int xMod = -1; xMod < 2; xMod += 2) {

            for (int yMod = -1; yMod < 2; yMod++) {

                detectBlock(x + xMod, y + yMod, z);

            }

        }

        for (int zMod = -1; zMod < 2; zMod += 2) {

            for (int yMod = -1; yMod < 2; yMod++) {

                detectBlock(x, y + yMod, z + zMod);

            }

        }

        for (int yMod = -1; yMod < 2; yMod += 2) {

            detectBlock(x, y + yMod, z);

        }

    }

    private void calculateBounds(MovecraftLocation l) {
        if (l.getX() > maxX) {
            maxX = l.getX();
        }
        if (l.getY() > maxY) {
            maxY = l.getY();
        }
        if (l.getZ() > maxZ) {
            maxZ = l.getZ();
        }/*
        if (data.getMinX() == null || l.getX() < data.getMinX()) {
            data.setMinX(l.getX());
        }*/
        if (l.getY() < minY) {
            minY = l.getY();
        }/*
        if (data.getMinZ() == null || l.getZ() < data.getMinZ()) {
            data.setMinZ(l.getZ());
        }*/
    }

    private boolean isWithinLimit(int size, int min, int max) {
        if (size < min) {
            fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too small"), min));
            return false;
        } else if (size > max) {
            fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), max));
            return false;
        } else {
            return true;
        }

    }

    private boolean confirmStructureRequirements(BlockLimitManager flyBlocks,
                                                 HashMap<Set<MovecraftBlock>, Integer> countData) {
        if (getCraft().getType().getRequireWaterContact() && !waterContact) {
            fail(I18nSupport.getInternationalisedString("Detection - Failed - Water contact required but not found"));
            return false;
        }
        for (BlockLimitManager.Entry i : flyBlocks.getEntries()) {
            Integer numberOfBlocks = countData.get(i.getBlocks());
            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }
            float blockPercentage = (((float) numberOfBlocks / hitBox.size()) * 100);
            double minPercentage = i.getLowerLimit();
            double maxPercentage = i.getUpperLimit();
            ArrayList<MovecraftBlock> flyBlockTypes = new ArrayList<>(i.getBlocks());
            if (flyBlockTypes.isEmpty())
                continue;
            String name = flyBlockTypes.get(0).getType().name();
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {

                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                            name.toLowerCase().replace("_", " "),
                            blockPercentage, minPercentage));
                    return false;

                }
            } else {
                if (numberOfBlocks < i.getLowerLimit() - 10000.0) {

                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %d < %d",
                            name.toLowerCase().replace("_", " "),
                            numberOfBlocks, (int) i.getLowerLimit() - 10000));
                    return false;

                }
            }
            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %.2f%% > %.2f%%",
                            name.toLowerCase().replace("_", " "), blockPercentage,
                            maxPercentage));
                    return false;
                }
            } else {
                if (numberOfBlocks > i.getUpperLimit() - 10000.0) {
                    fail(String.format(I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %d > %d",
                            name.toLowerCase().replace("_", " "), numberOfBlocks,
                            (int) (i.getUpperLimit() - 10000)));
                    return false;

                }
            }
        }

        return true;
    }

    private void checkMaxSignLimits() {
        if (signWithStringCount.isEmpty()) {
            return;
        }
        for (Map.Entry<List<String>, Double> entry : craft.getType().getMaxSignsWithString().entrySet()) {
            final double limit = entry.getValue();
            int count = signWithStringCount.getOrDefault(entry.getKey(), 0);
            if (limit >= 10000.0) {
                int max = (int) (limit - 10000.0);
                if (count <= max) {
                    continue;
                }
                fail(I18nSupport.getInternationalisedString("Detection - Too many signs with string") + " " + String.join(", ", entry.getKey()) + ": " + count + " > " + max);
            } else {
                int maxCount = (int) (hitBox.size() * (limit / 100.0));
                //double ratio = ((double) count / (double) hitBox.size()) * 100.0;
                if (count <= maxCount) {
                    continue;
                }
                fail(I18nSupport.getInternationalisedString("Detection - Too many signs with string") + " " + String.join(", ", entry.getKey()) + ": " + count + "% > " + maxCount + "%");
            }
        }
    }

    private void fail(String message) {
        failed = true;
        failMessage = message;
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    @NotNull
    public Player getNotificationPlayer() {
        return notificationPlayer;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public BlockContainer getAllowedBlocks() {
        return allowedBlocks;
    }

    public BlockContainer getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    @NotNull
    public String[] getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public double getDynamicFlyBlockSpeedMultiplier() {
        return dynamicFlyBlockSpeedMultiplier;
    }

    public boolean failed() {
        return failed;
    }

    @NotNull
    public BitmapHitBox getHitBox() {
        return hitBox;
    }

    @NotNull
    public BitmapHitBox getFluidBox() {
        return fluidBox;
    }

    public boolean isWaterContact() {
        return waterContact;
    }

    @NotNull
    public String getFailMessage() {
        return failMessage;
    }
}
