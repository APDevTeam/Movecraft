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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

final public class CraftType {
    private final boolean blockedByWater;
    private final boolean requireWaterContact;
    private final boolean tryNudge;
    private final boolean canBeNamed;
    private final boolean canCruise;
    private final boolean canTeleport;
    private final boolean canSwitchWorld;
    private final boolean canStaticMove;
    private final boolean canHover;
    private final boolean canDirectControl;
    private final boolean useGravity;
    private final boolean canHoverOverWater;
    private final boolean moveEntities;
    private final boolean onlyMovePlayers;
    private final boolean allowHorizontalMovement;
    private final boolean allowVerticalMovement;
    private final boolean allowRemoteSign;
    private final boolean allowCannonDirectorSign;
    private final boolean allowAADirectorSign;
    private final boolean cruiseOnPilot;
    private final boolean allowVerticalTakeoffAndLanding;
    private final boolean rotateAtMidpoint;
    private final boolean halfSpeedUnderwater;
    private final boolean focusedExplosion;
    private final boolean mustBeSubcraft;
    private final boolean keepMovingOnSink;
    private final int maxSize;
    private final int minSize;
    private final int minHeightLimit;
    private final int maxHeightLimit;
    private final int maxHeightAboveGround;
    private final int cruiseOnPilotVertMove;
    private final int maxStaticMove;
    private final int cruiseSkipBlocks;
    private final int vertCruiseSkipBlocks;
    private final int cruiseTickCooldown;
    private final int staticWaterLevel;
    private final int sinkRateTicks;
    private final int smokeOnSink;
    private final int tickCooldown;
    private final int hoverLimit;
    private final int dynamicFlyBlock;
    private final double fuelBurnRate;
    private final double sinkPercent;
    private final double overallSinkPercent;
    private final double detectionMultiplier;
    private final double underwaterDetectionMultiplier;
    private final double dynamicLagSpeedFactor;
    private final double dynamicLagPowerFactor;
    private final double dynamicLagMinSpeed;
    private final double dynamicFlyBlockSpeedFactor;
    private final double chestPenalty;
    private final float explodeOnCrash;
    private final float collisionExplosion;
    @NotNull private final String craftName;
    @NotNull private final int[] allowedBlocks;
    @NotNull private final int[] forbiddenBlocks;
    @NotNull private final String[] forbiddenSignStrings;
    @NotNull private final Map<List<Integer>, List<Double>> flyBlocks;
    @NotNull private final Map<List<Integer>, List<Double>> moveBlocks;
    @NotNull private final List<Material> harvestBlocks;
    @NotNull private final List<Material> harvesterBladeBlocks;
    @NotNull private final Set<Material> passthroughBlocks;
    @NotNull private final Set<Material> forbiddenHoverOverBlocks;
    private final int gravityDropDistance;
    private final int gravityInclineDistance;
    private final Sound collisionSound;

