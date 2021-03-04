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


import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BitmapHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MutableHitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class DetectionTask extends AsyncTask {
    @NotNull private final MovecraftLocation startLocation;
    private final int minSize;
    private final int maxSize;
    @NotNull private final Stack<MovecraftLocation> blockStack = new Stack<>();
    @NotNull private final MutableHitBox fluidBox = new BitmapHitBox();
    @NotNull private final MutableHitBox hitBox = new BitmapHitBox();
    @NotNull private final HashSet<MovecraftLocation> visited = new HashSet<>();
    @NotNull private final HashMap<List<Material>, Integer> blockTypeCount = new HashMap<>();
    @NotNull private final World world;
    @Nullable private final Player player;
    @NotNull private final Player notificationPlayer;
    private final EnumSet<Material> allowedBlocks;
    private final EnumSet<Material> forbiddenBlocks;
    @NotNull private final Set<String> forbiddenSignStrings;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int minY;
    @NotNull private Map<List<Material>, List<Double>> dFlyBlocks = new HashMap<>();
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
        this.world = craft.getW();
        this.player = player;
        this.notificationPlayer = notificationPlayer;
        this.allowedBlocks = craft.getType().getAllowedBlocks();
        this.forbiddenBlocks = craft.getType().getForbiddenBlocks();
        this.forbiddenSignStrings = craft.getType().getForbiddenSignStrings();
    }

    @Override
    public void execute() {
        Map<List<Material>, List<Double>> flyBlocks = getCraft().getType().getFlyBlocks();
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
            for (List<Material> i : flyBlocks.keySet()) {
                if (i.contains(getCraft().getType().getDynamicFlyBlock()))
                    foundMinimum = flyBlocks.get(i).get(0);
            }
            ratio = ratio - (foundMinimum / 100.0);
            ratio = ratio * getCraft().getType().getDynamicFlyBlockSpeedFactor();
            dynamicFlyBlockSpeedMultiplier = ratio;
        }
        if (!isWithinLimit(hitBox.size(), minSize, maxSize)) {
            return;
        }
        confirmStructureRequirements(flyBlocks, blockTypeCount);
    }

    private void detectBlock(int x, int y, int z) {

        MovecraftLocation workingLocation = new MovecraftLocation(x, y, z);

        if (notVisited(workingLocation, visited)) {

            Material testMaterial = null;
            BlockData testData = null;
            BlockState testState = null;
            try {
                Block testBlock = world.getBlockAt(x, y, z);
                testMaterial = testBlock.getType();
                testData = testBlock.getBlockData();
                testState = testBlock.getState();
            } catch (Exception e) {
                fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), maxSize));
            }

            if (testMaterial == Material.WATER) {
                waterContact = true;
            }
            if (testState instanceof Sign) {
                BlockState state = world.getBlockAt(x, y, z).getState();
                if (state instanceof Sign) {
                    Sign s = (Sign) state;
                    if (s.getLine(0).equalsIgnoreCase("Pilot:") && player != null) {
                        String playerName = player.getName();
                        boolean foundPilot = false;
                        if (s.getLine(1).equalsIgnoreCase(playerName) || s.getLine(2).equalsIgnoreCase(playerName)
                                || s.getLine(3).equalsIgnoreCase(playerName)) {
                            foundPilot = true;
                        }
                        if (!foundPilot && (!player.hasPermission("movecraft.bypasslock"))) {
                            fail(I18nSupport.getInternationalisedString(
                                    "Detection - Not Registered Pilot"));
                        }
                    }
                    for (int i = 0; i < 4; i++) {
                        if (isForbiddenSignString(s.getLine(i))) {
                            fail(I18nSupport.getInternationalisedString(
                                    "Detection - Forbidden sign string found"));
                        }
                    }
                    if (s.getLine(0).equalsIgnoreCase("Name:") && !craft.getType().getCanBeNamed()){
                        fail(I18nSupport.getInternationalisedString("Detection - Craft Type Cannot Be Named"));
                    }
                }
            }
            if (isForbiddenBlock(testMaterial)) {
                fail(I18nSupport.getInternationalisedString("Detection - Forbidden block found"));
            } else if (isAllowedBlock(testMaterial)) {
                // check for double chests
                if (testMaterial == Material.CHEST || testMaterial == Material.TRAPPED_CHEST) {
                    boolean foundDoubleChest = false;
                    if (world.getBlockAt(x - 1, y, z).getType() == testMaterial) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockAt(x + 1, y, z).getType() == testMaterial) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockAt(x, y, z - 1).getType() == testMaterial) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockAt(x, y, z + 1).getType() == testMaterial) {
                        foundDoubleChest = true;
                    }
                    if (foundDoubleChest) {
                        fail(I18nSupport.getInternationalisedString(
                                "Detection - ERROR: Double chest found"));
                    }
                }



                Location loc = new Location(world, x, y, z);
                Player p;
                if (player == null) {
                    p = notificationPlayer;
                } else {
                    p = player;
                }
                if (p != null) {
                    if (testMaterial == Material.WATER || testMaterial == Material.LAVA) {
                        fluidBox.add(workingLocation);
                    }
                    addToBlockList(workingLocation);
                    for (List<Material> flyBlockDef : dFlyBlocks.keySet()) {
                        if (flyBlockDef.contains(testMaterial)) {
                            addToBlockCount(flyBlockDef);
                        } else {
                            addToBlockCount(null);
                        }
                    }
                    if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
                        if (testMaterial == getCraft().getType().getDynamicFlyBlock()) {
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

    private boolean isAllowedBlock(Material test) {
        return allowedBlocks.contains(test);
    }

    private boolean isForbiddenBlock(Material test) {
        return forbiddenBlocks.contains(test);
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

    private void addToBlockCount(List<Material> id) {
        Integer count = blockTypeCount.get(id);

        if (count == null) {
            count = 0;
        }

        blockTypeCount.put(id, count + 1);
    }

    private void detectSurrounding(MovecraftLocation l) {
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

    private boolean confirmStructureRequirements(Map<List<Material>, List<Double>> flyBlocks,
                                                 Map<List<Material>, Integer> countData) {
        if (getCraft().getType().getRequireWaterContact() && !waterContact) {
            fail(I18nSupport.getInternationalisedString("Detection - Failed - Water contact required but not found"));
            return false;
        }
        for (List<Material> i : flyBlocks.keySet()) {
            Integer numberOfBlocks = countData.get(i);

            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }

            float blockPercentage = (((float) numberOfBlocks / hitBox.size()) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                            i.get(0).name().toLowerCase().replace("_", " "), blockPercentage,
                            minPercentage));
                    return false;
                }
            } else {
                if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %d < %d",
                            i.get(0).name().toLowerCase().replace("_", " "), numberOfBlocks,
                            flyBlocks.get(i).get(0).intValue() - 10000));
                    return false;
                }
            }
            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    fail(String.format(
                            I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %.2f%% > %.2f%%",
                            i.get(0).name().toLowerCase().replace("_", " "), blockPercentage,
                            maxPercentage));
                    return false;
                }
            } else {
                if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                    fail(String.format(I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %d > %d",
                            i.get(0).name().toLowerCase().replace("_", " "), numberOfBlocks,
                            flyBlocks.get(i).get(1).intValue() - 10000));
                    return false;
                }
            }
        }

        return true;
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

    public EnumSet<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    public EnumSet<Material> getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    @NotNull
    public Set<String> getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public double getDynamicFlyBlockSpeedMultiplier() {
        return dynamicFlyBlockSpeedMultiplier;
    }

    public boolean failed() {
        return failed;
    }

    @NotNull
    public MutableHitBox getHitBox() {
        return hitBox;
    }

    @NotNull
    public MutableHitBox getFluidBox() {
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
