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
import org.bukkit.Sound;
import org.bukkit.World;
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
    @NotNull private final Map<String, Integer> perWorldMinHeightLimit;
    private final int maxHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightLimit;
    private final int maxHeightAboveGround;
    @NotNull private final Map<String, Integer> perWorldMaxHeightAboveGround;
    private final int cruiseOnPilotVertMove;
    private final int maxStaticMove;
    private final int cruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldCruiseSkipBlocks;
    private final int vertCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldVertCruiseSkipBlocks;
    private final int cruiseTickCooldown;
    @NotNull private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    private final int vertCruiseTickCooldown;
    @NotNull private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
    private final int staticWaterLevel;
    private final int sinkRateTicks;
    private final int smokeOnSink;
    private final int tickCooldown;
    @NotNull private final Map<String, Integer> perWorldTickCooldown; // speed setting
    private final int hoverLimit;
    private final Material dynamicFlyBlock;
    private final double fuelBurnRate;
    @NotNull private final Map<String, Double> perWorldFuelBurnRate;
    private final double sinkPercent;
    private final double overallSinkPercent;
    private final double detectionMultiplier;
    @NotNull private final Map<String, Double> perWorldDetectionMultiplier;
    private final double underwaterDetectionMultiplier;
    @NotNull private final Map<String, Double> perWorldUnderwaterDetectionMultiplier;
    private final double dynamicLagSpeedFactor;
    private final double dynamicLagPowerFactor;
    private final double dynamicLagMinSpeed;
    private final double dynamicFlyBlockSpeedFactor;
    private final double chestPenalty;
    private final float explodeOnCrash;
    private final float collisionExplosion;
    @NotNull private final String craftName;
    @NotNull private final EnumSet<Material> allowedBlocks;
    @NotNull private final EnumSet<Material> forbiddenBlocks;
    @NotNull private final String[] forbiddenSignStrings;
    @NotNull private final Map<List<Material>, List<Double>> flyBlocks;
    @NotNull private final Map<List<Material>, List<Double>> moveBlocks;
    @NotNull private final EnumSet<Material> harvestBlocks;
    @NotNull private final EnumSet<Material> harvesterBladeBlocks;
    @NotNull private final EnumSet<Material> passthroughBlocks;
    @NotNull private final EnumSet<Material> forbiddenHoverOverBlocks;
    @NotNull private final Map<Material, Double> fuelTypes;
    @NotNull private final Set<String> disableTeleportToWorlds;
    private final int teleportationCooldown;
    private final int gravityDropDistance;
    private final int gravityInclineDistance;
    private final int gearShifts;
    private final boolean gearShiftsAffectTickCooldown;
    private final boolean gearShiftsAffectDirectMovement;
    private final Sound collisionSound;
    private final boolean gearShiftsAffectCruiseSkipBlocks;

    private final Map<String, Object> backingData;

    public CraftType(File f) {
        try {
            InputStream input = new FileInputStream(f);
            Yaml yaml = new Yaml();
            backingData = yaml.load(input);
            input.close();
        }
        catch (IOException e) {
            throw new TypeNotFoundException("No file found at path " + f.getAbsolutePath());
        }

        //Required craft flags
        craftName = (String) backingData.get("name");
        maxSize = this.getInt("maxSize");
        minSize = this.getInt("minSize");
        allowedBlocks = getBlockIDSet("allowedBlocks");

        forbiddenBlocks = getBlockIDSet("forbiddenBlocks");
        forbiddenSignStrings = stringListFromObject(backingData.get("forbiddenSignStrings"));
        tickCooldown = (int) Math.ceil(20 / (getDouble("speed")));
        perWorldTickCooldown = new HashMap<>();
        Map<String, Double> tickCooldownMap = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldSpeed", new HashMap<>()));
        tickCooldownMap.forEach((world, speed) -> perWorldTickCooldown.put(world, (int) Math.ceil(20 / speed)));
        flyBlocks = blockIDMapListFromObject(backingData.get("flyblocks"));

        //Optional craft flags
        blockedByWater = this.getBooleanOrDefault("canFly", this.getBooleanOrDefault("blockedByWater", true));
        requireWaterContact = this.getBooleanOrDefault("requireWaterContact", false);
        tryNudge = this.getBooleanOrDefault("tryNudge", false);
        moveBlocks = blockIDMapListFromObject(backingData.getOrDefault("moveblocks", new HashMap<>()));
        canCruise = this.getBooleanOrDefault("canCruise", false);
        canTeleport = this.getBooleanOrDefault("canTeleport", false);
        canSwitchWorld = this.getBooleanOrDefault("canSwitchWorld", false);
        canBeNamed = this.getBooleanOrDefault("canBeNamed", true);
        cruiseOnPilot = this.getBooleanOrDefault("cruiseOnPilot", false);
        cruiseOnPilotVertMove = getIntOrDefault("cruiseOnPilotVertMove", 0);
        allowVerticalMovement = this.getBooleanOrDefault("allowVerticalMovement", true);
        rotateAtMidpoint = this.getBooleanOrDefault("rotateAtMidpoint", false);
        allowHorizontalMovement = this.getBooleanOrDefault("allowHorizontalMovement", true);
        allowRemoteSign = this.getBooleanOrDefault("allowRemoteSign", true);
        canStaticMove = this.getBooleanOrDefault("canStaticMove", false);
        maxStaticMove = this.getIntOrDefault("maxStaticMove", 10000);
        cruiseSkipBlocks = this.getIntOrDefault("cruiseSkipBlocks", 0);
        perWorldCruiseSkipBlocks = stringToIntMapFromObject(backingData.getOrDefault("perWorldCruiseSkipBlocks", new HashMap<>()));
        vertCruiseSkipBlocks = this.getIntOrDefault("vertCruiseSkipBlocks", cruiseSkipBlocks);
        perWorldVertCruiseSkipBlocks = stringToIntMapFromObject(backingData.getOrDefault("perWorldVertCruiseSkipBlocks", new HashMap<>()));
        halfSpeedUnderwater = this.getBooleanOrDefault("halfSpeedUnderwater", false);
        focusedExplosion = this.getBooleanOrDefault("focusedExplosion", false);
        mustBeSubcraft = this.getBooleanOrDefault("mustBeSubcraft", false);
        staticWaterLevel = this.getIntOrDefault("staticWaterLevel", 0);
        fuelBurnRate = this.getDoubleOrDefault("fuelBurnRate", 0d);
        perWorldFuelBurnRate = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldFuelBurnRate", new HashMap<>()));
        sinkPercent = this.getDoubleOrDefault("sinkPercent", 0d);
        overallSinkPercent = this.getDoubleOrDefault("overallSinkPercent", 0d);
        detectionMultiplier = this.getDoubleOrDefault("detectionMultiplier", 0d);
        perWorldDetectionMultiplier = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldDetectionMultiplier", new HashMap<>()));
        underwaterDetectionMultiplier = this.getDoubleOrDefault("underwaterDetectionMultiplier", detectionMultiplier);
        perWorldUnderwaterDetectionMultiplier = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldUnderwaterDetectionMultiplier", new HashMap<>()));
        sinkRateTicks = this.getIntOrDefault("sinkRateTicks", (int) Math.ceil(20 / this.getDoubleOrDefault("sinkSpeed", -200))); // default becomes 0
        keepMovingOnSink = this.getBooleanOrDefault("keepMovingOnSink", false);
        smokeOnSink = this.getIntOrDefault("smokeOnSink", 0);
        explodeOnCrash = (float) getDoubleOrDefault("explodeOnCrash", 0D);
        collisionExplosion = (float) getDoubleOrDefault("collisionExplosion", 0D);
        minHeightLimit = Math.max(0, this.getIntOrDefault("minHeightLimit", 0));
        perWorldMinHeightLimit = new HashMap<>();
        Map<String, Integer> minHeightMap = stringToIntMapFromObject(backingData.getOrDefault("perWorldMinHeightLimit", new HashMap<>()));
        minHeightMap.forEach((world, height) -> perWorldMinHeightLimit.put(world, Math.max(0, height)));

        double cruiseSpeed = this.getDoubleOrDefault("cruiseSpeed", 20.0 / tickCooldown);
        cruiseTickCooldown = (int) Math.round((1.0 + cruiseSkipBlocks) * 20.0 / cruiseSpeed);
        perWorldCruiseTickCooldown = new HashMap<>();
        Map<String, Double> cruiseTickCooldownMap = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldCruiseSpeed", new HashMap<>()));
        cruiseTickCooldownMap.forEach((world, speed) -> {
            double worldCruiseSkipBlocks = perWorldCruiseSkipBlocks.getOrDefault(world, cruiseSkipBlocks);
            perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldCruiseSkipBlocks) * 20.0 / cruiseSpeed));
        });
        tickCooldownMap.forEach((world, speed) -> {
            if (!perWorldCruiseTickCooldown.containsKey(world)) {
                perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + cruiseSkipBlocks) * 20.0 / speed));
            }
        });

        double vertCruiseSpeed = this.getDoubleOrDefault("vertCruiseSpeed", cruiseSpeed);
        vertCruiseTickCooldown = (int) Math.round((1.0 + vertCruiseSkipBlocks) * 20.0 / vertCruiseSpeed);
        perWorldVertCruiseTickCooldown = new HashMap<>();
        Map<String, Double> vertCruiseTickCooldownMap = stringToDoubleMapFromObject(backingData.getOrDefault("perWorldVertCruiseSpeed", new HashMap<>()));
        vertCruiseTickCooldownMap.forEach((world, speed) -> {
            double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, vertCruiseSkipBlocks);
            perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / speed));
        });
        cruiseTickCooldownMap.forEach((world, speed) -> {
            if (!perWorldVertCruiseTickCooldown.containsKey(world)) {
                double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, vertCruiseSkipBlocks);
                perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / vertCruiseSpeed));
            }
        });

        int value = Math.min(this.getIntOrDefault("maxHeightLimit", 254), 255);
        if (value <= minHeightLimit) {
            value = 255;
        }

        maxHeightLimit = value;
        perWorldMaxHeightLimit = new HashMap<>();
        Map<String, Integer> maxHeightMap = stringToIntMapFromObject(backingData.getOrDefault("perWorldMaxHeightLimit", new HashMap<>()));
        maxHeightMap.forEach((world, height) -> {
            int worldValue = Math.min(height, 255);
            int worldMinHeight = perWorldMinHeightLimit.getOrDefault(world, minHeightLimit);
            if (worldValue <= worldMinHeight) worldValue = 255;
            perWorldMaxHeightLimit.put(world, worldValue);
        });
        
        maxHeightAboveGround = this.getIntOrDefault("maxHeightAboveGround", -1);
        perWorldMaxHeightAboveGround = stringToIntMapFromObject(backingData.getOrDefault("perWorldMaxHeightAboveGround", new HashMap<>()));
        canDirectControl = this.getBooleanOrDefault("canDirectControl", true);
        canHover = this.getBooleanOrDefault("canHover", false);
        canHoverOverWater = this.getBooleanOrDefault("canHoverOverWater", true);
        moveEntities = this.getBooleanOrDefault("moveEntities", true);
        onlyMovePlayers = this.getBooleanOrDefault("onlyMovePlayers", true);
        useGravity = this.getBooleanOrDefault("useGravity", false);
        hoverLimit = Math.max(0, this.getIntOrDefault("hoverLimit", 0));
        harvestBlocks = getBlockIDSetOrEmpty("harvestBlocks");
        harvesterBladeBlocks = this.getBlockIDSetOrEmpty("harvesterBladeBlocks");
        passthroughBlocks = this.getBlockIDSetOrEmpty("passthroughBlocks");
        if(!blockedByWater){
            passthroughBlocks.add(Material.WATER);
        }
        forbiddenHoverOverBlocks = this.getBlockIDSetOrEmpty("forbiddenHoverOverBlocks");
        if (!canHoverOverWater){
            forbiddenHoverOverBlocks.add(Material.WATER);
        }
        allowVerticalTakeoffAndLanding = this.getBooleanOrDefault("allowVerticalTakeoffAndLanding", true);
        dynamicLagSpeedFactor = this.getDoubleOrDefault("dynamicLagSpeedFactor", 0d);
        dynamicLagPowerFactor = this.getDoubleOrDefault("dynamicLagPowerFactor", 0d);
        dynamicLagMinSpeed = this.getDoubleOrDefault("dynamicLagMinSpeed", 0d);
        dynamicFlyBlockSpeedFactor = this.getDoubleOrDefault("dynamicFlyBlockSpeedFactor", 0d);
        dynamicFlyBlock = this.getMaterial("dynamicFlyBlock");
        chestPenalty = this.getDoubleOrDefault("chestPenalty", 0);
        gravityInclineDistance = this.getIntOrDefault("gravityInclineDistance", -1);
        int dropdist = this.getIntOrDefault("gravityDropDistance", -8);
        gravityDropDistance = dropdist > 0 ? -dropdist : dropdist;
        collisionSound = Sound.valueOf((String) backingData.getOrDefault("collisionSound", "BLOCK_ANVIL_LAND"));
        fuelTypes = new HashMap<>();
        Map<Object, Object> fTypes = (Map<Object, Object>) backingData.getOrDefault("fuelTypes", new HashMap<>());
        if (!fTypes.isEmpty()) {
            for (Object k : fTypes.keySet()) {
                Material type;
                type = Material.getMaterial(((String) k).toUpperCase());
                Object v = fTypes.get(k);
                double burnRate;
                if (v instanceof String) {
                    burnRate = Double.parseDouble((String) v);
                } else if (v instanceof Integer) {
                    burnRate = ((Integer) v).doubleValue();
                } else {
                    burnRate = (double) v;
                }
                fuelTypes.put(type, burnRate);
            }
        } else {
            fuelTypes.put(Material.COAL_BLOCK, 79.0);
            fuelTypes.put(Material.COAL, 7.0);
        }
        disableTeleportToWorlds = new HashSet<>();
        List<String> disabledWorlds = (List<String>) backingData.getOrDefault("disableTeleportToWorlds", new ArrayList<>());
        disableTeleportToWorlds.addAll(disabledWorlds);
        teleportationCooldown = this.getIntOrDefault("teleportationCooldown", 0);
        gearShifts = Math.max(this.getIntOrDefault("gearShifts", 1), 1);
        gearShiftsAffectTickCooldown = this.getBooleanOrDefault("gearShiftsAffectTickCooldown", true);
        gearShiftsAffectDirectMovement = this.getBooleanOrDefault("gearShiftsAffectDirectMovement", false);
        gearShiftsAffectCruiseSkipBlocks = this.getBooleanOrDefault("gearShiftsAffectCruiseSkipBlocks", false);
    }

    private boolean containsKey(String key){
        return this.backingData.containsKey(key);
    }

    private void requireOneOf(String... keys){
        for(String key : keys){
            if(this.containsKey(key)){
                return;
            }
        }
        throw new IllegalArgumentException("No keys found for " + Arrays.toString(keys));
    }

    private void requireKey(String key){
        if(!this.containsKey(key)){
            throw new IllegalArgumentException("No key found for " + key);
        }
    }

    public boolean getBoolean(String key){
        requireKey(key);
        return (Boolean) backingData.get(key);
    }

    public boolean getBooleanOrDefault(String key, boolean defaultValue){
        return (Boolean) backingData.getOrDefault(key, defaultValue);
    }

    public int getInt(String key){
        requireKey(key);
        return (Integer) backingData.get(key);
    }

    public int getIntOrDefault(String key, int defaultValue){
        return (Integer) backingData.getOrDefault(key, defaultValue);
    }

    public double getDouble(String key){
        requireKey(key);
        return (Double) backingData.get(key);
    }

    public double getDoubleOrDefault(String key, double defaultValue){
        return (Double) backingData.getOrDefault(key, defaultValue);
    }

    public Material getMaterial(String key){
        requireKey(key);
        return Material.valueOf((String) backingData.get(key));
    }
    public Material getMaterialOrDefault(String key, Material defaultValue){
        return this.containsKey(key) ? Material.valueOf((String) backingData.get(key)) : defaultValue;
    }

    private float floatFromObject(Object obj) {
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        }
        return (float) obj;
    }
    
    private Map<String, Integer> stringToIntMapFromObject(Object obj) {
        HashMap<String, Integer> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Integer i = (Integer) objMap.get(key);
            returnMap.put(str, i);
        }
        return returnMap;
    }
    
    private Map<String, Double> stringToDoubleMapFromObject(Object obj) {
        HashMap<String, Double> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Double d = (Double) objMap.get(key);
            returnMap.put(str, d);
        }
        return returnMap;
    }

    public EnumSet<Material> getBlockIDSet(String key){
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        requireKey(key);
        if(!(this.backingData.get(key) instanceof ArrayList)){
            throw new IllegalArgumentException("key " + key + " must be a list of materials.");
        }
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            String materialName = (String) object;
            returnList.add(Material.valueOf(materialName));
        }
        return returnList;
    }

    public EnumSet<Material> getBlockIDSetOrEmpty(String key){
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        if(!(this.backingData.get(key) instanceof ArrayList)){
            return returnList;
        }
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            String materialName = (String) object;
            returnList.add(Material.valueOf(materialName));
        }
        return returnList;
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

    private Map<List<Material>, List<Double>> blockIDMapListFromObject(Object obj) {
        HashMap<List<Material>, List<Double>> returnMap = new HashMap<>();
        HashMap<Object, Object> objMap = (HashMap<Object, Object>) obj;
        for (Object i : objMap.keySet()) {
            ArrayList<Material> rowList = new ArrayList<>();

            // first read in the list of the blocks that type of flyblock. It could be a single string (with or without a ":") or integer, or it could be multiple of them
            if (i instanceof ArrayList<?>) {
                for (Object o : (ArrayList<Object>) i) {
                    rowList.add(Material.valueOf((String) o));
                }
            } else  {
                rowList.add(Material.valueOf((String) i));
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

    @NotNull
    public EnumSet<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    @NotNull
    public EnumSet<Material> getForbiddenBlocks() {
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

    @Deprecated
    public int getCruiseSkipBlocks() {
        return cruiseSkipBlocks;
    }
    public int getCruiseSkipBlocks(@NotNull World world) {
        return perWorldCruiseSkipBlocks.getOrDefault(world.getName(), cruiseSkipBlocks);
    }

    @Deprecated
    public int getVertCruiseSkipBlocks() {
        return vertCruiseSkipBlocks;
    }

    public int getVertCruiseSkipBlocks(@NotNull World world) {
        return perWorldVertCruiseSkipBlocks.getOrDefault(world.getName(), vertCruiseSkipBlocks);
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

    @Deprecated
    public double getFuelBurnRate() {
        return fuelBurnRate;
    }
    public double getFuelBurnRate(@NotNull World world) {
        return perWorldFuelBurnRate.getOrDefault(world.getName(), fuelBurnRate);
    }

    public double getSinkPercent() {
        return sinkPercent;
    }

    public double getOverallSinkPercent() {
        return overallSinkPercent;
    }

    @Deprecated
    public double getDetectionMultiplier() {
        return detectionMultiplier;
    }
    public double getDetectionMultiplier(@NotNull World world) {
        return perWorldDetectionMultiplier.getOrDefault(world.getName(), detectionMultiplier);
    }

    @Deprecated
    public double getUnderwaterDetectionMultiplier() {
        return underwaterDetectionMultiplier;
    }
    public double getUnderwaterDetectionMultiplier(@NotNull World world) {
        return perWorldUnderwaterDetectionMultiplier.getOrDefault(world.getName(), underwaterDetectionMultiplier);
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

    @Deprecated
    public int getTickCooldown() {
        return tickCooldown;
    }
    public int getTickCooldown(@NotNull World world) {
        return perWorldTickCooldown.getOrDefault(world.getName(), tickCooldown);
    }

    @Deprecated
    public int getCruiseTickCooldown() {
        return cruiseTickCooldown;
    }

    public int getCruiseTickCooldown(@NotNull World world) {
        return perWorldCruiseTickCooldown.getOrDefault(world.getName(), cruiseTickCooldown);
    }

    @Deprecated
    public int getVertCruiseTickCooldown() {
        return vertCruiseTickCooldown;
    }

    public int getVertCruiseTickCooldown(@NotNull World world) {
        return perWorldVertCruiseTickCooldown.getOrDefault(world.getName(), vertCruiseTickCooldown);
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
    public Map<List<Material>, List<Double>> getFlyBlocks() {
        return flyBlocks;
    }

    @NotNull
    public Map<List<Material>, List<Double>> getMoveBlocks() {
        return moveBlocks;
    }

    @Deprecated
    public int getMaxHeightLimit() {
        return maxHeightLimit;
    }
    public int getMaxHeightLimit(@NotNull World world) {
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), maxHeightLimit);
    }

    @Deprecated
    public int getMinHeightLimit() {
        return minHeightLimit;
    }
    public int getMinHeightLimit(@NotNull World world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), minHeightLimit);
    }

    @Deprecated
    public int getMaxHeightAboveGround() {
        return maxHeightAboveGround;
    }
    public int getMaxHeightAboveGround(@NotNull World world) {
        return perWorldMaxHeightAboveGround.getOrDefault(world.getName(), maxHeightAboveGround);
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
    public EnumSet<Material> getHarvestBlocks() {
        return harvestBlocks;
    }

    @NotNull
    public EnumSet<Material> getHarvesterBladeBlocks() {
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

    public Material getDynamicFlyBlock() {
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

    public Map<Material, Double> getFuelTypes() {
        return fuelTypes;
    }

    @NotNull
    public Set<String> getDisableTeleportToWorlds() {
        return disableTeleportToWorlds;
    }

    public int getTeleportationCooldown() {
        return teleportationCooldown;
    }

    public int getGearShifts() {
        return gearShifts;
    }

    public boolean getGearShiftsAffectTickCooldown() {
        return gearShiftsAffectTickCooldown;
    }

    public boolean getGearShiftsAffectDirectMovement() {
        return gearShiftsAffectDirectMovement;
    }

    public boolean getGearShiftsAffectCruiseSkipBlocks() {
        return gearShiftsAffectCruiseSkipBlocks;
    }

    public class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}