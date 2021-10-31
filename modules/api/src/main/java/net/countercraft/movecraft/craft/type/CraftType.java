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

package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.property.BooleanProperty;
import net.countercraft.movecraft.craft.type.property.DoubleProperty;
import net.countercraft.movecraft.craft.type.property.FloatProperty;
import net.countercraft.movecraft.craft.type.property.ObjectProperty;
import net.countercraft.movecraft.craft.type.property.IntegerProperty;
import net.countercraft.movecraft.craft.type.property.MaterialSetProperty;
import net.countercraft.movecraft.craft.type.property.Property;
import net.countercraft.movecraft.craft.type.property.StringProperty;
import net.countercraft.movecraft.craft.type.transform.BooleanTransform;
import net.countercraft.movecraft.craft.type.transform.DoubleTransform;
import net.countercraft.movecraft.craft.type.transform.FloatTransform;
import net.countercraft.movecraft.craft.type.transform.IntegerTransform;
import net.countercraft.movecraft.craft.type.transform.MaterialSetTransform;
import net.countercraft.movecraft.craft.type.transform.ObjectTransform;
import net.countercraft.movecraft.craft.type.transform.StringTransform;
import net.countercraft.movecraft.craft.type.transform.Transform;
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
    static final List<Property<?>> properties = new ArrayList<>();

    /**
     * Register a property with Movecraft
     *
     * @param property property to register
     */
    public static void registerProperty(Property<?> property) {
        properties.add(property);
    }



    private Map<String, String> stringPropertyMap;
    /**
     * Get a string property of this CraftType
     *
     * @param key Key of the string property
     * @return value of the string property
     */
    public String getStringProperty(String key) {
        return stringPropertyMap.get(key);
    }

    private Map<String, Integer> intPropertyMap;
    /**
     * Get an integer property of this CraftType
     *
     * @param key Key of the integer property
     * @return value of the integer property
     */
    public int getIntProperty(String key) {
        return intPropertyMap.get(key);
    }

    private Map<String, Boolean> boolPropertyMap;
    /**
     * Get a boolean property of this CraftType
     *
     * @param key Key of the boolean property
     * @return value of the boolean property
     */
    public boolean getBoolProperty(String key) {
        return boolPropertyMap.get(key);
    }

    private Map<String, Float> floatPropertyMap;
    /**
     * Get a float property of this CraftType
     *
     * @param key Key of the float property
     * @return value of the float property
     */
    public float getFloatProperty(String key) {
        return floatPropertyMap.get(key);
    }

    private Map<String, Double> doublePropertyMap;
    /**
     * Get a double property of this CraftType
     *
     * @param key Key of the double property
     * @return value of the double property
     */
    public double getDoubleProperty(String key) {
        return doublePropertyMap.get(key);
    }

    private Map<String, Object> objectPropertyMap;
    /**
     * Get an object property of this CraftType
     * Note: Object properties have no type safety, it is expected that the addon developer handle type safety
     *
     * @param key Key of the object property
     * @return value of the object property
     */
    @Nullable
    public Object getObjectProperty(String key) {
        return objectPropertyMap.get(key);
    }

    private Map<String, EnumSet<Material>> materialSetPropertyMap;
    /**
     * Get a material set property of this CraftType
     *
     * @param key Key of the string property
     * @return value of the string property
     */
    public EnumSet<Material> getMaterialSetProperty(String key) {
        return materialSetPropertyMap.get(key);
    }



    static final List<Transform<?>> transforms = new ArrayList<>();

    /**
     * Register a craft type transform
     *
     * @param transform transform to modify the craft type
     */
    public static void registerTypeTransform(Transform<?> transform) {
        transforms.add(transform);
    }



    private static final List<Pair<Predicate<CraftType>, String>> validators = new ArrayList<>();

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
        // Required properties
        registerProperty(new StringProperty("name"));
        registerProperty(new IntegerProperty("maxSize"));
        registerProperty(new IntegerProperty("minSize"));
        registerProperty(new MaterialSetProperty("allowedBlocks"));
        registerProperty(new DoubleProperty("speed"));

        // Optional properties
        // TODO: forbiddenSignStrings
        // TODO: perWorldSpeed -> perWorldTickCooldown
        // TODO: flyBlocks
        registerProperty(new MaterialSetProperty("forbiddenBlocks", type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("blockedByWater", type -> true));
        registerProperty(new BooleanProperty("canFly", type -> type.getBoolProperty("blockedByWater")));
        registerProperty(new BooleanProperty("requireWaterContact", type -> false));
        registerProperty(new BooleanProperty("tryNudge", type -> false));
        // TODO: moveblocks
        registerProperty(new BooleanProperty("canCruise", type -> false));
        registerProperty(new BooleanProperty("canTeleport", type -> false));
        registerProperty(new BooleanProperty("canSwitchWorld", type -> false));
        registerProperty(new BooleanProperty("canBeNamed", type -> true));
        registerProperty(new BooleanProperty("cruiseOnPilot", type -> false));
        registerProperty(new IntegerProperty("cruiseOnPilotVertMove", type -> 0));
        registerProperty(new BooleanProperty("allowVerticalMovement", type -> true));
        registerProperty(new BooleanProperty("rotateAtMidpoint", type -> false));
        registerProperty(new BooleanProperty("allowHorizontalMovement", type -> true));
        registerProperty(new BooleanProperty("allowRemoteSign", type -> true));
        registerProperty(new BooleanProperty("canStaticMove", type -> false));
        registerProperty(new IntegerProperty("maxStaticMove", type -> 10000));
        registerProperty(new IntegerProperty("cruiseSkipBlocks", type -> 0));
        // TODO: perWorldCruiseSkipBlocks
        registerProperty(new IntegerProperty("vertCruiseSkipBlocks", type -> type.getIntProperty("cruiseSkipBlocks")));
        // TODO: perWorldVertCruiseSkipBlocks
        registerProperty(new BooleanProperty("halfSpeedUnderwater", type -> false));
        registerProperty(new BooleanProperty("focusedExplosion", type -> false));
        registerProperty(new BooleanProperty("mustBeSubcraft", type -> false));
        registerProperty(new IntegerProperty("staticWaterLevel", type -> 0));
        registerProperty(new DoubleProperty("fuelBurnRate", type -> 0D));
        // TODO: perWorldFuelBurnRate
        registerProperty(new DoubleProperty("sinkPercent", type -> 0D));
        registerProperty(new DoubleProperty("overallSinkPercent", type -> 0D));
        registerProperty(new DoubleProperty("detectionMultiplier", type -> 0D));
        // TODO: perWorldDetectionMultiplier
        registerProperty(new DoubleProperty("underwaterDetectionMultiplier", type-> type.getDoubleProperty("detectionMultplier")));
        registerProperty(new DoubleProperty("sinkSpeed", type -> 1D));
        registerProperty(new IntegerProperty("sinkRateTicks", type -> (int) Math.ceil(20 / type.getDoubleProperty("sinkSpeed"))));
        registerProperty(new BooleanProperty("keepMovingOnSink", type -> false));
        registerProperty(new IntegerProperty("smokeOnSink", type -> 0));
        registerProperty(new FloatProperty("explodeOnCrash", type -> 0F));
        registerProperty(new FloatProperty("collisionExplosion", type -> 0F));
        registerProperty(new IntegerProperty("minHeightLimit", type -> 0));
        // TODO: perWorldMinHeightLimit
        registerProperty(new DoubleProperty("cruiseSpeed", type -> 20.0 / type.getIntProperty("tickCooldown")));
        // TODO: perWorldCruiseSpeed -> perWorldCruiseTickCooldown
        registerProperty(new DoubleProperty("vertCruiseSpeed", type -> type.getDoubleProperty("cruiseSpeed")));
        // TODO: perWorldVertCruiseSpeed -> perWorldVertCruiseTickCooldown
        registerProperty(new IntegerProperty("maxHeightLimit", type -> 255));
        // TODO: perWorldMaxHeightLimit
        registerProperty(new IntegerProperty("maxHeightAboveGround", type -> -1));
        // TODO: perWorldMaxHeightAboveGround
        registerProperty(new BooleanProperty("canDirectControl", type -> true));
        registerProperty(new BooleanProperty("canHover", type -> false));
        registerProperty(new BooleanProperty("canHoverOverWater", type -> true));
        registerProperty(new BooleanProperty("moveEntities", type -> true));
        registerProperty(new BooleanProperty("onlyMovePlayers", type -> true));
        registerProperty(new BooleanProperty("useGravity", type -> false));
        registerProperty(new IntegerProperty("hoverLimit", type -> 0));
        registerProperty(new MaterialSetProperty("harvestBlocks", type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("harvesterBladeBlocks", type -> EnumSet.noneOf(Material.class)));
        // TODO: passthroughBlocks
        // TODO: forbiddenHoverOverBlocks
        registerProperty(new BooleanProperty("allowVerticalTakeoffAndLanding", type -> true));
        registerProperty(new DoubleProperty("dynamicLagSpeedFactor", type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagPowerFactor", type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagMinSpeed", type -> 0D));
        registerProperty(new DoubleProperty("dynamicFlyBlockSpeedFactor", type -> 0D));
        registerProperty(new MaterialSetProperty("dynamicFlyBlock", type -> EnumSet.noneOf(Material.class)));
        registerProperty(new DoubleProperty("chestPenalty", type -> 0D));
        registerProperty(new IntegerProperty("gravityInclineDistance", type -> -1));
        // TODO: gravityDropDistance
        // TODO: collisionSound
        // TODO: fuelTypes
        // TODO: disableTeleportToWorlds
        registerProperty(new IntegerProperty("teleportationCooldown", type -> 0));
        registerProperty(new IntegerProperty("gearShifts", type -> 1));
        registerProperty(new BooleanProperty("gearShiftsAffectTickCooldown", type -> true));
        registerProperty(new BooleanProperty("gearShiftsAffectDirectMovement", type -> false));
        registerProperty(new BooleanProperty("gearShiftsAffectCruiseSkipBlocks", type -> false));
        registerProperty(new IntegerProperty("releaseTimeout", type -> 30));

        // Craft type transforms
        registerTypeTransform((IntegerTransform) (data, type) -> {
            int tickCooldown = (int) Math.ceil(20 / type.getDoubleProperty("speed"));
            data.put("tickCooldown", tickCooldown);
            return data;
        });
        registerTypeTransform((DoubleTransform) (data, type) -> {
            data.remove("speed");
            return data;
        });
        registerTypeTransform((BooleanTransform) (data, type) -> {
            data.put("blockedByWater", data.get("canFly"));
            data.remove("canFly");
            return data;
        });
        registerTypeTransform((DoubleTransform) (data, type) -> {
            data.remove("sinkSpeed");
            return data;
        });
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put("cruiseTickCooldown", (int) Math.round((1.0 + type.getIntProperty("cruiseSkipBlocks")) * 20.0 / type.getDoubleProperty("cruiseSpeed")));
            return data;
        });
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put("vertCruiseTickCooldown", (int) Math.round((1.0 + type.getIntProperty("vertCruiseSkipBlocks")) * 20.0 / type.getDoubleProperty("vertCruiseSpeed")));
            return data;
        });
        // TODO: remove cruiseSpeed, vertCruiseSpeed

        // Craft type validators
        registerTypeValidator(
                type -> type.getIntProperty("minHeightLimit") <= type.getIntProperty("maxHeightLimit"),
                "minHeightLimit must be less than or equal to maxHeightLimit"
        );
        registerTypeValidator(
                type -> type.getIntProperty("minHeightLimit") >= 0 && type.getIntProperty("minHeightLimit") <= 255,
                "minHeightLimit must be between 0 and 255"
        );
        registerTypeValidator(
                type -> type.getIntProperty("maxHeightLimit") >= 0 && type.getIntProperty("maxHeightLimit") <= 255,
                "maxHeightLimit must be between 0 and 255"
        );
        registerTypeValidator(
                type -> type.getIntProperty("hoverLimit") <= 0,
                "hoverLimit must be greater than or equal to zero"
        );
        registerTypeValidator(
                type -> type.getIntProperty("gearShifts") <= 1,
                "gearShifts must be greater than or equal to one"
        );
    }



    @NotNull private final EnumSet<Material> passthroughBlocks;
    @NotNull private final EnumSet<Material> forbiddenHoverOverBlocks;
    @NotNull private final Map<String, Integer> perWorldMinHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightAboveGround;
    @NotNull private final Map<String, Integer> perWorldCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldVertCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    @NotNull private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
    @NotNull private final Map<String, Integer> perWorldTickCooldown; // speed setting
    @NotNull private final Map<String, Double> perWorldFuelBurnRate;
    @NotNull private final Map<String, Double> perWorldDetectionMultiplier;
    @NotNull private final Map<String, Double> perWorldUnderwaterDetectionMultiplier;
    @NotNull private final Set<String> forbiddenSignStrings;
    @NotNull private final Map<List<Material>, List<Double>> flyBlocks;
    @NotNull private final Map<List<Material>, List<Double>> moveBlocks;
    @NotNull private final Map<Material, Double> fuelTypes;
    @NotNull private final Set<String> disableTeleportToWorlds;
    private final Sound collisionSound;

    public CraftType(File f) {
        TypeData data = TypeData.loadConfiguration(f);

        // Load craft type properties
        stringPropertyMap = new HashMap<>();
        intPropertyMap = new HashMap<>();
        boolPropertyMap = new HashMap<>();
        floatPropertyMap = new HashMap<>();
        doublePropertyMap = new HashMap<>();
        objectPropertyMap = new HashMap<>();
        materialSetPropertyMap = new HashMap<>();
        for(var i : properties) {
            if(i instanceof StringProperty) {
                String value = ((StringProperty) i).load(data, this);
                if (value != null)
                    stringPropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof IntegerProperty) {
                Integer value = ((IntegerProperty) i).load(data, this);
                if (value != null)
                    intPropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof BooleanProperty) {
                Boolean value = ((BooleanProperty) i).load(data, this);
                if(value != null)
                    boolPropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof FloatProperty) {
                Float value = ((FloatProperty) i).load(data, this);
                if(value != null)
                    floatPropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof DoubleProperty) {
                Double value = ((DoubleProperty) i).load(data, this);
                if(value != null)
                    doublePropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof ObjectProperty) {
                Object value = ((ObjectProperty) i).load(data, this);
                if(value != null)
                    objectPropertyMap.put(i.getKey(), value);
            }
            else if(i instanceof MaterialSetProperty) {
                EnumSet<Material> value = ((MaterialSetProperty) i).load(data, this);
                if(value != null)
                    materialSetPropertyMap.put(i.getKey(), value);
            }
        }

        // Transform craft type
        for(var i : transforms) {
            if(i instanceof StringTransform)
                stringPropertyMap = ((StringTransform) i).transform(stringPropertyMap, this);
            else if(i instanceof IntegerTransform)
                intPropertyMap = ((IntegerTransform) i).transform(intPropertyMap, this);
            else if(i instanceof BooleanTransform)
                boolPropertyMap = ((BooleanTransform) i).transform(boolPropertyMap, this);
            else if(i instanceof FloatTransform)
                floatPropertyMap = ((FloatTransform) i).transform(floatPropertyMap, this);
            else if(i instanceof DoubleTransform)
                doublePropertyMap = ((DoubleTransform) i).transform(doublePropertyMap, this);
            else if(i instanceof ObjectTransform)
                objectPropertyMap = ((ObjectTransform) i).transform(objectPropertyMap, this);
            else if(i instanceof MaterialSetTransform)
                materialSetPropertyMap = ((MaterialSetTransform) i).transform(materialSetPropertyMap, this);
        }

        // Validate craft type
        for(var i : validators) {
            if(!i.getLeft().test(this))
                throw new IllegalArgumentException(i.getRight());
        }


        // Required craft flags
        forbiddenSignStrings = data.getStringListOrEmpty("forbiddenSignStrings").stream().map(String::toLowerCase).collect(Collectors.toSet());
        perWorldTickCooldown = new HashMap<>();
        Map<String, Double> tickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldSpeed").getBackingData());
        tickCooldownMap.forEach((world, speed) -> perWorldTickCooldown.put(world, (int) Math.ceil(20 / speed)));
        flyBlocks = blockIDMapListFromObject("flyblocks", data.getDataOrEmpty("flyblocks").getBackingData());

        // Optional craft flags
        int dropdist = data.getIntOrDefault("gravityDropDistance", -8);
        intPropertyMap.put("gravityDropDistance", dropdist > 0 ? -dropdist : dropdist);

        moveBlocks = blockIDMapListFromObject("moveblocks", data.getDataOrEmpty("moveblocks").getBackingData());
        perWorldCruiseSkipBlocks = stringToIntMapFromObject(data.getDataOrEmpty("perWorldCruiseSkipBlocks").getBackingData());
        perWorldVertCruiseSkipBlocks = stringToIntMapFromObject(data.getDataOrEmpty("perWorldVertCruiseSkipBlocks").getBackingData());
        perWorldFuelBurnRate = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldFuelBurnRate").getBackingData());
        perWorldDetectionMultiplier = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldDetectionMultiplier").getBackingData());
        perWorldUnderwaterDetectionMultiplier = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldUnderwaterDetectionMultiplier").getBackingData());
        perWorldMinHeightLimit = new HashMap<>();
        Map<String, Integer> minHeightMap = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMinHeightLimit").getBackingData());
        minHeightMap.forEach((world, height) -> perWorldMinHeightLimit.put(world, Math.max(0, height)));

        perWorldCruiseTickCooldown = new HashMap<>();
        Map<String, Double> cruiseTickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldCruiseSpeed").getBackingData());
        cruiseTickCooldownMap.forEach((world, speed) -> {
            double worldCruiseSkipBlocks = perWorldCruiseSkipBlocks.getOrDefault(world, getIntProperty("cruiseSkipBlocks"));
            perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldCruiseSkipBlocks) * 20.0 / getDoubleProperty("cruiseSpeed")));
        });
        tickCooldownMap.forEach((world, speed) -> {
            if (!perWorldCruiseTickCooldown.containsKey(world)) {
                perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + getIntProperty("cruiseSkipBlocks")) * 20.0 / speed));
            }
        });

        perWorldVertCruiseTickCooldown = new HashMap<>();
        Map<String, Double> vertCruiseTickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldVertCruiseSpeed").getBackingData());
        vertCruiseTickCooldownMap.forEach((world, speed) -> {
            double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty("vertCruiseSkipBlocks"));
            perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / speed));
        });
        cruiseTickCooldownMap.forEach((world, speed) -> {
            if (!perWorldVertCruiseTickCooldown.containsKey(world)) {
                double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty("vertCruiseSkipBlocks"));
                perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / getDoubleProperty("vertCruiseSpeed")));
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
        passthroughBlocks = data.getMaterialsOrEmpty("passthroughBlocks");
        if(!getBoolProperty("blockedByWater")){
            passthroughBlocks.add(Material.WATER);
        }
        forbiddenHoverOverBlocks = data.getMaterialsOrEmpty("forbiddenHoverOverBlocks");
        if (!getBoolProperty("canHoverOverWater")){
            forbiddenHoverOverBlocks.add(Material.WATER);
        }
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
        }
        else {
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
    public Set<String> getForbiddenSignStrings() {
        return forbiddenSignStrings;
    }

    public int getCruiseSkipBlocks(@NotNull World world) {
        return perWorldCruiseSkipBlocks.getOrDefault(world.getName(), getIntProperty("cruiseSkipBlocks"));
    }

    public int getVertCruiseSkipBlocks(@NotNull World world) {
        return perWorldVertCruiseSkipBlocks.getOrDefault(world.getName(), getIntProperty("vertCruiseSkipBlocks"));
    }

    public double getFuelBurnRate(@NotNull World world) {
        return perWorldFuelBurnRate.getOrDefault(world.getName(), getDoubleProperty("fuelBurnRate"));
    }

    public double getDetectionMultiplier(@NotNull World world) {
        return perWorldDetectionMultiplier.getOrDefault(world.getName(), getDoubleProperty("detectionMultiplier"));
    }

    public double getUnderwaterDetectionMultiplier(@NotNull World world) {
        return perWorldUnderwaterDetectionMultiplier.getOrDefault(world.getName(), getDoubleProperty("underwaterDetectionMultiplier"));
    }

    public int getTickCooldown(@NotNull World world) {
        return perWorldTickCooldown.getOrDefault(world.getName(), getIntProperty("tickCooldown"));
    }

    public int getCruiseTickCooldown(@NotNull World world) {
        return perWorldCruiseTickCooldown.getOrDefault(world.getName(), getIntProperty("cruiseTickCooldown"));
    }

    public int getVertCruiseTickCooldown(@NotNull World world) {
        return perWorldVertCruiseTickCooldown.getOrDefault(world.getName(), getIntProperty("vertCruiseTickCooldown"));
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
    public Set<Material> getPassthroughBlocks() {
        return passthroughBlocks;
    }

    @NotNull
    public Set<Material> getForbiddenHoverOverBlocks() {
        return forbiddenHoverOverBlocks;
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