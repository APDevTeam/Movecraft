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

import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final public class CraftType {


    private static final List<IntegerProperty> intProperties = new ArrayList<>();

    /**
     * Register an integer property with Movecraft
     *
     * @param integerProperty property to register
     */
    public static void registerIntProperty(IntegerProperty integerProperty) {
        intProperties.add(integerProperty);
    }

    static {
        // Required properties
        intProperties.add(new IntegerProperty("maxSize"));
        intProperties.add(new IntegerProperty("minSize"));

        // Optional properties
        intProperties.add(new IntegerProperty("minHeightLimit", type -> 0));
        intProperties.add(new IntegerProperty("maxHeightLimit", type -> 255));
        intProperties.add(new IntegerProperty("cruiseOnPilotVertMove", type -> 0));
        intProperties.add(new IntegerProperty("maxHeightAboveGround", type -> -1));
        intProperties.add(new IntegerProperty("maxStaticMove", type -> 10000));
        intProperties.add(new IntegerProperty("cruiseSkipBlocks", type -> 0));
        intProperties.add(new IntegerProperty("vertCruiseSkipBlocks", type -> type.getIntProperty("cruiseSkipBlocks")));
        intProperties.add(new IntegerProperty("staticWaterLevel", type -> 0));
        intProperties.add(new IntegerProperty("smokeOnSink", type -> 0));
        intProperties.add(new IntegerProperty("releaseTimeout", type -> 30));
        intProperties.add(new IntegerProperty("hoverLimit", type -> 0));
        intProperties.add(new IntegerProperty("teleportationCooldown", type -> 0));
        intProperties.add(new IntegerProperty("gravityInclineDistance", type -> -1));
        intProperties.add(new IntegerProperty("gearShifts", type -> 1));
    }

    private final Map<String, Integer> intPropertyMap;

    /**
     * Get an integer property of this CraftType
     *
     * @param key Key of the integer property
     * @return value of the integer property
     */
    public int getIntProperty(String key) {
        return intPropertyMap.get(key);
    }



    private static final List<BooleanProperty> boolProperties = new ArrayList<>();

    /**
     * Register a boolean property with Movecraft
     *
     * @param booleanProperty property to register
     */
    public static void registerBoolProperty(BooleanProperty booleanProperty) {
        boolProperties.add(booleanProperty);
    }

    static {
        // Optional properties
        boolProperties.add(new BooleanProperty("requireWaterContact", type -> false));
        boolProperties.add(new BooleanProperty("tryNudge", type -> false));
        boolProperties.add(new BooleanProperty("canCruise", type -> false));
        boolProperties.add(new BooleanProperty("canTeleport", type -> false));
        boolProperties.add(new BooleanProperty("canBeNamed", type -> true));
        boolProperties.add(new BooleanProperty("canSwitchWorld", type -> false));
        boolProperties.add(new BooleanProperty("canStaticMove", type -> false));
        boolProperties.add(new BooleanProperty("canHover", type -> false));
        boolProperties.add(new BooleanProperty("canDirectControl", type -> true));
        boolProperties.add(new BooleanProperty("useGravity", type -> false));
        boolProperties.add(new BooleanProperty("canHoverOverWater", type -> true));
        boolProperties.add(new BooleanProperty("moveEntities", type -> true));
        boolProperties.add(new BooleanProperty("onlyMovePlayers", type -> true));
        boolProperties.add(new BooleanProperty("allowHorizontalMovement", type -> true));
        boolProperties.add(new BooleanProperty("allowVerticalMovement", type -> true));
        boolProperties.add(new BooleanProperty("allowRemoteSign", type -> true));
        boolProperties.add(new BooleanProperty("cruiseOnPilot", type -> false));
        boolProperties.add(new BooleanProperty("allowVerticalTakeoffAndLanding", type -> true));
        boolProperties.add(new BooleanProperty("rotateAtMidpoint", type -> false));
        boolProperties.add(new BooleanProperty("halfSpeedUnderwater", type -> false));
        boolProperties.add(new BooleanProperty("focusedExplosion", type -> false));
        boolProperties.add(new BooleanProperty("mustBeSubcraft", type -> false));
        boolProperties.add(new BooleanProperty("keepMovingOnSink", type -> false));
        boolProperties.add(new BooleanProperty("gearShiftsAffectTickCooldown", type -> true));
        boolProperties.add(new BooleanProperty("gearShiftsAffectDirectMovement", type -> false));
        boolProperties.add(new BooleanProperty("gearShiftsAffectCruiseSkipBlocks", type -> false));
    }

    private final Map<String, Boolean> boolPropertyMap;

    /**
     * Get a boolean property of this CraftType
     *
     * @param key Key of the boolean property
     * @return value of the boolean property
     */
    public boolean getBoolProperty(String key) {
        return boolPropertyMap.get(key);
    }



    public static final List<Pair<Predicate<CraftType>, String>> validators = new ArrayList<>();

    /**
     * Register a craft type validator
     *
     * @param validator validator to parse the craft type
     * @param errorMessage message to throw on failure
     */
    public static void registerTypeValidator(Predicate<CraftType> validator, String errorMessage) {
        validators.add(new Pair<>(validator, errorMessage));
    }

    static {
        validators.add(new Pair<>(
                type -> type.getIntProperty("minHeightLimit") < type.getIntProperty("maxHeightLimit"),
                "minHeightLimit must be less than maxHeightLimit"
        ));
        validators.add(new Pair<>(
                type -> type.getIntProperty("minHeightLimit") >= 0 && type.getIntProperty("minHeightLimit") <= 255,
                "minHeightLimit must be between 0 and 255"
        ));
        validators.add(new Pair<>(
                type -> type.getIntProperty("maxHeightLimit") >= 0 && type.getIntProperty("maxHeightLimit") <= 255,
                "maxHeightLimit must be between 0 and 255"
        ));
        validators.add(new Pair<>(
                type -> type.getIntProperty("hoverLimit") <= 0,
                "hoverLimit must be greater than or equal to zero"
        ));
        validators.add(new Pair<>(
                type -> type.getIntProperty("gearShifts") <= 1,
                "gearShifts must be greater than or equal to one"
        ));
    }



    // problem child properties
    private final int cruiseTickCooldown;
    private final int vertCruiseTickCooldown;
    private final int sinkRateTicks;
    private final int tickCooldown;
    private final int gravityDropDistance;
    private final boolean blockedByWater;

    @NotNull private final Map<String, Integer> perWorldMinHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightAboveGround;
    @NotNull private final Map<String, Integer> perWorldCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldVertCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    @NotNull private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
    @NotNull private final Map<String, Integer> perWorldTickCooldown; // speed setting
    private final EnumSet<Material> dynamicFlyBlocks;
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
    @NotNull private final Set<String> forbiddenSignStrings;
    @NotNull private final Map<List<Material>, List<Double>> flyBlocks;
    @NotNull private final Map<List<Material>, List<Double>> moveBlocks;
    @NotNull private final EnumSet<Material> harvestBlocks;
    @NotNull private final EnumSet<Material> harvesterBladeBlocks;
    @NotNull private final EnumSet<Material> passthroughBlocks;
    @NotNull private final EnumSet<Material> forbiddenHoverOverBlocks;
    @NotNull private final Map<Material, Double> fuelTypes;
    @NotNull private final Set<String> disableTeleportToWorlds;
    private final Sound collisionSound;

    public CraftType(File f) {
        TypeData data = TypeData.loadConfiguration(f);

        // Load integer properties
        intPropertyMap = new HashMap<>();
        for(IntegerProperty i : intProperties) {
            Integer value = i.load(data, this);
            if(value != null)
                intPropertyMap.put(i.getKey(), value);
        }

        // Load boolean properties
        boolPropertyMap = new HashMap<>();
        for(BooleanProperty i : boolProperties) {
            Boolean value = i.load(data, this);
            if(value != null)
                boolPropertyMap.put(i.getKey(), value);
        }

        // Validate craft type
        for(var i : validators) {
            if(!i.getLeft().test(this))
                throw new IllegalArgumentException(i.getRight());
        }


        //Required craft flags
        craftName = data.getString("name");
        allowedBlocks = data.getMaterials("allowedBlocks");

        forbiddenSignStrings = data.getStringListOrEmpty("forbiddenSignStrings").stream().map(String::toLowerCase).collect(Collectors.toSet());
        tickCooldown = (int) Math.ceil(20 / (data.getDouble("speed")));
        perWorldTickCooldown = new HashMap<>();
        Map<String, Double> tickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldSpeed").getBackingData());
        tickCooldownMap.forEach((world, speed) -> perWorldTickCooldown.put(world, (int) Math.ceil(20 / speed)));
        flyBlocks = blockIDMapListFromObject("flyblocks", data.getDataOrEmpty("flyblocks").getBackingData());

        //Optional craft flags
        forbiddenBlocks = data.getMaterialsOrEmpty("forbiddenBlocks");
        blockedByWater = data.getBooleanOrDefault("canFly", data.getBooleanOrDefault("blockedByWater", true));
        moveBlocks = blockIDMapListFromObject("moveblocks", data.getDataOrEmpty("moveblocks").getBackingData());
        perWorldCruiseSkipBlocks = stringToIntMapFromObject(data.getDataOrEmpty("perWorldCruiseSkipBlocks").getBackingData());
        perWorldVertCruiseSkipBlocks = stringToIntMapFromObject(data.getDataOrEmpty("perWorldVertCruiseSkipBlocks").getBackingData());
        fuelBurnRate = data.getDoubleOrDefault("fuelBurnRate", 0d);
        perWorldFuelBurnRate = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldFuelBurnRate").getBackingData());
        sinkPercent = data.getDoubleOrDefault("sinkPercent", 0d);
        overallSinkPercent = data.getDoubleOrDefault("overallSinkPercent", 0d);
        detectionMultiplier = data.getDoubleOrDefault("detectionMultiplier", 0d);
        perWorldDetectionMultiplier = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldDetectionMultiplier").getBackingData());
        underwaterDetectionMultiplier = data.getDoubleOrDefault("underwaterDetectionMultiplier", detectionMultiplier);
        perWorldUnderwaterDetectionMultiplier = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldUnderwaterDetectionMultiplier").getBackingData());
        sinkRateTicks = data.getIntOrDefault("sinkRateTicks", (int) Math.ceil(20 / data.getDoubleOrDefault("sinkSpeed", -200))); // default becomes 0
        explodeOnCrash = (float) data.getDoubleOrDefault("explodeOnCrash", 0D);
        collisionExplosion = (float) data.getDoubleOrDefault("collisionExplosion", 0D);
        perWorldMinHeightLimit = new HashMap<>();
        Map<String, Integer> minHeightMap = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMinHeightLimit").getBackingData());
        minHeightMap.forEach((world, height) -> perWorldMinHeightLimit.put(world, Math.max(0, height)));

        double cruiseSpeed = data.getDoubleOrDefault("cruiseSpeed", 20.0 / tickCooldown);
        cruiseTickCooldown = (int) Math.round((1.0 + getIntProperty("cruiseSkipBlocks")) * 20.0 / cruiseSpeed);
        perWorldCruiseTickCooldown = new HashMap<>();
        Map<String, Double> cruiseTickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldCruiseSpeed").getBackingData());
        cruiseTickCooldownMap.forEach((world, speed) -> {
            double worldCruiseSkipBlocks = perWorldCruiseSkipBlocks.getOrDefault(world, getIntProperty("cruiseSkipBlocks"));
            perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldCruiseSkipBlocks) * 20.0 / cruiseSpeed));
        });
        tickCooldownMap.forEach((world, speed) -> {
            if (!perWorldCruiseTickCooldown.containsKey(world)) {
                perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + getIntProperty("cruiseSkipBlocks")) * 20.0 / speed));
            }
        });

        double vertCruiseSpeed = data.getDoubleOrDefault("vertCruiseSpeed", cruiseSpeed);
        vertCruiseTickCooldown = (int) Math.round((1.0 + getIntProperty("vertCruiseSkipBlocks")) * 20.0 / vertCruiseSpeed);
        perWorldVertCruiseTickCooldown = new HashMap<>();
        Map<String, Double> vertCruiseTickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldVertCruiseSpeed").getBackingData());
        vertCruiseTickCooldownMap.forEach((world, speed) -> {
            double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty("vertCruiseSkipBlocks"));
            perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / speed));
        });
        cruiseTickCooldownMap.forEach((world, speed) -> {
            if (!perWorldVertCruiseTickCooldown.containsKey(world)) {
                double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty("vertCruiseSkipBlocks"));
                perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / vertCruiseSpeed));
            }
        });

        perWorldMaxHeightLimit = new HashMap<>();
        Map<String, Integer> maxHeightMap = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMaxHeightLimit").getBackingData());
        maxHeightMap.forEach((world, height) -> {
            int worldValue = Math.min(height, 255);
            int worldMinHeight = perWorldMinHeightLimit.getOrDefault(world, getIntProperty("minHeightLimit"));
            if (worldValue <= worldMinHeight) worldValue = 255;
            perWorldMaxHeightLimit.put(world, worldValue);
        });
        
        perWorldMaxHeightAboveGround = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMaxHeightAboveGround").getBackingData());
        harvestBlocks = data.getMaterialsOrEmpty("harvestBlocks");
        harvesterBladeBlocks = data.getMaterialsOrEmpty("harvesterBladeBlocks");
        passthroughBlocks = data.getMaterialsOrEmpty("passthroughBlocks");
        if(!blockedByWater){
            passthroughBlocks.add(Material.WATER);
        }
        forbiddenHoverOverBlocks = data.getMaterialsOrEmpty("forbiddenHoverOverBlocks");
        if (!getBoolProperty("canHoverOverWater")){
            forbiddenHoverOverBlocks.add(Material.WATER);
        }
        dynamicLagSpeedFactor = data.getDoubleOrDefault("dynamicLagSpeedFactor", 0d);
        dynamicLagPowerFactor = data.getDoubleOrDefault("dynamicLagPowerFactor", 0d);
        dynamicLagMinSpeed = data.getDoubleOrDefault("dynamicLagMinSpeed", 0d);
        dynamicFlyBlockSpeedFactor = data.getDoubleOrDefault("dynamicFlyBlockSpeedFactor", 0d);
        dynamicFlyBlocks = data.getMaterialsOrEmpty("dynamicFlyBlock");
        chestPenalty = data.getDoubleOrDefault("chestPenalty", 0);
        int dropdist = data.getIntOrDefault("gravityDropDistance", -8);
        gravityDropDistance = dropdist > 0 ? -dropdist : dropdist;
        collisionSound = data.getSoundOrDefault("collisionSound",  Sound.sound(Key.key("block.anvil.land"), Sound.Source.NEUTRAL, 2.0f,1.0f));
        fuelTypes = new HashMap<>();
        Map<String, Object> fTypes =  data.getDataOrEmpty("fuelTypes").getBackingData();
        if (!fTypes.isEmpty()) {
            for (String k : fTypes.keySet()) {
                Material type;
                type = Material.getMaterial(k.toUpperCase());
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
            fuelTypes.put(Material.CHARCOAL, 7.0);
        }
        disableTeleportToWorlds = new HashSet<>();
        List<String> disabledWorlds = data.getStringListOrEmpty("disableTeleportToWorlds");
        disableTeleportToWorlds.addAll(disabledWorlds);
    }

    private Map<String, Integer> stringToIntMapFromObject(Map<String, Object> objMap) {
        HashMap<String, Integer> returnMap = new HashMap<>();
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Integer i = (Integer) objMap.get(key);
            returnMap.put(str, i);
        }
        return returnMap;
    }
    
    private Map<String, Double> stringToDoubleMapFromObject(Map<String, Object> objMap) {
        HashMap<String, Double> returnMap = new HashMap<>();
        for (Object key : objMap.keySet()) {
            String str = (String) key;
            Double d = (Double) objMap.get(key);
            returnMap.put(str, d);
        }
        return returnMap;
    }

    private Map<List<Material>, List<Double>> blockIDMapListFromObject(String key, Map<String, Object> objMap) {
        HashMap<List<Material>, List<Double>> returnMap = new HashMap<>();
        for (Object i : objMap.keySet()) {
            ArrayList<Material> rowList = new ArrayList<>();

            // first read in the list of the blocks that type of flyblock. It could be a single string (with or without a ":") or integer, or it could be multiple of them
            if (i instanceof ArrayList) {
                for (Object o : (ArrayList<?>) i) {
                    if (!(o instanceof String)) {
                        if(o == null){
                            throw new IllegalArgumentException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
                        }
                        throw new IllegalArgumentException("Entry " + o + " must be a material for key " + key);
                    }
                    var string = (String) o;
                    var tagSet = Tags.parseBlockRegistry(string);
                    if(tagSet == null){
                        rowList.add(Material.valueOf(string.toUpperCase()));
                    } else {
                        if(tagSet.isEmpty()){
                            throw new IllegalArgumentException("Entry " + string + " describes an empty or non-existent Tag for key " + key);
                        }
                        rowList.addAll(tagSet);
                    }
                }
            } else  {
                if (!(i instanceof String)) {
                    if(i == null){
                        throw new IllegalArgumentException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
                    }
                    throw new IllegalArgumentException("Entry " + i + " must be a material for key " + key);
                }
                var string = (String) i;
                var tagSet = Tags.parseBlockRegistry(string);
                if(tagSet == null){
                    rowList.add(Material.valueOf(string.toUpperCase()));
                } else {
                    if(tagSet.isEmpty()){
                        throw new IllegalArgumentException("Entry " + string + " describes an empty or non-existent Tag for key " + key);
                    }
                    rowList.addAll(tagSet);
                }
            }

            // then read in the limitation values, low and high
            ArrayList<?> objList = (ArrayList<?>) objMap.get(i);
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
            if(limitList.size() != 2){
                throw new IllegalArgumentException("Range must be a pair, but found " + limitList.size() + " entries");
            }
            returnMap.put(rowList, limitList);
        }
        return returnMap;
    }

    @NotNull
    public String getCraftName() {
        return craftName;
    }

    @NotNull
    public EnumSet<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    @NotNull
    public EnumSet<Material> getForbiddenBlocks() {
        return forbiddenBlocks;
    }

    @NotNull
    public Set<String> getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public boolean blockedByWater() {
        return blockedByWater;
    }

    public int getCruiseSkipBlocks(@NotNull World world) {
        return perWorldCruiseSkipBlocks.getOrDefault(world.getName(), getIntProperty("cruiseSkipBlocks"));
    }

    public int getVertCruiseSkipBlocks(@NotNull World world) {
        return perWorldVertCruiseSkipBlocks.getOrDefault(world.getName(), getIntProperty("vertCruiseSkipBlocks"));
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

    public float getExplodeOnCrash() {
        return explodeOnCrash;
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

    @NotNull
    public Map<List<Material>, List<Double>> getFlyBlocks() {
        return flyBlocks;
    }

    @NotNull
    public Map<List<Material>, List<Double>> getMoveBlocks() {
        return moveBlocks;
    }

    public int getMaxHeightLimit(@NotNull World world) {
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), getIntProperty("maxHeightLimit"));
    }

    public int getMaxHeightLimit(@NotNull MovecraftWorld world) {
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), getIntProperty("maxHeightLimit"));
    }

    public int getMinHeightLimit(@NotNull World world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), getIntProperty("minHeightLimit"));
    }

    public int getMinHeightLimit(@NotNull MovecraftWorld world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), getIntProperty("minHeightLimit"));
    }

    public int getMaxHeightAboveGround(@NotNull World world) {
        return perWorldMaxHeightAboveGround.getOrDefault(world.getName(), getIntProperty("maxHeightAboveGround"));
    }

    @NotNull
    public EnumSet<Material> getHarvestBlocks() {
        return harvestBlocks;
    }

    @NotNull
    public EnumSet<Material> getHarvesterBladeBlocks() {
        return harvesterBladeBlocks;
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

    public EnumSet<Material> getDynamicFlyBlocks() {
        return dynamicFlyBlocks;
    }

    public double getChestPenalty() {
        return chestPenalty;
    }

    @NotNull
    public Set<Material> getPassthroughBlocks() {
        return passthroughBlocks;
    }

    @NotNull
    public Set<Material> getForbiddenHoverOverBlocks() {
        return forbiddenHoverOverBlocks;
    }

    public int getGravityDropDistance() {
        return gravityDropDistance;
    }

    @NotNull
    public Sound getCollisionSound() {
        return collisionSound;
    }

    @NotNull
    public Map<Material, Double> getFuelTypes() {
        return fuelTypes;
    }

    @NotNull
    public Set<String> getDisableTeleportToWorlds() {
        return disableTeleportToWorlds;
    }

    public static class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}