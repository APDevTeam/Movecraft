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
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.*;

public class DetectionTask extends AsyncTask {
    private final MovecraftLocation startLocation;
    private final int minSize;
    private final int maxSize;
    private final Stack<MovecraftLocation> blockStack = new Stack<>();
    private final HashHitBox blockList = new HashHitBox();
    private final HashSet<MovecraftLocation> visited = new HashSet<>();
    private final HashMap<Map<Material, List<Integer>>, Integer> blockTypeCount = new HashMap<Map<Material, List<Integer>>, Integer>();
    private final DetectionTaskData data;
    private final World world;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int minY;
    private Map<Map<Material, List<Integer>>, List<Double>> dFlyBlocks;
    private int foundDynamicFlyBlock = 0;

    public DetectionTask(Craft c, MovecraftLocation startLocation, Player player) {
        super(c);
        this.startLocation = startLocation;
        this.minSize = craft.getType().getMinSize();
        this.maxSize = craft.getType().getMaxSize();
        this.world = craft.getW();
        data = new DetectionTaskData(craft.getW(), player, craft.getNotificationPlayer(), craft.getType().getAllowedBlocks(), craft.getType().getForbiddenBlocks(),
                craft.getType().getForbiddenSignStrings());
    }

    @Override
    public void execute() {
        Map<Map<Material, List<Integer>>, List<Double>> flyBlocks = getCraft().getType().getFlyBlocks();
        dFlyBlocks = flyBlocks;
        blockStack.push(startLocation);
        do {
            detectSurrounding(blockStack.pop());
        } while (!blockStack.isEmpty());
        if (data.failed()) {
            return;
        }
        if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
            int totalBlocks = blockList.size();
            double ratio = (double) foundDynamicFlyBlock / totalBlocks;
            double foundMinimum = 0.0;
            for (Map<Material, List<Integer>> i : flyBlocks.keySet()) {
                if (i.containsKey(getCraft().getType().getDynamicFlyBlock()))
                    foundMinimum = flyBlocks.get(i).get(0);
            }
            ratio = ratio - (foundMinimum / 100.0);
            ratio = ratio * getCraft().getType().getDynamicFlyBlockSpeedFactor();
            data.dynamicFlyBlockSpeedMultiplier = ratio;
        }
        if (isWithinLimit(blockList.size(), minSize, maxSize)) {
            data.setBlockList(blockList);
            if (confirmStructureRequirements(flyBlocks, blockTypeCount)) {
                data.setHitBox(blockList);

            }
        }
    }

    private void detectBlock(int x, int y, int z) {

        MovecraftLocation workingLocation = new MovecraftLocation(x, y, z);

        if (notVisited(workingLocation, visited)) {

            Material testType = null;
            int testID = 0;
            int testData = 0;
            try {
                testData = data.getWorld().getBlockAt(x, y, z).getData();
                testType = data.getWorld().getBlockAt(x, y, z).getType();

            } catch (Exception e) {
                fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), maxSize));
            }

            if ((testType == Material.WATER) || (testType == LegacyUtils.STATIONARY_WATER)) {
                data.setWaterContact(true);
            }
            if (testType == (Settings.IsLegacy ? LegacyUtils.SIGN_POST : Material.SIGN) || testType == Material.WALL_SIGN) {
                BlockState state = data.getWorld().getBlockAt(x, y, z).getState();
                if (state instanceof Sign) {
                    Sign s = (Sign) state;
                    if (s.getLine(0).equalsIgnoreCase("Pilot:") && data.getPlayer() != null) {
                        String playerName = data.getPlayer().getName();
                        boolean foundPilot = false;
                        if (s.getLine(1).equalsIgnoreCase(playerName) || s.getLine(2).equalsIgnoreCase(playerName)
                                || s.getLine(3).equalsIgnoreCase(playerName)) {
                            foundPilot = true;
                        }
                        if (!foundPilot && (!data.getPlayer().hasPermission("movecraft.bypasslock"))) {
                            fail(I18nSupport.getInternationalisedString(
                                    "Not one of the registered pilots on this craft"));
                        }
                    }
                    for (int i = 0; i < 4; i++) {
                        if (isForbiddenSignString(s.getLine(i))) {
                            fail(I18nSupport.getInternationalisedString(
                                    "Detection - Forbidden sign string found"));
                        }
                    }
                    if (s.getLine(0).equalsIgnoreCase("Name:") && !craft.getType().getCanBeNamed()){
                        fail("This craft type cannot be named");
                    }
                }
            }
            if (isForbiddenBlock(testType, testData)) {
                fail(I18nSupport.getInternationalisedString("Detection - Forbidden block found"));
            } else if (isAllowedBlock(testType, testData)) {
                // check for double chests
                if (testType == Material.CHEST) {
                    boolean foundDoubleChest = false;
                    if (data.getWorld().getBlockAt(x - 1, y, z).getType() == Material.CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x + 1, y, z).getType() == Material.CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x, y, z - 1).getType() == Material.CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x, y, z + 1).getType() == Material.CHEST) {
                        foundDoubleChest = true;
                    }
                    if (foundDoubleChest) {
                        fail(I18nSupport.getInternationalisedString(
                                "Detection - ERROR: Double chest found"));
                    }
                }
                // check for double trapped chests
                if (testType == Material.TRAPPED_CHEST) {
                    boolean foundDoubleChest = false;
                    if (data.getWorld().getBlockAt(x - 1, y, z).getType() == Material.TRAPPED_CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x + 1, y, z).getType() == Material.TRAPPED_CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x, y, z - 1).getType() == Material.TRAPPED_CHEST) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockAt(x, y, z + 1).getType() == Material.TRAPPED_CHEST) {
                        foundDoubleChest = true;
                    }
                    if (foundDoubleChest) {
                        fail(I18nSupport.getInternationalisedString(
                                "Detection - ERROR: Double chest found"));
                    }
                }

                Location loc = new Location(data.getWorld(), x, y, z);
                Player p;
                if (data.getPlayer() == null) {
                    p = data.getNotificationPlayer();
                } else {
                    p = data.getPlayer();
                }
                if (p != null) {

                    addToBlockList(workingLocation);
                    Material blockType = testType;
                    int blockID = testID;
                    int dataID = testData;
                    Integer shiftedID = (blockID << 4) + dataID + 10000;
                    for (Map<Material, List<Integer>> flyBlockDef : dFlyBlocks.keySet()) {
                        if ((flyBlockDef.containsKey(blockType) && flyBlockDef.get(blockType).isEmpty()) || (flyBlockDef.containsKey(blockType) && flyBlockDef.get(blockType).contains(dataID))) {
                            addToBlockCount(flyBlockDef);
                        } else {
                            addToBlockCount(null);
                        }
                    }
                    if (getCraft().getType().getDynamicFlyBlockSpeedFactor() != 0.0) {
                        if (blockType == getCraft().getType().getDynamicFlyBlock()) {
                            foundDynamicFlyBlock++;
                        }
                    }

                    if (isWithinLimit(blockList.size(), 0, maxSize)) {

                        addToDetectionStack(workingLocation);

                        calculateBounds(workingLocation);

                    }
                }
            }
        }
    }

    private boolean isAllowedBlock(Material test, int testData) {

        if (data.getAllowedBlocks().containsKey(test)){
            if (!data.getAllowedBlocks().get(test).isEmpty()){
                if (data.getAllowedBlocks().get(test).contains(testData)){
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        /*
        for (Material type : data.getAllowedBlocks().keySet()) {
            if ((type == test) || (type == test && data.getAllowedBlocks().get(test).contains(testData))) {
                return true;
            }
        }*/

        return false;
    }

    private boolean isForbiddenBlock(Material test, int testData) {
        if (data.getForbiddenBlocks().containsKey(test)){
            if (!data.getForbiddenBlocks().get(test).isEmpty()){
                if (data.getForbiddenBlocks().get(test).contains(testData)){
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    private boolean isForbiddenSignString(String testString) {

        for (String s : data.getForbiddenSignStrings()) {
            if (testString.equals(s)) {
                return true;
            }
        }

        return false;
    }

    public DetectionTaskData getData() {
        return data;
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
        blockList.add(l);
    }

    private void addToDetectionStack(MovecraftLocation l) {
        blockStack.push(l);
    }

    private void addToBlockCount(Map<Material, List<Integer>> id) {
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
        }
        if (data.getMinX() == null || l.getX() < data.getMinX()) {
            data.setMinX(l.getX());
        }
        if (l.getY() < minY) {
            minY = l.getY();
        }
        if (data.getMinZ() == null || l.getZ() < data.getMinZ()) {
            data.setMinZ(l.getZ());
        }
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

    private MovecraftLocation[] finaliseBlockList(HashSet<MovecraftLocation> blockSet) {
        // MovecraftLocation[] finalList=blockSet.toArray( new
        // MovecraftLocation[1] );
        ArrayList<MovecraftLocation> finalList = new ArrayList<>();

        // Sort the blocks from the bottom up to minimize lower altitude block
        // updates
        for (int posx = data.getMinX(); posx <= this.maxX; posx++) {
            for (int posz = data.getMinZ(); posz <= this.maxZ; posz++) {
                for (int posy = this.minY; posy <= this.maxY; posy++) {
                    MovecraftLocation test = new MovecraftLocation(posx, posy, posz);
                    if (blockSet.contains(test))
                        finalList.add(test);
                }
            }
        }
        return finalList.toArray(new MovecraftLocation[1]);
    }

    private boolean confirmStructureRequirements(Map<Map<Material, List<Integer>>, List<Double>> flyBlocks,
                                                 HashMap<Map<Material, List<Integer>>, Integer> countData) {
        if (getCraft().getType().getRequireWaterContact()) {
            if (!data.getWaterContact()) {
                fail(I18nSupport
                        .getInternationalisedString("Detection - Failed - Water contact required but not found"));
                return false;
            }
        }
        for (Map<Material, List<Integer>> i : flyBlocks.keySet()) {
            Integer numberOfBlocks = countData.get(i);
            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }

            float blockPercentage = (((float) numberOfBlocks / data.getBlockList().size()) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    if (i != null) {
                        if (i.get(0) != null) {
                            if (i.get(0).isEmpty() || i.get(0) == null) {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "), blockPercentage,
                                        minPercentage));
                                return false;
                            } else {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "),
                                        blockPercentage, minPercentage));
                                return false;
                            }
                        } else {
                            fail(String.format(
                                    I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                    i.keySet().iterator().next().name().toLowerCase().replace("_", " "), blockPercentage,
                                    minPercentage));
                            return false;
                        }
                    }
                }
            } else {
                if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                    if (i != null) {
                        if (i.get(0) != null) {
                            if (i.get(0).isEmpty() || i.get(0) == null) {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %d < %d",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "), numberOfBlocks,
                                        flyBlocks.get(i).get(0).intValue() - 10000));
                                return false;
                            } else {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %d < %d",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "),
                                        numberOfBlocks, flyBlocks.get(i).get(0).intValue() - 10000));
                                return false;
                            }
                        } else {
                            fail(String.format(I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %d > %d",
                                    i.keySet().iterator().next().name().toLowerCase().replace("_", " "), numberOfBlocks,
                                    flyBlocks.get(i).get(1).intValue() - 10000));
                            return false;
                        }
                    }
                }
            }
            if (maxPercentage < 10000.0) {
                if (blockPercentage > maxPercentage) {
                    if (i != null) {
                        if (i.get(0) != null) {
                            if (i.get(0).isEmpty() || i.get(0) == null) {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "), blockPercentage,
                                        maxPercentage));
                                return false;
                            } else {
                                fail(String.format(
                                        I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "),
                                        blockPercentage, maxPercentage));
                                return false;
                            }
                        } else {
                            fail(String.format(
                                    I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                    i.keySet().iterator().next().name().toLowerCase().replace("_", " "),
                                    blockPercentage, maxPercentage));
                            return false;
                        }
                    }
                }
            } else {
                if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                    if (i != null) {
                        if (i.get(0) != null) {
                            if (i.get(0).isEmpty()) {
                                fail(String.format(I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %d > %d",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "), numberOfBlocks,
                                        flyBlocks.get(i).get(1).intValue() - 10000));
                                return false;
                            } else {
                                fail(String.format(I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %d > %d",
                                        i.keySet().iterator().next().name().toLowerCase().replace("_", " "),
                                        numberOfBlocks, flyBlocks.get(i).get(1).intValue() - 10000));
                                return false;
                            }
                        } else {
                            fail(String.format(I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %d > %d",
                                    i.keySet().iterator().next().name().toLowerCase().replace("_", " "), numberOfBlocks,
                                    flyBlocks.get(i).get(1).intValue() - 10000));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void fail(String message) {
        data.setFailed(true);
        data.setFailMessage(message);
    }
}
