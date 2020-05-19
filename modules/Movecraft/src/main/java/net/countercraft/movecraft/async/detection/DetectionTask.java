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
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DetectionTask extends AsyncTask {
    @NotNull private final MovecraftLocation startLocation;
    private final int minSize;
    private final int maxSize;
    @NotNull private final Stack<MovecraftLocation> blockStack = new Stack<>();
    @NotNull private final BitmapHitBox fluidBox = new BitmapHitBox();
    @NotNull private final BitmapHitBox hitBox = new BitmapHitBox();
    @NotNull private final HashSet<MovecraftLocation> visited = new HashSet<>();
    @NotNull private final HashMap<List<Integer>, Integer> blockTypeCount = new HashMap<>();
    @NotNull private final World world;
    @Nullable private final Player player;
    @NotNull private final Player notificationPlayer;
    private final int[] allowedBlocks;
    private final int[] forbiddenBlocks;
    @NotNull private final String[] forbiddenSignStrings;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int minY;
    @NotNull private Map<List<Integer>, List<Double>> dFlyBlocks = new HashMap<>();
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
        Map<List<Integer>, List<Double>> flyBlocks = getCraft().getType().getFlyBlocks();
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
            for (List<Integer> i : flyBlocks.keySet()) {
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

            int testID = 0;
            int testData = 0;
            try {
                testData = world.getBlockAt(x, y, z).getData();
                testID = world.getBlockTypeIdAt(x, y, z);
            } catch (Exception e) {
                fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), maxSize));
            }

            if ((testID == 8) || (testID == 9)) {
                waterContact = true;
            }
            if (testID == 63 || testID == 68) {
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
            if (isForbiddenBlock(testID, testData)) {
                fail(I18nSupport.getInternationalisedString("Detection - Forbidden block found"));
            } else if (isAllowedBlock(testID, testData)) {
                // check for double chests
                if (testID == 54) {
                    boolean foundDoubleChest = false;
                    if (world.getBlockTypeIdAt(x - 1, y, z) == 54) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x + 1, y, z) == 54) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x, y, z - 1) == 54) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x, y, z + 1) == 54) {
                        foundDoubleChest = true;
                    }
                    if (foundDoubleChest) {
                        fail(I18nSupport.getInternationalisedString(
                                "Detection - ERROR: Double chest found"));
                    }
                }
                // check for double trapped chests
                if (testID == 146) {
                    boolean foundDoubleChest = false;
                    if (world.getBlockTypeIdAt(x - 1, y, z) == 146) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x + 1, y, z) == 146) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x, y, z - 1) == 146) {
                        foundDoubleChest = true;
                    }
                    if (world.getBlockTypeIdAt(x, y, z + 1) == 146) {
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
                    if (testID == 8 || testID == 9 || testID == 10 || testID == 11) {
                        fluidBox.add(workingLocation);
                    }
                    addToBlockList(workingLocation);
                    Integer blockID = testID;
                    Integer dataID = testData;
                    Integer shiftedID = (blockID << 4) + dataID + 10000;
                    for (List<Integer> flyBlockDef : dFlyBlocks.keySet()) {
                        if (flyBlockDef.contains(blockID) || flyBlockDef.contains(shiftedID)) {
                            addToBlockCount(flyBlockDef);
                        } else {
                            addToBlockCount(null);
                        }
                    }
                    if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
                        if (blockID == getCraft().getType().getDynamicFlyBlock()) {
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

    private boolean isAllowedBlock(int test, int testData) {

        for (int i : allowedBlocks) {
            if ((i == test) || (i == (test << 4) + testData + 10000)) {
                return true;
            }
        }

        return false;
    }

    private boolean isForbiddenBlock(int test, int testData) {

        for (int i : forbiddenBlocks) {
            if ((i == test) || (i == (test << 4) + testData + 10000)) {
                return true;
            }
        }

        return false;
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

    private void addToBlockCount(List<Integer> id) {
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

    private boolean confirmStructureRequirements(Map<List<Integer>, List<Double>> flyBlocks,
                                                 Map<List<Integer>, Integer> countData) {
        if (getCraft().getType().getRequireWaterContact() && !waterContact) {
            fail(I18nSupport.getInternationalisedString("Detection - Failed - Water contact required but not found"));
            return false;
        }
        for (List<Integer> i : flyBlocks.keySet()) {
            Integer numberOfBlocks = countData.get(i);

            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }

            float blockPercentage = (((float) numberOfBlocks / hitBox.size()) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    if (i.get(0) < 10000) {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), blockPercentage,
                                minPercentage));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                blockPercentage, minPercentage));
                        return false;
                    }
                }
            } else {
                if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                    if (i.get(0) < 10000) {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %d < %d",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), numberOfBlocks,
                                flyBlocks.get(i).get(0).intValue() - 10000));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Not enough flyblock") + ": %s %d < %d",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                numberOfBlocks, flyBlocks.get(i).get(0).intValue() - 10000));
                        return false;
                    }
                }
            }
            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    if (i.get(0) < 10000) {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), blockPercentage,
                                maxPercentage));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                blockPercentage, maxPercentage));
                        return false;
                    }
                }
            } else {
                if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                    if (i.get(0) < 10000) {
                        fail(String.format(I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %d > %d",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), numberOfBlocks,
                                flyBlocks.get(i).get(1).intValue() - 10000));
                        return false;
                    } else {
                        fail(String.format(I18nSupport.getInternationalisedString("Detection - Too much flyblock") + ": %s %d > %d",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                numberOfBlocks, flyBlocks.get(i).get(1).intValue() - 10000));
                        return false;
                    }
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

    public int[] getAllowedBlocks() {
        return allowedBlocks;
    }

    public int[] getForbiddenBlocks() {
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