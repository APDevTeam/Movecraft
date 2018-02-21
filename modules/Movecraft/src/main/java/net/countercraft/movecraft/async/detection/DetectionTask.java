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

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.AsyncTask;
<<<<<<< HEAD
import net.countercraft.movecraft.api.config.Settings;
import net.countercraft.movecraft.api.craft.Craft;
=======
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
>>>>>>> upstream/master
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BoundingBoxUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.api.utils.TownyWorldHeightLimits;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DetectionTask extends AsyncTask {
    private final MovecraftLocation startLocation;
    private final Integer minSize;
    private final Integer maxSize;
    private final Stack<MovecraftLocation> blockStack = new Stack<>();
    private final HashSet<MovecraftLocation> blockList = new HashSet<>();
    private final HashSet<MovecraftLocation> visited = new HashSet<>();
    private final HashMap<ArrayList<Integer>, Integer> blockTypeCount = new HashMap<>();
    private final DetectionTaskData data;
    Set<TownBlock> townBlockSet = new HashSet<>();
    TownyWorld townyWorld = null;
    TownyWorldHeightLimits townyWorldHeightLimits = null;
    private Integer maxX;
    private Integer maxY;
    private Integer maxZ;
    private Integer minY;
    private HashMap<ArrayList<Integer>, ArrayList<Double>> dFlyBlocks;
    private int craftMinY = 0;
    private int craftMaxY = 0;
    private boolean townyEnabled = false;
    private int foundDynamicFlyBlock = 0;

    public DetectionTask(Craft c, MovecraftLocation startLocation, int minSize, int maxSize, Integer[] allowedBlocks,
                         Integer[] forbiddenBlocks, Player player, Player notificationPlayer, World w,
                         String[] forbiddenSignStrings) {
        super(c);
        this.startLocation = startLocation;
        this.minSize = minSize;
        this.maxSize = maxSize;
        data = new DetectionTaskData(w, player, notificationPlayer, allowedBlocks, forbiddenBlocks,
                forbiddenSignStrings);

        this.townyEnabled = Movecraft.getInstance().getTownyPlugin() != null;
        if (townyEnabled && Settings.TownyBlockMoveOnSwitchPerm) {
            this.townyWorld = TownyUtils.getTownyWorld(getCraft().getW());
            if (townyWorld != null) {
                this.townyEnabled = townyWorld.isUsingTowny();
                if (townyEnabled)
                    townyWorldHeightLimits = TownyUtils.getWorldLimits(getCraft().getW());
            }
        } else {
            this.townyEnabled = false;
        }
    }

    public DetectionTask(Craft c, MovecraftLocation startLocation, int minSize, int maxSize, Integer[] allowedBlocks,
                         Integer[] forbiddenBlocks, String[] forbiddenSignStrings, Player player, Player notificationPlayer,
                         World w) {
        super(c);
        this.startLocation = startLocation;
        this.minSize = minSize;
        this.maxSize = maxSize;
        data = new DetectionTaskData(w, player, notificationPlayer, allowedBlocks, forbiddenBlocks,
                forbiddenSignStrings);

        this.townyEnabled = Movecraft.getInstance().getTownyPlugin() != null;
        if (townyEnabled && Settings.TownyBlockMoveOnSwitchPerm) {
            this.townyWorld = TownyUtils.getTownyWorld(getCraft().getW());
            if (townyWorld != null) {
                this.townyEnabled = townyWorld.isUsingTowny();
                if (townyEnabled)
                    townyWorldHeightLimits = TownyUtils.getWorldLimits(getCraft().getW());
            }
        } else {
            this.townyEnabled = false;
        }
    }

    @Override
    public void excecute() {

        HashMap<ArrayList<Integer>, ArrayList<Double>> flyBlocks = (HashMap<ArrayList<Integer>, ArrayList<Double>>) getCraft()
                .getType().getFlyBlocks().clone();
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
            for (ArrayList<Integer> i : flyBlocks.keySet()) {
                if (i.contains(getCraft().getType().getDynamicFlyBlock()))
                    foundMinimum = flyBlocks.get(i).get(0);
            }
            ratio = ratio - (foundMinimum / 100.0);
            ratio = ratio * getCraft().getType().getDynamicFlyBlockSpeedFactor();
            data.dynamicFlyBlockSpeedMultiplier = ratio;
        }
        if (isWithinLimit(blockList.size(), minSize, maxSize)) {

            data.setBlockList(finaliseBlockList(blockList));

            if (confirmStructureRequirements(flyBlocks, blockTypeCount)) {
                data.setHitBox(BoundingBoxUtils.formBoundingBox(data.getBlockList(), data.getMinX(), maxX,
                        data.getMinZ(), maxZ));

            }

        }

    }

    private void detectBlock(int x, int y, int z) {

        MovecraftLocation workingLocation = new MovecraftLocation(x, y, z);

        if (notVisited(workingLocation, visited)) {

            int testID = 0;
            int testData = 0;
            try {
                testData = data.getWorld().getBlockAt(x, y, z).getData();
                testID = data.getWorld().getBlockTypeIdAt(x, y, z);
            } catch (Exception e) {
                fail(String.format(I18nSupport.getInternationalisedString("Detection - Craft too large"), maxSize));
            }

            if ((testID == 8) || (testID == 9)) {
                data.setWaterContact(true);
            }
            if (testID == 63 || testID == 68) {
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
                }
            }
            if (isForbiddenBlock(testID, testData)) {
                fail(I18nSupport.getInternationalisedString("Detection - Forbidden block found"));
            } else if (isAllowedBlock(testID, testData)) {
                // check for double chests
                if (testID == 54) {
                    boolean foundDoubleChest = false;
                    if (data.getWorld().getBlockTypeIdAt(x - 1, y, z) == 54) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x + 1, y, z) == 54) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x, y, z - 1) == 54) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x, y, z + 1) == 54) {
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
                    if (data.getWorld().getBlockTypeIdAt(x - 1, y, z) == 146) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x + 1, y, z) == 146) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x, y, z - 1) == 146) {
                        foundDoubleChest = true;
                    }
                    if (data.getWorld().getBlockTypeIdAt(x, y, z + 1) == 146) {
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
                    if (Movecraft.getInstance().getWorldGuardPlugin() != null
                            && Movecraft.getInstance().getWGCustomFlagsPlugin() != null
                            && Settings.WGCustomFlagsUsePilotFlag) {
                        LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin()
                                .wrapPlayer(p);
                        WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                        if (!WGCFU.validateFlag(loc, Movecraft.FLAG_PILOT, lp)) {
                            fail(String.format(
                                    I18nSupport.getInternationalisedString(
                                            "WGCustomFlags - Detection Failed") + " @ %d,%d,%d",
                                    x, y, z));
                        }
                    }

                    if (this.townyEnabled) {
                        TownBlock townBlock = TownyUtils.getTownBlock(loc);
                        if (townBlock != null && !this.townBlockSet.contains(townBlock)) {
                            if (TownyUtils.validateCraftMoveEvent(p, loc, this.townyWorld)) {
                                this.townBlockSet.add(townBlock);
                            } else {
                                int tY = loc.getBlockY();
                                boolean oChange = false;
                                if (this.craftMinY > tY) {
                                    this.craftMinY = tY;
                                    oChange = true;
                                }
                                if (this.craftMaxY < tY) {
                                    this.craftMaxY = tY;
                                    oChange = true;
                                }
                                if (oChange) {
                                    boolean failed = false;
                                    Town town = TownyUtils.getTown(townBlock);
                                    if (town != null) {
                                        Location locSpawn = TownyUtils.getTownSpawn(townBlock);
                                        if (locSpawn != null) {
                                            if (!this.townyWorldHeightLimits.validate(y,
                                                    locSpawn.getBlockY())) {
                                                failed = true;
                                            }
                                        } else {
                                            failed = true;
                                        }
                                        if (failed) {
                                            if (Movecraft.getInstance()
                                                    .getWorldGuardPlugin() != null
                                                    && Movecraft.getInstance()
                                                    .getWGCustomFlagsPlugin() != null
                                                    && Settings.WGCustomFlagsUsePilotFlag) {
                                                LocalPlayer lp = Movecraft
                                                        .getInstance().getWorldGuardPlugin()
                                                        .wrapPlayer(p);
                                                ApplicableRegionSet regions = Movecraft
                                                        .getInstance().getWorldGuardPlugin()
                                                        .getRegionManager(loc.getWorld())
                                                        .getApplicableRegions(loc);
                                                if (regions.size() != 0) {
                                                    WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                                                    if (WGCFU.validateFlag(loc,
                                                            Movecraft.FLAG_PILOT, lp)) {
                                                        failed = false;
                                                    }
                                                }
                                            }
                                        }
                                        if (failed) {
                                            fail(String
                                                    .format(I18nSupport.getInternationalisedString(
                                                            "Towny - Detection Failed")
                                                            + " %s @ %d,%d,%d", town.getName(), x, y, z));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    addToBlockList(workingLocation);
                    Integer blockID = testID;
                    Integer dataID = testData;
                    Integer shiftedID = (blockID << 4) + dataID + 10000;
                    for (ArrayList<Integer> flyBlockDef : dFlyBlocks.keySet()) {
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

                    if (isWithinLimit(blockList.size(), 0, maxSize)) {

                        addToDetectionStack(workingLocation);

                        calculateBounds(workingLocation);

                    }
                }
            }
        }
    }

    private boolean isAllowedBlock(int test, int testData) {

        for (int i : data.getAllowedBlocks()) {
            if ((i == test) || (i == (test << 4) + testData + 10000)) {
                return true;
            }
        }

        return false;
    }

    private boolean isForbiddenBlock(int test, int testData) {

        for (int i : data.getForbiddenBlocks()) {
            if ((i == test) || (i == (test << 4) + testData + 10000)) {
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

    private void addToBlockCount(ArrayList<Integer> id) {
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
        if (maxX == null || l.getX() > maxX) {
            maxX = l.getX();
        }
        if (maxY == null || l.getY() > maxY) {
            maxY = l.getY();
        }
        if (maxZ == null || l.getZ() > maxZ) {
            maxZ = l.getZ();
        }
        if (data.getMinX() == null || l.getX() < data.getMinX()) {
            data.setMinX(l.getX());
        }
        if (minY == null || l.getY() < minY) {
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

    private boolean confirmStructureRequirements(HashMap<ArrayList<Integer>, ArrayList<Double>> flyBlocks,
                                                 HashMap<ArrayList<Integer>, Integer> countData) {
        if (getCraft().getType().getRequireWaterContact()) {
            if (!data.getWaterContact()) {
                fail(I18nSupport
                        .getInternationalisedString("Detection - Failed - Water contact required but not found"));
                return false;
            }
        }
        for (ArrayList<Integer> i : flyBlocks.keySet()) {
            Integer numberOfBlocks = countData.get(i);

            if (numberOfBlocks == null) {
                numberOfBlocks = 0;
            }

            float blockPercentage = (((float) numberOfBlocks / data.getBlockList().length) * 100);
            Double minPercentage = flyBlocks.get(i).get(0);
            Double maxPercentage = flyBlocks.get(i).get(1);
            if (minPercentage < 10000.0) {
                if (blockPercentage < minPercentage) {
                    if (i.get(0) < 10000) {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), blockPercentage,
                                minPercentage));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %.2f%% < %.2f%%",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                blockPercentage, minPercentage));
                        return false;
                    }
                }
            } else {
                if (numberOfBlocks < flyBlocks.get(i).get(0) - 10000.0) {
                    if (i.get(0) < 10000) {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %d < %d",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), numberOfBlocks,
                                flyBlocks.get(i).get(0).intValue() - 10000));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Not enough flyblock") + ": %s %d < %d",
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
                                I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), blockPercentage,
                                maxPercentage));
                        return false;
                    } else {
                        fail(String.format(
                                I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %.2f%% > %.2f%%",
                                Material.getMaterial((i.get(0) - 10000) >> 4).name().toLowerCase().replace("_", " "),
                                blockPercentage, maxPercentage));
                        return false;
                    }
                }
            } else {
                if (numberOfBlocks > flyBlocks.get(i).get(1) - 10000.0) {
                    if (i.get(0) < 10000) {
                        fail(String.format(I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %d > %d",
                                Material.getMaterial(i.get(0)).name().toLowerCase().replace("_", " "), numberOfBlocks,
                                flyBlocks.get(i).get(1).intValue() - 10000));
                        return false;
                    } else {
                        fail(String.format(I18nSupport.getInternationalisedString("Too much flyblock") + ": %s %d > %d",
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
        data.setFailed(true);
        data.setFailMessage(message);
    }
}
