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
import net.countercraft.movecraft.craft.type.property.PerWorldProperty;
import net.countercraft.movecraft.craft.type.property.Property;
import net.countercraft.movecraft.craft.type.property.StringProperty;
import net.countercraft.movecraft.craft.type.transform.BooleanTransform;
import net.countercraft.movecraft.craft.type.transform.DoubleTransform;
import net.countercraft.movecraft.craft.type.transform.FloatTransform;
import net.countercraft.movecraft.craft.type.transform.IntegerTransform;
import net.countercraft.movecraft.craft.type.transform.MaterialSetTransform;
import net.countercraft.movecraft.craft.type.transform.ObjectTransform;
import net.countercraft.movecraft.craft.type.transform.PerWorldTransform;
import net.countercraft.movecraft.craft.type.transform.StringTransform;
import net.countercraft.movecraft.craft.type.transform.Transform;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final public class CraftType {
    public static final NamespacedKey NAME = buildKey("name");
    public static final NamespacedKey MAX_SIZE = buildKey("max_size");
    public static final NamespacedKey MIN_SIZE = buildKey("min_size");
    public static final NamespacedKey ALLOWED_BLOCKS = buildKey("allowed_blocks");
    private static final NamespacedKey SPEED = buildKey("speed"); // Private key used to calculate TICK_COOLDOWN
    public static final NamespacedKey TICK_COOLDOWN = buildKey("tick_cooldown");
    private static final NamespacedKey PER_WORLD_SPEED = buildKey("per_world_speed"); // Private key used to calculate PER_WORLD_TICK_COOLDOWN
    public static final NamespacedKey PER_WORLD_TICK_COOLDOWN = buildKey("per_world_tick_cooldown");
    public static final NamespacedKey FORBIDDEN_BLOCKS = buildKey("forbidden_blocks");
    public static final NamespacedKey BLOCKED_BY_WATER = buildKey("blocked_by_water");
    private static final NamespacedKey CAN_FLY = buildKey("can_fly"); // Private key used to calculate BLOCKED_BY_WATER
    public static final NamespacedKey REQUIRE_WATER_CONTACT = buildKey("require_water_contact");
    public static final NamespacedKey TRY_NUDGE = buildKey("tryNudge");
    public static final NamespacedKey CAN_CRUISE = buildKey("can_cruise");
    public static final NamespacedKey CAN_TELEPORT = buildKey("can_teleport");
    public static final NamespacedKey CAN_SWITCH_WORLD = buildKey("can_switch_world");
    public static final NamespacedKey CAN_BE_NAMED = buildKey("can_be_named");
    public static final NamespacedKey CRUISE_ON_PILOT = buildKey("cruise_on_pilot");
    public static final NamespacedKey CRUISE_ON_PILOT_VERT_MOVE = buildKey("cruise_on_pilot_vert_move");
    public static final NamespacedKey ALLOW_VERTICAL_MOVEMENT = buildKey("allow_vertical_movement");
    public static final NamespacedKey ROTATE_AT_MIDPOINT = buildKey("rotate_at_midpoint");
    public static final NamespacedKey ALLOW_HORIZONTAL_MOVEMENT = buildKey("allow_horizontal_movement");
    public static final NamespacedKey ALLOW_REMOTE_SIGN = buildKey("allow_remote_sign");
    public static final NamespacedKey CAN_STATIC_MOVE = buildKey("can_static_move");
    public static final NamespacedKey MAX_STATIC_MOVE = buildKey("max_static_move");
    public static final NamespacedKey CRUISE_SKIP_BLOCKS = buildKey("cruise_skip_blocks");
    public static final NamespacedKey PER_WORLD_CRUISE_SKIP_BLOCKS = buildKey("per_world_cruise_skip_blocks");
    public static final NamespacedKey VERT_CRUISE_SKIP_BLOCKS = buildKey("vert_cruise_skip_blocks");
    public static final NamespacedKey HALF_SPEED_UNDERWATER = buildKey("half_speed_underwater");
    public static final NamespacedKey FOCUSED_EXPLOSION = buildKey("focused_explosion");
    public static final NamespacedKey MUST_BE_SUBCRAFT = buildKey("must_be_subcraft");
    public static final NamespacedKey STATIC_WATER_LEVEL = buildKey("static_water_level");
    public static final NamespacedKey FUEL_BURN_RATE = buildKey("fuel_burn_rate");
    public static final NamespacedKey SINK_PERCENT = buildKey("sink_percent");
    public static final NamespacedKey OVERALL_SINK_PERCENT = buildKey("overall_sink_percent");
    public static final NamespacedKey DETECTION_MULTIPLIER = buildKey("detection_multiplier");
    public static final NamespacedKey UNDERWATER_DETECTION_MULTIPLIER = buildKey("underwater_detection_multiplier");
    private static final NamespacedKey SINK_SPEED = buildKey("sink_speed"); // Private key used to calculate SINK_RATE_TICKS
    public static final NamespacedKey SINK_RATE_TICKS = buildKey("sink_rate_ticks");
    public static final NamespacedKey KEEP_MOVING_ON_SINK = buildKey("keep_moving_on_sink");
    public static final NamespacedKey SMOKE_ON_SINK = buildKey("smoke_on_sink");
    public static final NamespacedKey EXPLODE_ON_CRASH = buildKey("explode_on_crash");
    public static final NamespacedKey COLLISION_EXPLOSION = buildKey("collision_explosion");
    public static final NamespacedKey MIN_HEIGHT_LIMIT = buildKey("min_height_limit");
    private static final NamespacedKey CRUISE_SPEED = buildKey("cruise_speed"); // Private key used to calculate CRUISE_TICK_COOLDOWN
    public static final NamespacedKey CRUISE_TICK_COOLDOWN = buildKey("cruise_tick_cooldown");
    private static final NamespacedKey VERT_CRUISE_SPEED = buildKey("vert_cruise_speed"); // Private key used to calculate VERT_CRUISE_TICK_COOLDOWN
    public static final NamespacedKey VERT_CRUISE_TICK_COOLDOWN = buildKey("vert_cruise_tick_cooldown");
    public static final NamespacedKey MAX_HEIGHT_LIMIT = buildKey("max_height_limit");
    public static final NamespacedKey MAX_HEIGHT_ABOVE_GROUND = buildKey("max_height_above_ground");
    public static final NamespacedKey CAN_DIRECT_CONTROL = buildKey("can_direct_control");
    public static final NamespacedKey CAN_HOVER = buildKey("can_hover");
    public static final NamespacedKey CAN_HOVER_OVER_WATER = buildKey("can_hover_over_water");
    public static final NamespacedKey MOVE_ENTITIES = buildKey("move_entities");
    public static final NamespacedKey ONLY_MOVE_PLAYERS = buildKey("only_move_players");
    public static final NamespacedKey USE_GRAVITY = buildKey("use_gravity");
    public static final NamespacedKey HOVER_LIMIT = buildKey("hover_limit");
    public static final NamespacedKey HARVEST_BLOCKS = buildKey("harvest_blocks");
    public static final NamespacedKey HARVESTER_BLADE_BLOCKS = buildKey("harvester_blade_blocks");
    public static final NamespacedKey PASSTHROUGH_BLOCKS = buildKey("passthrough_blocks");
    public static final NamespacedKey FORBIDDEN_HOVER_OVER_BLOCKS = buildKey("forbidden_hover_over_blocks");
    public static final NamespacedKey ALLOW_VERTICAL_TAKEOFF_AND_LANDING = buildKey("allow_vertical_takeoff_and_landing");
    public static final NamespacedKey DYNAMIC_LAG_SPEED_FACTOR = buildKey("dynamic_lag_speed_factor");
    public static final NamespacedKey DYNAMIC_LAG_POWER_FACTOR = buildKey("dynamic_lag_power_factor");
    public static final NamespacedKey DYNAMIC_LAG_MIN_SPEED = buildKey("dynamic_lag_min_speed");
    public static final NamespacedKey DYNAMIC_FLY_BLOCK_SPEED_FACTOR = buildKey("dynamic_fly_block_speed_factor");
    public static final NamespacedKey DYNAMIC_FLY_BLOCK = buildKey("dynamic_fly_block");
    public static final NamespacedKey CHEST_PENALTY = buildKey("chest_penalty");
    public static final NamespacedKey GRAVITY_INCLINE_DISTANCE = buildKey("gravity_incline_distance");
    public static final NamespacedKey GRAVITY_DROP_DISTANCE = buildKey("gravity_drop_distance");
    public static final NamespacedKey TELEPORTATION_COOLDOWN = buildKey("teleportation_cooldown");
    public static final NamespacedKey GEAR_SHIFTS = buildKey("gear_shifts");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_TICK_COOLDOWN = buildKey("gear_shifts_affect_tick_cooldown");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT = buildKey("gear_shifts_affect_direct_movement");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS = buildKey("gear_shifts_affect_cruise_skip_blocks");
    public static final NamespacedKey RELEASE_TIMEOUT = buildKey("release_timeout");

    @Contract("_ -> new")
    private static @NotNull NamespacedKey buildKey(String key) {
        return new NamespacedKey("movecraft", key);
    }



    static final List<Property<?>> properties = new ArrayList<>();

    /**
     * Register a property with Movecraft
     *
     * @param property property to register
     */
    public static void registerProperty(Property<?> property) {
        properties.add(property);
    }



    private Map<NamespacedKey, String> stringPropertyMap;
    /**
     * Get a string property of this CraftType
     *
     * @param key Key of the string property
     * @return value of the string property
     */
    public String getStringProperty(NamespacedKey key) {
        if(!stringPropertyMap.containsKey(key))
            throw new IllegalStateException("String property " + key + " not found.");
        return stringPropertyMap.get(key);
    }

    private Map<NamespacedKey, Integer> intPropertyMap;
    /**
     * Get an integer property of this CraftType
     *
     * @param key Key of the integer property
     * @return value of the integer property
     */
    public int getIntProperty(NamespacedKey key) {
        if(!intPropertyMap.containsKey(key))
            throw new IllegalStateException("Int property " + key + " not found.");
        return intPropertyMap.get(key);
    }

    private Map<NamespacedKey, Boolean> boolPropertyMap;
    /**
     * Get a boolean property of this CraftType
     *
     * @param key Key of the boolean property
     * @return value of the boolean property
     */
    public boolean getBoolProperty(NamespacedKey key) {
        if(!boolPropertyMap.containsKey(key))
            throw new IllegalStateException("Bool property " + key + " not found.");
        return boolPropertyMap.get(key);
    }

    private Map<NamespacedKey, Float> floatPropertyMap;
    /**
     * Get a float property of this CraftType
     *
     * @param key Key of the float property
     * @return value of the float property
     */
    public float getFloatProperty(NamespacedKey key) {
        if(!floatPropertyMap.containsKey(key))
            throw new IllegalStateException("Float property " + key + " not found.");
        return floatPropertyMap.get(key);
    }

    private Map<NamespacedKey, Double> doublePropertyMap;
    /**
     * Get a double property of this CraftType
     *
     * @param key Key of the double property
     * @return value of the double property
     */
    public double getDoubleProperty(NamespacedKey key) {
        if(!doublePropertyMap.containsKey(key))
            throw new IllegalStateException("Double property " + key + " not found.");
        return doublePropertyMap.get(key);
    }

    private Map<NamespacedKey, Object> objectPropertyMap;
    /**
     * Get an object property of this CraftType
     * Note: Object properties have no type safety, it is expected that the addon developer handle type safety
     *
     * @param key Key of the object property
     * @return value of the object property
     */
    @Nullable
    public Object getObjectProperty(NamespacedKey key) {
        if(!objectPropertyMap.containsKey(key))
            throw new IllegalStateException("Object property " + key + " not found.");
        return objectPropertyMap.get(key);
    }

    private Map<NamespacedKey, EnumSet<Material>> materialSetPropertyMap;
    /**
     * Get a material set property of this CraftType
     *
     * @param key Key of the string property
     * @return value of the string property
     */
    public EnumSet<Material> getMaterialSetProperty(NamespacedKey key) {
        if(!materialSetPropertyMap.containsKey(key))
            throw new IllegalStateException("Materials property " + key + " not found.");
        return materialSetPropertyMap.get(key);
    }

    private Map<NamespacedKey, Pair<Map<String, Object>, Function<CraftType, Object>>> perWorldPropertyMap = new HashMap<>();
    private Object getPerWorldProperty(NamespacedKey key, String worldName) {
        if(!perWorldPropertyMap.containsKey(key))
            throw new IllegalStateException("Per world property " + key + " not found.");
        var pair =  perWorldPropertyMap.get(key);
        var map = pair.getLeft();
        var defaultProvider = pair.getRight();

        if(!map.containsKey(worldName))
            return defaultProvider.apply(this);

        return map.get(worldName);
    }
    /**
     * Get a per world property of this CraftType
     *
     * @param key Key of the string property
     * @param world the world to check
     * @return value of the string property
     */
    public Object getPerWorldProperty(NamespacedKey key, @NotNull World world) {
        return getPerWorldProperty(key, world.getName());
    }
    /**
     * Get a per world property of this CraftType
     *
     * @param key Key of the string property
     * @param world the world to check
     * @return value of the string property
     */
    public Object getPerWorldProperty(NamespacedKey key, @NotNull MovecraftWorld world) {
        return getPerWorldProperty(key, world.getName());
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
        registerProperty(new StringProperty("name", NAME));
        registerProperty(new IntegerProperty("maxSize", MAX_SIZE));
        registerProperty(new IntegerProperty("minSize", MIN_SIZE));
        registerProperty(new MaterialSetProperty("allowedBlocks", ALLOWED_BLOCKS));
        registerProperty(new DoubleProperty("speed", SPEED));

        // Optional properties
        // TODO: forbiddenSignStrings
        registerProperty(new PerWorldProperty<Double>("perWorldSpeed", PER_WORLD_SPEED, type -> Collections.emptyMap()));
        // TODO: flyBlocks
        registerProperty(new MaterialSetProperty("forbiddenBlocks", FORBIDDEN_BLOCKS, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("blockedByWater", BLOCKED_BY_WATER, type -> true));
        registerProperty(new BooleanProperty("canFly", CAN_FLY, type -> type.getBoolProperty(BLOCKED_BY_WATER)));
        registerProperty(new BooleanProperty("requireWaterContact", REQUIRE_WATER_CONTACT, type -> false));
        registerProperty(new BooleanProperty("tryNudge", TRY_NUDGE, type -> false));
        // TODO: moveblocks
        registerProperty(new BooleanProperty("canCruise", CAN_CRUISE, type -> false));
        registerProperty(new BooleanProperty("canTeleport", CAN_TELEPORT, type -> false));
        registerProperty(new BooleanProperty("canSwitchWorld", CAN_SWITCH_WORLD, type -> false));
        registerProperty(new BooleanProperty("canBeNamed", CAN_BE_NAMED, type -> true));
        registerProperty(new BooleanProperty("cruiseOnPilot", CRUISE_ON_PILOT, type -> false));
        registerProperty(new IntegerProperty("cruiseOnPilotVertMove", CRUISE_ON_PILOT_VERT_MOVE, type -> 0));
        registerProperty(new BooleanProperty("allowVerticalMovement", ALLOW_VERTICAL_MOVEMENT, type -> true));
        registerProperty(new BooleanProperty("rotateAtMidpoint", ROTATE_AT_MIDPOINT, type -> false));
        registerProperty(new BooleanProperty("allowHorizontalMovement", ALLOW_HORIZONTAL_MOVEMENT, type -> true));
        registerProperty(new BooleanProperty("allowRemoteSign", ALLOW_REMOTE_SIGN, type -> true));
        registerProperty(new BooleanProperty("canStaticMove", CAN_STATIC_MOVE, type -> false));
        registerProperty(new IntegerProperty("maxStaticMove", MAX_STATIC_MOVE, type -> 10000));
        registerProperty(new IntegerProperty("cruiseSkipBlocks", CRUISE_SKIP_BLOCKS, type -> 0));
        registerProperty(new PerWorldProperty<IntegerProperty>("perWorldCruiseSkipBlocks", PER_WORLD_CRUISE_SKIP_BLOCKS, type -> Collections.emptyMap()));
        // TODO: perWorldCruiseSkipBlocks
        registerProperty(new IntegerProperty("vertCruiseSkipBlocks", VERT_CRUISE_SKIP_BLOCKS, type -> type.getIntProperty(CRUISE_SKIP_BLOCKS)));
        // TODO: perWorldVertCruiseSkipBlocks
        registerProperty(new BooleanProperty("halfSpeedUnderwater", HALF_SPEED_UNDERWATER, type -> false));
        registerProperty(new BooleanProperty("focusedExplosion", FOCUSED_EXPLOSION, type -> false));
        registerProperty(new BooleanProperty("mustBeSubcraft", MUST_BE_SUBCRAFT, type -> false));
        registerProperty(new IntegerProperty("staticWaterLevel", STATIC_WATER_LEVEL, type -> 0));
        registerProperty(new DoubleProperty("fuelBurnRate", FUEL_BURN_RATE, type -> 0D));
        // TODO: perWorldFuelBurnRate
        registerProperty(new DoubleProperty("sinkPercent", SINK_PERCENT, type -> 0D));
        registerProperty(new DoubleProperty("overallSinkPercent", OVERALL_SINK_PERCENT, type -> 0D));
        registerProperty(new DoubleProperty("detectionMultiplier", DETECTION_MULTIPLIER, type -> 0D));
        // TODO: perWorldDetectionMultiplier
        registerProperty(new DoubleProperty("underwaterDetectionMultiplier", UNDERWATER_DETECTION_MULTIPLIER, type-> type.getDoubleProperty(DETECTION_MULTIPLIER)));
        registerProperty(new DoubleProperty("sinkSpeed", SINK_SPEED, type -> 1D));
        registerProperty(new IntegerProperty("sinkRateTicks", SINK_RATE_TICKS, type -> (int) Math.ceil(20 / type.getDoubleProperty(SINK_SPEED))));
        registerProperty(new BooleanProperty("keepMovingOnSink", KEEP_MOVING_ON_SINK, type -> false));
        registerProperty(new IntegerProperty("smokeOnSink", SMOKE_ON_SINK, type -> 0));
        registerProperty(new FloatProperty("explodeOnCrash", EXPLODE_ON_CRASH, type -> 0F));
        registerProperty(new FloatProperty("collisionExplosion", COLLISION_EXPLOSION, type -> 0F));
        registerProperty(new IntegerProperty("minHeightLimit", MIN_HEIGHT_LIMIT, type -> 0));
        // TODO: perWorldMinHeightLimit
        registerProperty(new DoubleProperty("cruiseSpeed", CRUISE_SPEED, type -> 20.0 / type.getIntProperty(TICK_COOLDOWN)));
        // TODO: perWorldCruiseSpeed -> perWorldCruiseTickCooldown
        registerProperty(new DoubleProperty("vertCruiseSpeed", VERT_CRUISE_SPEED, type -> type.getDoubleProperty(CRUISE_SPEED)));
        // TODO: perWorldVertCruiseSpeed -> perWorldVertCruiseTickCooldown
        registerProperty(new IntegerProperty("maxHeightLimit", MAX_HEIGHT_LIMIT, type -> 255));
        // TODO: perWorldMaxHeightLimit
        registerProperty(new IntegerProperty("maxHeightAboveGround", MAX_HEIGHT_ABOVE_GROUND, type -> -1));
        // TODO: perWorldMaxHeightAboveGround
        registerProperty(new BooleanProperty("canDirectControl", CAN_DIRECT_CONTROL, type -> true));
        registerProperty(new BooleanProperty("canHover", CAN_HOVER, type -> false));
        registerProperty(new BooleanProperty("canHoverOverWater", CAN_HOVER_OVER_WATER, type -> true));
        registerProperty(new BooleanProperty("moveEntities", MOVE_ENTITIES, type -> true));
        registerProperty(new BooleanProperty("onlyMovePlayers", ONLY_MOVE_PLAYERS, type -> true));
        registerProperty(new BooleanProperty("useGravity", USE_GRAVITY, type -> false));
        registerProperty(new IntegerProperty("hoverLimit", HOVER_LIMIT, type -> 0));
        registerProperty(new MaterialSetProperty("harvestBlocks", HARVEST_BLOCKS, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("harvesterBladeBlocks", HARVESTER_BLADE_BLOCKS, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("passthroughBlocks", PASSTHROUGH_BLOCKS, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("forbiddenHoverOverBlocks", FORBIDDEN_HOVER_OVER_BLOCKS, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("allowVerticalTakeoffAndLanding", ALLOW_VERTICAL_TAKEOFF_AND_LANDING, type -> true));
        registerProperty(new DoubleProperty("dynamicLagSpeedFactor", DYNAMIC_LAG_SPEED_FACTOR, type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagPowerFactor", DYNAMIC_LAG_POWER_FACTOR, type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagMinSpeed", DYNAMIC_LAG_MIN_SPEED, type -> 0D));
        registerProperty(new DoubleProperty("dynamicFlyBlockSpeedFactor", DYNAMIC_FLY_BLOCK_SPEED_FACTOR, type -> 0D));
        registerProperty(new MaterialSetProperty("dynamicFlyBlock", DYNAMIC_FLY_BLOCK, type -> EnumSet.noneOf(Material.class)));
        registerProperty(new DoubleProperty("chestPenalty", CHEST_PENALTY, type -> 0D));
        registerProperty(new IntegerProperty("gravityInclineDistance", GRAVITY_INCLINE_DISTANCE, type -> -1));
        registerProperty(new IntegerProperty("gravityDropDistance", GRAVITY_DROP_DISTANCE, type -> -8));
        // TODO: collisionSound
        // TODO: fuelTypes
        // TODO: disableTeleportToWorlds
        registerProperty(new IntegerProperty("teleportationCooldown", TELEPORTATION_COOLDOWN, type -> 0));
        registerProperty(new IntegerProperty("gearShifts", GEAR_SHIFTS, type -> 1));
        registerProperty(new BooleanProperty("gearShiftsAffectTickCooldown", GEAR_SHIFTS_AFFECT_TICK_COOLDOWN, type -> true));
        registerProperty(new BooleanProperty("gearShiftsAffectDirectMovement", GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT, type -> false));
        registerProperty(new BooleanProperty("gearShiftsAffectCruiseSkipBlocks", GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS, type -> false));
        registerProperty(new IntegerProperty("releaseTimeout", RELEASE_TIMEOUT, type -> 30));

        // Craft type transforms
        registerTypeTransform((IntegerTransform) (data, type) -> {
            int tickCooldown = (int) Math.ceil(20 / type.getDoubleProperty(SPEED));
            data.put(TICK_COOLDOWN, tickCooldown);
            return data;
        });
        registerTypeTransform((DoubleTransform) (data, type) -> {
            data.remove(SPEED);
            return data;
        });
        registerTypeTransform((BooleanTransform) (data, type) -> {
            data.put(BLOCKED_BY_WATER, data.get(CAN_FLY));
            data.remove(CAN_FLY);
            return data;
        });
        registerTypeTransform((DoubleTransform) (data, type) -> {
            data.remove(SINK_SPEED);
            return data;
        });
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put(CRUISE_TICK_COOLDOWN, (int) Math.round((1.0 + data.get(CRUISE_SKIP_BLOCKS)) * 20.0 / type.getDoubleProperty(CRUISE_SPEED)));
            return data;
        });
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put(VERT_CRUISE_TICK_COOLDOWN, (int) Math.round((1.0 + data.get(VERT_CRUISE_SKIP_BLOCKS)) * 20.0 / type.getDoubleProperty(VERT_CRUISE_SPEED)));
            return data;
        });
        registerTypeTransform((IntegerTransform) (data, type) -> {
            int dropDist = data.get(GRAVITY_DROP_DISTANCE);
            data.put(GRAVITY_DROP_DISTANCE, dropDist > 0 ? -dropDist : dropDist);
            return data;
        });
        registerTypeTransform((MaterialSetTransform) (data, type) -> {
            if(type.getBoolProperty(BLOCKED_BY_WATER))
                return data;

            var passthroughBlocks = data.get(PASSTHROUGH_BLOCKS);
            passthroughBlocks.add(Material.WATER);
            data.put(PASSTHROUGH_BLOCKS, passthroughBlocks);
            return data;
        });
        registerTypeTransform((MaterialSetTransform) (data, type) -> {
            if(type.getBoolProperty(CAN_HOVER_OVER_WATER))
                return data;

            var forbiddenHoverOverBlocks = data.get(FORBIDDEN_HOVER_OVER_BLOCKS);
            forbiddenHoverOverBlocks.add(Material.WATER);
            data.put(FORBIDDEN_HOVER_OVER_BLOCKS, forbiddenHoverOverBlocks);
            return data;
        });
        registerTypeTransform((PerWorldTransform) (data, type) -> {
            var map = data.get(PER_WORLD_SPEED).getLeft();
            Map<String, Object> resultMap = new HashMap<>();
            for(var i : map.entrySet()) {
                var value = i.getValue();
                if(!(value instanceof Double))
                    throw new IllegalStateException("PER_WORLD_SPEED must be of type Double");
                resultMap.put(i.getKey(), (int) Math.ceil(20 / (double) value));
            }
            var defaultProvider = (Function<CraftType, Object>) craftType -> craftType.getIntProperty(TICK_COOLDOWN);
            data.put(PER_WORLD_TICK_COOLDOWN, new Pair<>(resultMap, defaultProvider));
            return data;
        });
        registerTypeTransform((PerWorldTransform) (data, type) -> {
            data.remove(PER_WORLD_SPEED);
            return data;
        });
        // TODO: remove cruiseSpeed, vertCruiseSpeed, perWorldSpeed

        // Craft type validators
        registerTypeValidator(
                type -> type.getIntProperty(MIN_HEIGHT_LIMIT) <= type.getIntProperty(MAX_HEIGHT_LIMIT),
                "minHeightLimit must be less than or equal to maxHeightLimit"
        );
        registerTypeValidator(
                type -> type.getIntProperty(MIN_HEIGHT_LIMIT) >= 0 && type.getIntProperty(MIN_HEIGHT_LIMIT) <= 255,
                "minHeightLimit must be between 0 and 255"
        );
        registerTypeValidator(
                type -> type.getIntProperty(MAX_HEIGHT_LIMIT) >= 0 && type.getIntProperty(MAX_HEIGHT_LIMIT) <= 255,
                "maxHeightLimit must be between 0 and 255"
        );
        registerTypeValidator(
                type -> type.getIntProperty(HOVER_LIMIT) <= 0,
                "hoverLimit must be greater than or equal to zero"
        );
        registerTypeValidator(
                type -> type.getIntProperty(GEAR_SHIFTS) <= 1,
                "gearShifts must be greater than or equal to one"
        );
    }

    @NotNull private final Map<String, Integer> perWorldMinHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightLimit;
    @NotNull private final Map<String, Integer> perWorldMaxHeightAboveGround;
    @NotNull private final Map<String, Integer> perWorldVertCruiseSkipBlocks;
    @NotNull private final Map<String, Integer> perWorldCruiseTickCooldown; // cruise speed setting
    @NotNull private final Map<String, Integer> perWorldVertCruiseTickCooldown; // cruise speed setting
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
                    stringPropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof IntegerProperty) {
                Integer value = ((IntegerProperty) i).load(data, this);
                if (value != null)
                    intPropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof BooleanProperty) {
                Boolean value = ((BooleanProperty) i).load(data, this);
                if(value != null)
                    boolPropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof FloatProperty) {
                Float value = ((FloatProperty) i).load(data, this);
                if(value != null)
                    floatPropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof DoubleProperty) {
                Double value = ((DoubleProperty) i).load(data, this);
                if(value != null)
                    doublePropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof ObjectProperty) {
                Object value = ((ObjectProperty) i).load(data, this);
                if(value != null)
                    objectPropertyMap.put(i.getNamespacedKey(), value);
            }
            else if(i instanceof MaterialSetProperty) {
                EnumSet<Material> value = ((MaterialSetProperty) i).load(data, this);
                if(value != null)
                    materialSetPropertyMap.put(i.getNamespacedKey(), value);
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
        flyBlocks = blockIDMapListFromObject("flyblocks", data.getDataOrEmpty("flyblocks").getBackingData());

        // Optional craft flags
        moveBlocks = blockIDMapListFromObject("moveblocks", data.getDataOrEmpty("moveblocks").getBackingData());
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
            var perWorldCruiseSkipBlocks = perWorldPropertyMap.get(PER_WORLD_CRUISE_SKIP_BLOCKS).getLeft();
            double worldCruiseSkipBlocks = (double) perWorldCruiseSkipBlocks.getOrDefault(world, getIntProperty(CRUISE_SKIP_BLOCKS));
            perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldCruiseSkipBlocks) * 20.0 / getDoubleProperty(CRUISE_SPEED)));
        });
        var tickCooldownMap = perWorldPropertyMap.get(PER_WORLD_TICK_COOLDOWN).getLeft();
        tickCooldownMap.forEach((world, speed) -> {
            if (!perWorldCruiseTickCooldown.containsKey(world)) {
                if(!(speed instanceof Integer))
                    throw new IllegalStateException("PER_WORLD_TICK_COOLDOWN must be of type int");
                perWorldCruiseTickCooldown.put(world, (int) Math.round((1.0 + getIntProperty(CRUISE_SKIP_BLOCKS)) * 20.0 / (int) speed));
            }
        });

        perWorldVertCruiseTickCooldown = new HashMap<>();
        Map<String, Double> vertCruiseTickCooldownMap = stringToDoubleMapFromObject(data.getDataOrEmpty("perWorldVertCruiseSpeed").getBackingData());
        vertCruiseTickCooldownMap.forEach((world, speed) -> {
            double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty(VERT_CRUISE_SKIP_BLOCKS));
            perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / speed));
        });
        cruiseTickCooldownMap.forEach((world, speed) -> {
            if (!perWorldVertCruiseTickCooldown.containsKey(world)) {
                double worldVertCruiseSkipBlocks = perWorldVertCruiseSkipBlocks.getOrDefault(world, getIntProperty(VERT_CRUISE_SKIP_BLOCKS));
                perWorldVertCruiseTickCooldown.put(world, (int) Math.round((1.0 + worldVertCruiseSkipBlocks) * 20.0 / getDoubleProperty(VERT_CRUISE_SPEED)));
            }
        });

        perWorldMaxHeightLimit = new HashMap<>();
        Map<String, Integer> maxHeightMap = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMaxHeightLimit").getBackingData());
        maxHeightMap.forEach((world, height) -> {
            int worldValue = Math.min(height, 255);
            int worldMinHeight = perWorldMinHeightLimit.getOrDefault(world, getIntProperty(MIN_HEIGHT_LIMIT));
            if (worldValue <= worldMinHeight) worldValue = 255;
            perWorldMaxHeightLimit.put(world, worldValue);
        });
        
        perWorldMaxHeightAboveGround = stringToIntMapFromObject(data.getDataOrEmpty("perWorldMaxHeightAboveGround").getBackingData());
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
        return (int) getPerWorldProperty(PER_WORLD_CRUISE_SKIP_BLOCKS, world);
    }

    public int getVertCruiseSkipBlocks(@NotNull World world) {
        return perWorldVertCruiseSkipBlocks.getOrDefault(world.getName(), getIntProperty(VERT_CRUISE_SKIP_BLOCKS));
    }

    public double getFuelBurnRate(@NotNull World world) {
        return perWorldFuelBurnRate.getOrDefault(world.getName(), getDoubleProperty(FUEL_BURN_RATE));
    }

    public double getDetectionMultiplier(@NotNull World world) {
        return perWorldDetectionMultiplier.getOrDefault(world.getName(), getDoubleProperty(DETECTION_MULTIPLIER));
    }

    public double getUnderwaterDetectionMultiplier(@NotNull World world) {
        return perWorldUnderwaterDetectionMultiplier.getOrDefault(world.getName(), getDoubleProperty(UNDERWATER_DETECTION_MULTIPLIER));
    }

    public int getTickCooldown(@NotNull World world) {
        return (int) getPerWorldProperty(PER_WORLD_TICK_COOLDOWN, world);
    }

    public int getCruiseTickCooldown(@NotNull World world) {
        return perWorldCruiseTickCooldown.getOrDefault(world.getName(), getIntProperty(CRUISE_TICK_COOLDOWN));
    }

    public int getVertCruiseTickCooldown(@NotNull World world) {
        return perWorldVertCruiseTickCooldown.getOrDefault(world.getName(), getIntProperty(VERT_CRUISE_TICK_COOLDOWN));
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
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), getIntProperty(MAX_HEIGHT_LIMIT));
    }

    public int getMaxHeightLimit(@NotNull MovecraftWorld world) {
        return perWorldMaxHeightLimit.getOrDefault(world.getName(), getIntProperty(MAX_HEIGHT_LIMIT));
    }

    public int getMinHeightLimit(@NotNull World world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), getIntProperty(MIN_HEIGHT_LIMIT));
    }

    public int getMinHeightLimit(@NotNull MovecraftWorld world) {
        return perWorldMinHeightLimit.getOrDefault(world.getName(), getIntProperty(MIN_HEIGHT_LIMIT));
    }

    public int getMaxHeightAboveGround(@NotNull World world) {
        return perWorldMaxHeightAboveGround.getOrDefault(world.getName(), getIntProperty(MAX_HEIGHT_ABOVE_GROUND));
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