    @SuppressWarnings("unchecked")
    public CraftType(File f) {
        final Map data;
        try {
            InputStream input = new FileInputStream(f);
            Yaml yaml = new Yaml();
            data = (Map) yaml.load(input);
            input.close();
        } catch (IOException e) {
            throw new TypeNotFoundException("No file found at path " + f.getAbsolutePath());
        }

        //Required craft flags
        craftName = (String) data.get("name");
        maxSize = integerFromObject(data.get("maxSize"));
        minSize = integerFromObject(data.get("minSize"));
        allowedBlocks = blockIDListFromObject(data.get("allowedBlocks"));
        Arrays.sort(allowedBlocks);

        forbiddenBlocks = blockIDListFromObject(data.get("forbiddenBlocks"));
        forbiddenSignStrings = stringListFromObject(data.get("forbiddenSignStrings"));
        tickCooldown = (int) Math.ceil(20 / (doubleFromObject(data.get("speed"))));
        flyBlocks = blockIDMapListFromObject(data.get("flyblocks"));

        //Optional craft flags
        blockedByWater = (boolean) (data.containsKey("canFly") ? data.get("canFly") : data.getOrDefault("blockedByWater", true));
        requireWaterContact = (boolean) data.getOrDefault("requireWaterContact", false);
        tryNudge = (boolean) data.getOrDefault("tryNudge", false);
        moveBlocks = blockIDMapListFromObject(data.getOrDefault("moveblocks", new HashMap<>()));
        canCruise = (boolean) data.getOrDefault("canCruise", false);
        canTeleport = (boolean) data.getOrDefault("canTeleport", false);
        canSwitchWorld = (boolean) data.getOrDefault("canSwitchWorld", false);
        canBeNamed = (boolean) data.getOrDefault("canBeNamed", true);
        cruiseOnPilot = (boolean) data.getOrDefault("cruiseOnPilot", false);
        cruiseOnPilotVertMove = integerFromObject(data.getOrDefault("cruiseOnPilotVertMove", 0));
        allowVerticalMovement = (boolean) data.getOrDefault("allowVerticalMovement", true);
        rotateAtMidpoint = (boolean) data.getOrDefault("rotateAtMidpoint", false);
        allowHorizontalMovement = (boolean) data.getOrDefault("allowHorizontalMovement", true);
        allowRemoteSign = (boolean) data.getOrDefault("allowRemoteSign", true);
        allowCannonDirectorSign = (boolean) data.getOrDefault("allowCannonDirectorSign", true);
        allowAADirectorSign = (boolean) data.getOrDefault("allowAADirectorSign", true);
        canStaticMove = (boolean) data.getOrDefault("canStaticMove", false);
        maxStaticMove = integerFromObject(data.getOrDefault("maxStaticMove", 10000));
        cruiseSkipBlocks = integerFromObject(data.getOrDefault("cruiseSkipBlocks", 0));
        vertCruiseSkipBlocks = integerFromObject(data.getOrDefault("vertCruiseSkipBlocks", cruiseSkipBlocks));
        halfSpeedUnderwater = (boolean) data.getOrDefault("halfSpeedUnderwater", false);
        focusedExplosion = (boolean) data.getOrDefault("focusedExplosion", false);
        mustBeSubcraft = (boolean) data.getOrDefault("mustBeSubcraft", false);
        staticWaterLevel = integerFromObject(data.getOrDefault("staticWaterLevel", 0));
        fuelBurnRate = doubleFromObject(data.getOrDefault("fuelBurnRate", 0d));
        sinkPercent = doubleFromObject(data.getOrDefault("sinkPercent", 0d));
        overallSinkPercent = doubleFromObject(data.getOrDefault("overallSinkPercent", 0d));
        detectionMultiplier = doubleFromObject(data.getOrDefault("detectionMultiplier", 0d));
        underwaterDetectionMultiplier = doubleFromObject(data.getOrDefault("underwaterDetectionMultiplier", detectionMultiplier));
        sinkRateTicks = data.containsKey("sinkSpeed") ? (int) Math.ceil(20 / (doubleFromObject(data.get("sinkSpeed")))) : integerFromObject(data.getOrDefault("sinkTickRate", 0));
        keepMovingOnSink = (Boolean) data.getOrDefault("keepMovingOnSink", false);
        smokeOnSink = integerFromObject(data.getOrDefault("smokeOnSink", 0));
        explodeOnCrash = floatFromObject(data.getOrDefault("explodeOnCrash", 0F));
        collisionExplosion = floatFromObject(data.getOrDefault("collisionExplosion", 0F));
        minHeightLimit = Math.max(0, integerFromObject(data.getOrDefault("minHeightLimit", 0)));
        
        double cruiseSpeed = doubleFromObject(data.getOrDefault("cruiseSpeed", 20.0 / tickCooldown));
        cruiseTickCooldown = (int) Math.round((1.0 + cruiseSkipBlocks) * 20.0 / cruiseSpeed);
        if(Settings.Debug) {
            Bukkit.getLogger().info("Craft: " + craftName);
            Bukkit.getLogger().info("CruiseSpeed: " + cruiseSpeed);
            Bukkit.getLogger().info("Cooldown: " + cruiseTickCooldown);
        }

        int value = Math.min(integerFromObject(data.getOrDefault("maxHeightLimit", 254)), 255);
        if (value <= minHeightLimit) {
            value = 255;
        }
        
        maxHeightLimit = value;
        maxHeightAboveGround = integerFromObject(data.getOrDefault("maxHeightAboveGround", -1));
        canDirectControl = (boolean) data.getOrDefault("canDirectControl", true);
        canHover = (boolean) data.getOrDefault("canHover", false);
        canHoverOverWater = (boolean) data.getOrDefault("canHoverOverWater", true);
        moveEntities = (boolean) data.getOrDefault("moveEntities", true);
        onlyMovePlayers = (boolean) data.getOrDefault("onlyMovePlayers", true);
        useGravity = (boolean) data.getOrDefault("useGravity", false);
        hoverLimit = Math.max(0, integerFromObject(data.getOrDefault("hoverLimit", 0)));
        harvestBlocks = new ArrayList<>();
        harvesterBladeBlocks = new ArrayList<>();
        if (data.containsKey("harvestBlocks")) {
            ArrayList objList = (ArrayList) data.get("harvestBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial((String) i);
                    harvestBlocks.add(mat);
                } else {
                    Material mat = Material.getMaterial((Integer) i);
                    harvestBlocks.add(mat);
                }
            }

        }
        if (data.containsKey("harvesterBladeBlocks")) {
            ArrayList objList = (ArrayList) data.get("harvesterBladeBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial((String) i);
                    harvesterBladeBlocks.add(mat);
                } else {
                    Integer typeID = (Integer) i;
                    Material mat = Material.getMaterial((Integer) i);
                    harvesterBladeBlocks.add(mat);
                }
            }
        }
        passthroughBlocks = new HashSet<>();
        if (data.containsKey("passthroughBlocks")) {
            ArrayList objList = (ArrayList) data.get("passthroughBlocks");
            for (Object i : objList) {
                if (i instanceof String) {
                    Material mat = Material.getMaterial((String) i);
                    passthroughBlocks.add(mat);
                } else {
                    Material mat = Material.getMaterial((Integer) i);
                    passthroughBlocks.add(mat);
                }
            }
        }
        if(!blockedByWater){
            passthroughBlocks.add(Material.WATER);
            passthroughBlocks.add(Material.STATIONARY_WATER);
        }
        forbiddenHoverOverBlocks = new HashSet<>();
        if (data.containsKey("forbiddenHoverOverBlocks")){
            final ArrayList objList = (ArrayList) data.get("forbiddenHoverOverBlocks");
            for (Object i : objList){
                if (i instanceof Integer){
                    forbiddenHoverOverBlocks.add(Material.getMaterial((int) i));
                } else if (i instanceof String){
                    forbiddenHoverOverBlocks.add(Material.getMaterial(((String) i).toUpperCase()));
                }
            }
        }
        if (!canHoverOverWater){
            forbiddenHoverOverBlocks.add(Material.WATER);
            forbiddenHoverOverBlocks.add(Material.STATIONARY_WATER);
        }
        allowVerticalTakeoffAndLanding = (boolean) data.getOrDefault("allowVerticalTakeoffAndLanding", true);
        dynamicLagSpeedFactor = doubleFromObject(data.getOrDefault("dynamicLagSpeedFactor", 0d));
        dynamicLagPowerFactor = doubleFromObject(data.getOrDefault("dynamicLagPowerFactor", 0d));
        dynamicLagMinSpeed = doubleFromObject((data.getOrDefault("dynamicLagMinSpeed", 0d)));
        dynamicFlyBlockSpeedFactor = doubleFromObject(data.getOrDefault("dynamicFlyBlockSpeedFactor", 0d));
        dynamicFlyBlock = integerFromObject(data.getOrDefault("dynamicFlyBlock", 0));
        chestPenalty = doubleFromObject(data.getOrDefault("chestPenalty", 0));
        gravityInclineDistance = integerFromObject(data.getOrDefault("gravityInclineDistance", -1));
        int dropdist = integerFromObject(data.getOrDefault("gravityDropDistance", -8));
        gravityDropDistance = dropdist > 0 ? -dropdist : dropdist;
        collisionSound = Sound.valueOf((String) data.getOrDefault("collisionSound", "BLOCK_ANVIL_LAND"));
    }

    private int integerFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }
        return (Integer) obj;
    }


    private double doubleFromObject(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        return (Double) obj;
    }

    private float floatFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        }
        return (float) obj;
    }

    private int[] blockIDListFromObject(Object obj) {
        ArrayList<Integer> returnList = new ArrayList<>();
        ArrayList objList = (ArrayList) obj;
        for (Object i : objList) {
            if (i instanceof String) {
                String str = (String) i;
                if (str.contains(":")) {
                    String[] parts = str.split(":");
                    Integer typeID = Integer.valueOf(parts[0]);
                    Integer metaData = Integer.valueOf(parts[1]);
                    returnList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                } else {
                    Integer typeID = Integer.valueOf(str);
                    returnList.add(typeID);
                }
            } else {
                Integer typeID = (Integer) i;
                returnList.add(typeID);
            }
        }
        int[] output = new int[returnList.size()];
        for (int i = 0; i < output.length; i++)
            output[i] = returnList.get(i);
        return output;
    }

    private String[] stringListFromObject(Object obj) {
        ArrayList<String> returnList = new ArrayList<>();
        if (obj == null) {
            return returnList.toArray(new String[1]);
        }
        ArrayList objList = (ArrayList) obj;
        for (Object i : objList) {
            if (i instanceof String) {
                String str = (String) i;
                returnList.add(str);
            }
        }
        return returnList.toArray(new String[1]);
    }

    private Map<List<Integer>, List<Double>> blockIDMapListFromObject(Object obj) {
        HashMap<List<Integer>, List<Double>> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object i : objMap.keySet()) {
            ArrayList<Integer> rowList = new ArrayList<>();

            // first read in the list of the blocks that type of flyblock. It could be a single string (with or without a ":") or integer, or it could be multiple of them
            if (i instanceof ArrayList<?>) {
                for (Object o : (ArrayList<Object>) i) {
                    if (o instanceof String) {
                        String str = (String) o;
                        if (str.contains(":")) {
                            String[] parts = str.split(":");
                            Integer typeID = Integer.valueOf(parts[0]);
                            Integer metaData = Integer.valueOf(parts[1]);
                            rowList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                        } else {
                            Integer typeID = Integer.valueOf(str);
                            rowList.add(typeID);
                        }
                    } else {
                        Integer typeID = (Integer) o;
                        rowList.add(typeID);
                    }
                }
            } else if (i instanceof String) {
                String str = (String) i;
                if (str.contains(":")) {
                    String[] parts = str.split(":");
                    Integer typeID = Integer.valueOf(parts[0]);
                    Integer metaData = Integer.valueOf(parts[1]);
                    rowList.add(10000 + (typeID << 4) + metaData);  // id greater than 10000 indicates it has a meta data / damage value
                } else {
                    Integer typeID = Integer.valueOf(str);
                    rowList.add(typeID);
                }
            } else {
                Integer typeID = (Integer) i;
                rowList.add(typeID);
            }

            // then read in the limitation values, low and high
            ArrayList<Object> objList = (ArrayList<Object>) objMap.get(i);
            ArrayList<Double> limitList = new ArrayList<>();
            for (Object limitObj : objList) {
                if (limitObj instanceof String) {
                    String str = (String) limitObj;
                    if (str.contains("N")) { // a # indicates a specific quantity, IE: #2 for exactly 2 of the block
                        String[] parts = str.split("N");
                        Double val = Double.valueOf(parts[1]);
                        limitList.add(10000d + val);  // limit greater than 10000 indicates an specific quantity (not a ratio)
                    } else {
                        Double val = Double.valueOf(str);
                        limitList.add(val);
                    }
                } else if (limitObj instanceof Integer) {
                    Double ret = ((Integer) limitObj).doubleValue();
                    limitList.add(ret);
                } else
                    limitList.add((Double) limitObj);
            }
            returnMap.put(rowList, limitList);
        }
        return returnMap;
    }

    @NotNull
    public String getCraftName() {
        return craftName;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinSize() {
        return minSize;
    }

    public int[] getAllowedBlocks() {
        return allowedBlocks;
    }

    public int[] getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    public String[] getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public boolean blockedByWater() {
        return blockedByWater;
    }

    public boolean getRequireWaterContact() {
        return requireWaterContact;
    }

    public boolean getCanCruise() {
        return canCruise;
    }

    public int getCruiseSkipBlocks() {
        return cruiseSkipBlocks;
    }

    public int getVertCruiseSkipBlocks() {
        return vertCruiseSkipBlocks;
    }

    public boolean getCanBeNamed(){
        return canBeNamed;
    }

    public int maxStaticMove() {
        return maxStaticMove;
    }

    public int getStaticWaterLevel() {
        return staticWaterLevel;
    }

    public boolean getCanTeleport() {
        return canTeleport;
    }
    
    public boolean getCanSwitchWorld() {
        return canSwitchWorld;
    }

    public boolean getCanStaticMove() {
        return canStaticMove;
    }

    public boolean getCruiseOnPilot() {
        return cruiseOnPilot;
    }

    public int getCruiseOnPilotVertMove() {
        return cruiseOnPilotVertMove;
    }

    public boolean allowVerticalMovement() {
        return allowVerticalMovement;
    }

    public boolean rotateAtMidpoint() {
        return rotateAtMidpoint;
    }

    public boolean allowHorizontalMovement() {
        return allowHorizontalMovement;
    }

    public boolean allowRemoteSign() {
        return allowRemoteSign;
    }

    public boolean allowCannonDirectorSign() {
        return allowCannonDirectorSign;
    }

    public boolean allowAADirectorSign() {
        return allowAADirectorSign;
    }

    public double getFuelBurnRate() {
        return fuelBurnRate;
    }

    public double getSinkPercent() {
        return sinkPercent;
    }

    public double getOverallSinkPercent() {
        return overallSinkPercent;
    }

    public double getDetectionMultiplier() {
        return detectionMultiplier;
    }

    public double getUnderwaterDetectionMultiplier() {
        return underwaterDetectionMultiplier;
    }

    public int getSinkRateTicks() {
        return sinkRateTicks;
    }

    public boolean getKeepMovingOnSink() {
        return keepMovingOnSink;
    }

    public float getExplodeOnCrash() {
        return explodeOnCrash;
    }

    public int getSmokeOnSink() {
        return smokeOnSink;
    }

    public float getCollisionExplosion() {
        return collisionExplosion;
    }

    public int getTickCooldown() {
        return tickCooldown;
    }

    public int getCruiseTickCooldown() {
        return cruiseTickCooldown;
    }

    public boolean getHalfSpeedUnderwater() {
        return halfSpeedUnderwater;
    }

    public boolean getFocusedExplosion() {
        return focusedExplosion;
    }

    public boolean getMustBeSubcraft() {
        return mustBeSubcraft;
    }

    public boolean isTryNudge() {
        return tryNudge;
    }

    @NotNull
    public Map<List<Integer>, List<Double>> getFlyBlocks() {
        return flyBlocks;
    }

    @NotNull
    public Map<List<Integer>, List<Double>> getMoveBlocks() {
        return moveBlocks;
    }

    public int getMaxHeightLimit() {
        return maxHeightLimit;
    }

    public int getMinHeightLimit() {
        return minHeightLimit;
    }

    public int getMaxHeightAboveGround() {
        return maxHeightAboveGround;
    }

    public boolean getCanHover() {
        return canHover;
    }

    public boolean getCanDirectControl() {
        return canDirectControl;
    }

    public int getHoverLimit() {
        return hoverLimit;
    }

    @NotNull
    public List<Material> getHarvestBlocks() {
        return harvestBlocks;
    }

    @NotNull
    public List<Material> getHarvesterBladeBlocks() {
        return harvesterBladeBlocks;
    }

    public boolean getCanHoverOverWater() {
        return canHoverOverWater;
    }

    public boolean getMoveEntities() {
        return moveEntities;
    }

    public boolean getUseGravity() {
        return useGravity;
    }

    public boolean allowVerticalTakeoffAndLanding() {
        return allowVerticalTakeoffAndLanding;
    }

    public double getDynamicLagSpeedFactor() {
        return dynamicLagSpeedFactor;
    }
    
    public double getDynamicLagPowerFactor() {
        return dynamicLagPowerFactor;
    }

    public double getDynamicLagMinSpeed() {
        return dynamicLagMinSpeed;
    }

    public double getDynamicFlyBlockSpeedFactor() {
        return dynamicFlyBlockSpeedFactor;
    }

    public int getDynamicFlyBlock() {
        return dynamicFlyBlock;
    }

    public double getChestPenalty() {
        return chestPenalty;
    }

    @NotNull
    public Set<Material> getPassthroughBlocks() {
        return passthroughBlocks;
    }

    public boolean getOnlyMovePlayers() {
        return onlyMovePlayers;
    }

    @NotNull
    public Set<Material> getForbiddenHoverOverBlocks() {
        return forbiddenHoverOverBlocks;
    }

    public int getGravityDropDistance() {
        return gravityDropDistance;
    }

    public int getGravityInclineDistance() {
        return gravityInclineDistance;
    }

    @NotNull
    public Sound getCollisionSound() {
        return collisionSound;
    }

    private class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}
