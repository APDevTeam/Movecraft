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

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

        craftName = (String) data.get("name");
        maxSize = integerFromObject(data.get("maxSize"));
        minSize = integerFromObject(data.get("minSize"));
//		allowedBlocks = ((ArrayList<String> ) data.get( "allowedBlocks" )).toArray( new Integer[1] );
        allowedBlocks = blockIDListFromObject(data.get("allowedBlocks"));
        Arrays.sort(allowedBlocks);

        forbiddenBlocks = blockIDListFromObject(data.get("forbiddenBlocks"));
        forbiddenSignStrings = stringListFromObject(data.get("forbiddenSignStrings"));
        if (data.containsKey("canFly")) {
            blockedByWater = (Boolean) data.get("canFly");
        } else if (data.containsKey("blockedByWater")) {
            blockedByWater = (Boolean) data.get("blockedByWater");
        } else {
            blockedByWater = true;
        }
        if (data.containsKey("requireWaterContact")) {
            requireWaterContact = (Boolean) data.get("requireWaterContact");
        } else {
            requireWaterContact = false;
        }
        if (data.containsKey("tryNudge")) {
            tryNudge = (Boolean) data.get("tryNudge");
        } else {
            tryNudge = false;
        }
        tickCooldown = (int) Math.ceil(20 / (doubleFromObject(data.get("speed"))));
        if (data.containsKey("cruiseSpeed")) {
            cruiseTickCooldown = (int) Math.ceil(20 / (doubleFromObject(data.get("cruiseSpeed"))));
        } else {
            cruiseTickCooldown = tickCooldown;
        }

        flyBlocks = blockIDMapListFromObject(data.get("flyblocks"));
        if (data.containsKey("moveblocks")) {
            moveBlocks = blockIDMapListFromObject(data.get("moveblocks"));
        } else {
            moveBlocks = new HashMap<>();
        }

        if (data.containsKey("canCruise")) {
            canCruise = (Boolean) data.get("canCruise");
        } else {
            canCruise = false;
        }
        if (data.containsKey("canTeleport")) {
            canTeleport = (Boolean) data.get("canTeleport");
        } else {
            canTeleport = false;
        }
        if (data.containsKey("canSwitchWorld")) {
            canSwitchWorld = (Boolean) data.get("canSwitchWorld");
        } else {
            canSwitchWorld = false;
        }
        if (data.containsKey("canBeNamed")){
            canBeNamed = (Boolean) data.get("canBeNamed");
        } else {
            canBeNamed = true;
        }
        if (data.containsKey("cruiseOnPilot")) {
            cruiseOnPilot = (Boolean) data.get("cruiseOnPilot");
        } else {
            cruiseOnPilot = false;
        }
        if (data.containsKey("cruiseOnPilotVertMove")) {
            cruiseOnPilotVertMove = integerFromObject(data.get("cruiseOnPilotVertMove"));
        } else {
            cruiseOnPilotVertMove = 0;
        }
        if (data.containsKey("allowVerticalMovement")) {
            allowVerticalMovement = (Boolean) data.get("allowVerticalMovement");
        } else {
            allowVerticalMovement = true;
        }
        if (data.containsKey("rotateAtMidpoint")) {
            rotateAtMidpoint = (Boolean) data.get("rotateAtMidpoint");
        } else {
            rotateAtMidpoint = false;
        }
        if (data.containsKey("allowHorizontalMovement")) {
            allowHorizontalMovement = (Boolean) data.get("allowHorizontalMovement");
        } else {
            allowHorizontalMovement = true;
        }
        if (data.containsKey("allowRemoteSign")) {
            allowRemoteSign = (Boolean) data.get("allowRemoteSign");
        } else {
            allowRemoteSign = true;
        }
        if (data.containsKey("allowCannonDirectorSign")) {
            allowCannonDirectorSign = (Boolean) data.get("allowCannonDirectorSign");
        } else {
            allowCannonDirectorSign = true;
        }
        if (data.containsKey("allowAADirectorSign")) {
            allowAADirectorSign = (Boolean) data.get("allowAADirectorSign");
        } else {
            allowAADirectorSign = true;
        }
        if (data.containsKey("canStaticMove")) {
            canStaticMove = (Boolean) data.get("canStaticMove");
        } else {
            canStaticMove = false;
        }
        if (data.containsKey("maxStaticMove")) {
            maxStaticMove = integerFromObject(data.get("maxStaticMove"));
        } else {
            maxStaticMove = 10000;
        }
        if (data.containsKey("cruiseSkipBlocks")) {
            cruiseSkipBlocks = integerFromObject(data.get("cruiseSkipBlocks"));
        } else {
            cruiseSkipBlocks = 0;
        }
        if (data.containsKey("vertCruiseSkipBlocks")) {
            vertCruiseSkipBlocks = integerFromObject(data.get("vertCruiseSkipBlocks"));
        } else {
            vertCruiseSkipBlocks = cruiseSkipBlocks;
        }
        if (data.containsKey("halfSpeedUnderwater")) {
            halfSpeedUnderwater = (Boolean) data.get("halfSpeedUnderwater");
        } else {
            halfSpeedUnderwater = false;
        }
        if (data.containsKey("focusedExplosion")) {
            focusedExplosion = (Boolean) data.get("focusedExplosion");
        } else {
            focusedExplosion = false;
        }
        if (data.containsKey("mustBeSubcraft")) {
            mustBeSubcraft = (Boolean) data.get("mustBeSubcraft");
        } else {
            mustBeSubcraft = false;
        }
        if (data.containsKey("staticWaterLevel")) {
            staticWaterLevel = integerFromObject(data.get("staticWaterLevel"));
        } else {
            staticWaterLevel = 0;
        }
        if (data.containsKey("fuelBurnRate")) {
            fuelBurnRate = doubleFromObject(data.get("fuelBurnRate"));
        } else {
            fuelBurnRate = 0d;
        }
        if (data.containsKey("sinkPercent")) {
            sinkPercent = doubleFromObject(data.get("sinkPercent"));
        } else {
            sinkPercent = 0d;
        }
        if (data.containsKey("overallSinkPercent")) {
            overallSinkPercent = doubleFromObject(data.get("overallSinkPercent"));
        } else {
            overallSinkPercent = 0d;
        }
        if (data.containsKey("detectionMultiplier")) {
            detectionMultiplier = doubleFromObject(data.get("detectionMultiplier"));
        } else {
            detectionMultiplier = 0d;
        }
        if (data.containsKey("underwaterDetectionMultiplier")) {
            underwaterDetectionMultiplier = doubleFromObject(data.get("underwaterDetectionMultiplier"));
        } else {
            underwaterDetectionMultiplier = detectionMultiplier;
        }
        if(data.containsKey("sinkTickRate")){
            sinkRateTicks = integerFromObject(data.get("sinkTickRate"));
        }else if (data.containsKey("sinkSpeed")) {
            sinkRateTicks = (int) Math.ceil(20 / (doubleFromObject(data.get("sinkSpeed"))));
        } else {
            //sinkRateTicks = (int) Settings.SinkRateTicks;
            sinkRateTicks = 0;
        }
        if (data.containsKey("keepMovingOnSink")) {
            keepMovingOnSink = (Boolean) data.get("keepMovingOnSink");
        } else {
            keepMovingOnSink = false;
        }
        if (data.containsKey("smokeOnSink")) {
            smokeOnSink = integerFromObject(data.get("smokeOnSink"));
        } else {
            smokeOnSink = 0;
        }
        if (data.containsKey("explodeOnCrash")) {
            double temp = doubleFromObject(data.get("explodeOnCrash"));
            explodeOnCrash = (float) temp;
        } else {
            explodeOnCrash = 0F;
        }
        if (data.containsKey("collisionExplosion")) {
            double temp = doubleFromObject(data.get("collisionExplosion"));
            collisionExplosion = (float) temp;
        } else {
            collisionExplosion = 0F;
        }
        if (data.containsKey("minHeightLimit")) {
            minHeightLimit = Math.max(0, integerFromObject(data.get("minHeightLimit")));
        } else {
            minHeightLimit = 0;
        }
        if (data.containsKey("maxHeightLimit")) {
            int value = integerFromObject(data.get("maxHeightLimit"));
            if (value <= minHeightLimit) {
                value = 255;
            }
            maxHeightLimit = value;
        } else {
            maxHeightLimit = 254;
        }
        if (data.containsKey("maxHeightAboveGround")) {
            maxHeightAboveGround = integerFromObject(data.get("maxHeightAboveGround"));
        } else {
            maxHeightAboveGround = -1;
        }
        if (data.containsKey("canDirectControl")) {
            canDirectControl = (Boolean) data.get("canDirectControl");
        } else {
            canDirectControl = true;
        }
        if (data.containsKey("canHover")) {
            canHover = (Boolean) data.get("canHover");
        } else {
            canHover = false;
        }
        if (data.containsKey("canHoverOverWater")) {
            canHoverOverWater = (Boolean) data.get("canHoverOverWater");
        } else {
            canHoverOverWater = true;
        }
        if (data.containsKey("moveEntities")) {
            moveEntities = (Boolean) data.get("moveEntities");
        } else {
            moveEntities = true;
        }

        if(data.containsKey("onlyMovePlayers")){
            onlyMovePlayers = (Boolean) data.get("onlyMovePlayers");
        } else {
            onlyMovePlayers = true;
        }

        if (data.containsKey("useGravity")) {
            useGravity = (Boolean) data.get("useGravity");
        } else {
            useGravity = false;
        }

        if (data.containsKey("hoverLimit")) {
            hoverLimit = Math.max(0, integerFromObject(data.get("hoverLimit")));
        } else {
            hoverLimit = 0;
        }
        harvestBlocks = new ArrayList<>();
        harvesterBladeBlocks = new ArrayList<>();
        if (data.containsKey("harvestBlocks")) {
    /*        	String[] temp = ((ArrayList<String> ) data.get( "harvestBlocks" )).toArray( new String[1] );
                    for (int i = 0; i < temp.length; i++){
                            Material mat = Material.getMaterial(temp[i]);
                            if (mat != null ){
                                    harvestBlocks.add(mat);
                            }*/
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
        if (data.containsKey("allowVerticalTakeoffAndLanding")) {
            allowVerticalTakeoffAndLanding = (Boolean) data.get("allowVerticalTakeoffAndLanding");
        } else {
            allowVerticalTakeoffAndLanding = true;
        }

        if (data.containsKey("dynamicLagSpeedFactor")) {
            dynamicLagSpeedFactor = doubleFromObject(data.get("dynamicLagSpeedFactor"));
        } else {
            dynamicLagSpeedFactor = 0d;
        }
        if (data.containsKey("dynamicFlyBlockSpeedFactor")) {
            dynamicFlyBlockSpeedFactor = doubleFromObject(data.get("dynamicFlyBlockSpeedFactor"));
        } else {
            dynamicFlyBlockSpeedFactor = 0d;
        }
        if (data.containsKey("dynamicFlyBlock")) {
            dynamicFlyBlock = integerFromObject(data.get("dynamicFlyBlock"));
        } else {
            dynamicFlyBlock = 0;
        }
        chestPenalty = data.containsKey("chestPenalty") ? doubleFromObject(data.get("chestPenalty")) : 0d;
    }

    private Integer integerFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }
        return (Integer) obj;
    }

    private Double doubleFromObject(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        return (Double) obj;
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

    private class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}
