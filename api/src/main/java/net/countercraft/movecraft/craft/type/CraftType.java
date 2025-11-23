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
import net.countercraft.movecraft.craft.type.property.ObjectPropertyImpl;
import net.countercraft.movecraft.craft.type.property.PerWorldProperty;
import net.countercraft.movecraft.craft.type.property.Property;
import net.countercraft.movecraft.craft.type.property.RequiredBlockProperty;
import net.countercraft.movecraft.craft.type.property.StringProperty;
import net.countercraft.movecraft.craft.type.transform.BooleanTransform;
import net.countercraft.movecraft.craft.type.transform.DoubleTransform;
import net.countercraft.movecraft.craft.type.transform.FloatTransform;
import net.countercraft.movecraft.craft.type.transform.IntegerTransform;
import net.countercraft.movecraft.craft.type.transform.MaterialSetTransform;
import net.countercraft.movecraft.craft.type.transform.ObjectTransform;
import net.countercraft.movecraft.craft.type.transform.PerWorldTransform;
import net.countercraft.movecraft.craft.type.transform.RequiredBlockTransform;
import net.countercraft.movecraft.craft.type.transform.StringTransform;
import net.countercraft.movecraft.craft.type.transform.Transform;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.jetbrains.annotations.Contract;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final public class CraftType {
    //region Property Keys
    public static final NamespacedKey NAME = buildKey("name");
    public static final NamespacedKey MAX_SIZE = buildKey("max_size");
    public static final NamespacedKey MIN_SIZE = buildKey("min_size");
    public static final NamespacedKey ALLOWED_BLOCKS = buildKey("allowed_blocks");
    private static final NamespacedKey SPEED = buildKey("speed");
        // Private key used to calculate TICK_COOLDOWN
    private static final NamespacedKey TICK_COOLDOWN = buildKey("tick_cooldown");
        // Private key used as default for PER_WORLD_TICK_COOLDOWN
    public static final NamespacedKey FLY_BLOCKS = buildKey("fly_blocks");
    public static final NamespacedKey DETECTION_BLOCKS = buildKey("detection_blocks");
    public static final NamespacedKey FORBIDDEN_SIGN_STRINGS = buildKey("forbidden_sign_strings");
    private static final NamespacedKey PER_WORLD_SPEED = buildKey("per_world_speed");
        // Private key used to calculate PER_WORLD_TICK_COOLDOWN
    public static final NamespacedKey PER_WORLD_TICK_COOLDOWN = buildKey("per_world_tick_cooldown");
    public static final NamespacedKey FORBIDDEN_BLOCKS = buildKey("forbidden_blocks");
    public static final NamespacedKey BLOCKED_BY_WATER = buildKey("blocked_by_water");
    private static final NamespacedKey CAN_FLY = buildKey("can_fly");
        // Private key used to calculate BLOCKED_BY_WATER
    public static final NamespacedKey REQUIRE_WATER_CONTACT = buildKey("require_water_contact");
    public static final NamespacedKey TRY_NUDGE = buildKey("try_nudge");
    public static final NamespacedKey MOVE_BLOCKS = buildKey("move_blocks");
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
    private static final NamespacedKey CRUISE_SKIP_BLOCKS = buildKey("cruise_skip_blocks");
        // Private key used as default for PER_WORLD_CRUISE_SKIP_BLOCKS
    public static final NamespacedKey PER_WORLD_CRUISE_SKIP_BLOCKS = buildKey("per_world_cruise_skip_blocks");
    private static final NamespacedKey VERT_CRUISE_SKIP_BLOCKS = buildKey("vert_cruise_skip_blocks");
        // Private key used as default for PER_WORLD_VERT_CRUISE_SKIP_BLOCKS
    public static final NamespacedKey PER_WORLD_VERT_CRUISE_SKIP_BLOCKS = buildKey("per_world_vert_cruise_skip_blocks");
    public static final NamespacedKey HALF_SPEED_UNDERWATER = buildKey("half_speed_underwater");
    public static final NamespacedKey FOCUSED_EXPLOSION = buildKey("focused_explosion");
    public static final NamespacedKey MUST_BE_SUBCRAFT = buildKey("must_be_subcraft");
    public static final NamespacedKey STATIC_WATER_LEVEL = buildKey("static_water_level");
    private static final NamespacedKey FUEL_BURN_RATE = buildKey("fuel_burn_rate");
        // Private key used as default for PER_WORLD_FUEL_BURN_RATE
    public static final NamespacedKey PER_WORLD_FUEL_BURN_RATE = buildKey("per_world_fuel_burn_rate");
    public static final NamespacedKey SINK_PERCENT = buildKey("sink_percent");
    public static final NamespacedKey OVERALL_SINK_PERCENT = buildKey("overall_sink_percent");
    private static final NamespacedKey DETECTION_MULTIPLIER = buildKey("detection_multiplier");
        // Private key used as default for PER_WORLD_DETECTION_MULTIPLIER
    public static final NamespacedKey PER_WORLD_DETECTION_MULTIPLIER = buildKey("per_world_detection_multiplier");
    private static final NamespacedKey UNDERWATER_DETECTION_MULTIPLIER = buildKey("underwater_detection_multiplier");
        // Private key used as default for PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER
    public static final NamespacedKey PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER = buildKey(
            "per_world_underwater_detection_multiplier");
    private static final NamespacedKey SINK_SPEED = buildKey("sink_speed");
        // Private key used to calculate SINK_RATE_TICKS
    public static final NamespacedKey SINK_RATE_TICKS = buildKey("sink_rate_ticks");
    public static final NamespacedKey KEEP_MOVING_ON_SINK = buildKey("keep_moving_on_sink");
    public static final NamespacedKey SMOKE_ON_SINK = buildKey("smoke_on_sink");
    public static final NamespacedKey EXPLODE_ON_CRASH = buildKey("explode_on_crash");
    public static final NamespacedKey INCENDIARY_ON_CRASH = buildKey("incendiary_on_crash");
    public static final NamespacedKey COLLISION_EXPLOSION = buildKey("collision_explosion");
    public static final NamespacedKey UNDERWATER_COLLISION_EXPLOSION = buildKey("underwater_collision_explosion");
    private static final NamespacedKey MIN_HEIGHT_LIMIT = buildKey("min_height_limit");
        // Private key used as default for PER_WORLD_MIN_HEIGHT_LIMIT
    public static final NamespacedKey PER_WORLD_MIN_HEIGHT_LIMIT = buildKey("per_world_min_height_limit");
    private static final NamespacedKey CRUISE_SPEED = buildKey("cruise_speed");
        // Private key used to calculate CRUISE_TICK_COOLDOWN
    private static final NamespacedKey CRUISE_TICK_COOLDOWN = buildKey("cruise_tick_cooldown");
        // Private key used as default for PER_WORLD_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey PER_WORLD_CRUISE_SPEED = buildKey("per_world_cruise_speed");
        // Private key used to calculate PER_WORLD_CRUISE_TICK_COOLDOWN
    public static final NamespacedKey PER_WORLD_CRUISE_TICK_COOLDOWN = buildKey("per_world_cruise_tick_cooldown");
    private static final NamespacedKey VERT_CRUISE_SPEED = buildKey("vert_cruise_speed");
        // Private key used to calculate VERT_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey VERT_CRUISE_TICK_COOLDOWN = buildKey("vert_cruise_tick_cooldown");
        // Private key used as default for PER_WORLD_VERT_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey PER_WORLD_VERT_CRUISE_SPEED = buildKey("per_world_vert_cruise_speed");
        // Private key used to calculate PER_WORLD_VERT_CRUISE_SPEED
    public static final NamespacedKey PER_WORLD_VERT_CRUISE_TICK_COOLDOWN = buildKey(
            "per_world_vert_cruise_tick_cooldown");
    private static final NamespacedKey MAX_HEIGHT_LIMIT = buildKey("max_height_limit");
        // Private key used as default for PER_WORLD_MAX_HEIGHT_LIMIT
    public static final NamespacedKey PER_WORLD_MAX_HEIGHT_LIMIT = buildKey("per_world_max_height_limit");
    private static final NamespacedKey MAX_HEIGHT_ABOVE_GROUND = buildKey("max_height_above_ground");
        // Private key used as default for PER_WORLD_MAX_HEIGHT_ABOVE_GROUND
    public static final NamespacedKey PER_WORLD_MAX_HEIGHT_ABOVE_GROUND = buildKey("per_world_max_height_above_ground");
    public static final NamespacedKey CAN_DIRECT_CONTROL = buildKey("can_direct_control");
    public static final NamespacedKey CAN_HOVER = buildKey("can_hover");
    public static final NamespacedKey CAN_HOVER_OVER_WATER = buildKey("can_hover_over_water");
    public static final NamespacedKey MOVE_ENTITIES = buildKey("move_entities");
    public static final NamespacedKey ONLY_MOVE_PLAYERS = buildKey("only_move_players");
    public static final NamespacedKey USE_GRAVITY = buildKey("use_gravity");
    public static final NamespacedKey USE_INCLINE = buildKey("use_incline");
    public static final NamespacedKey HOVER_LIMIT = buildKey("hover_limit");
    public static final NamespacedKey HARVEST_BLOCKS = buildKey("harvest_blocks");
    public static final NamespacedKey HARVESTER_BLADE_BLOCKS = buildKey("harvester_blade_blocks");
    public static final NamespacedKey PASSTHROUGH_BLOCKS = buildKey("passthrough_blocks");

    public static final NamespacedKey INVERT_HOVER_OVER_BLOCKS = buildKey("invert_hover_over_blocks");
    public static final NamespacedKey ALLOW_HOVER_OVER_BLOCKS = buildKey("allow_hover_over_blocks");    
    public static final NamespacedKey FORBIDDEN_HOVER_OVER_BLOCKS = buildKey("forbidden_hover_over_blocks");
    public static final NamespacedKey ALLOW_VERTICAL_TAKEOFF_AND_LANDING = buildKey(
            "allow_vertical_takeoff_and_landing");
    public static final NamespacedKey DYNAMIC_LAG_SPEED_FACTOR = buildKey("dynamic_lag_speed_factor");
    public static final NamespacedKey DYNAMIC_LAG_POWER_FACTOR = buildKey("dynamic_lag_power_factor");
    public static final NamespacedKey DYNAMIC_LAG_MIN_SPEED = buildKey("dynamic_lag_min_speed");
    public static final NamespacedKey DYNAMIC_FLY_BLOCK_SPEED_FACTOR = buildKey("dynamic_fly_block_speed_factor");
    public static final NamespacedKey DYNAMIC_FLY_BLOCK = buildKey("dynamic_fly_block");
    public static final NamespacedKey CHEST_PENALTY = buildKey("chest_penalty");
    public static final NamespacedKey GRAVITY_INCLINE_DISTANCE = buildKey("gravity_incline_distance");
    public static final NamespacedKey GRAVITY_DROP_DISTANCE = buildKey("gravity_drop_distance");
    public static final NamespacedKey COLLISION_SOUND = buildKey("collision_sound");
    public static final NamespacedKey FUEL_TYPES = buildKey("fuel_types");
    public static final NamespacedKey DISABLE_TELEPORT_TO_WORLDS = buildKey("disable_teleport_to_worlds");
    public static final NamespacedKey TELEPORTATION_COOLDOWN = buildKey("teleportation_cooldown");
    public static final NamespacedKey GEAR_SHIFTS = buildKey("gear_shifts");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_TICK_COOLDOWN = buildKey("gear_shifts_affect_tick_cooldown");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT = buildKey(
            "gear_shifts_affect_direct_movement");
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS = buildKey(
            "gear_shifts_affect_cruise_skip_blocks");
    public static final NamespacedKey RELEASE_TIMEOUT = buildKey("release_timeout");
    public static final NamespacedKey MERGE_PISTON_EXTENSIONS = buildKey("merge_piston_extensions");

    public static final NamespacedKey CRUISE_ON_PILOT_LIFETIME = buildKey("cruise_on_pilot_lifetime");

    public static final NamespacedKey EXPLOSION_ARMING_TIME = buildKey("explosion_arming_time");
    public static final NamespacedKey DIRECTIONAL_DEPENDENT_MATERIALS = buildKey("directional_dependent_materials");
    public static final NamespacedKey ALLOW_INTERNAL_COLLISION_EXPLOSION = buildKey("allow_internal_collision_explosion");
    //endregion

    @Contract("_ -> new")
    private static @NotNull NamespacedKey buildKey(String key) {
        return new NamespacedKey("movecraft", key);
    }



    private static final List<Property<?>> properties = new ArrayList<>();

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
     * @param key Key of the material set property
     * @return value of the material set property
     */
    public EnumSet<Material> getMaterialSetProperty(NamespacedKey key) {
        if(!materialSetPropertyMap.containsKey(key))
            throw new IllegalStateException("Materials property " + key + " not found.");
        return materialSetPropertyMap.get(key);
    }

    private Map<NamespacedKey, Pair<Map<String, Object>, BiFunction<CraftType, String, Object>>> perWorldPropertyMap;
    private Object getPerWorldProperty(NamespacedKey key, String worldName) {
        if(!perWorldPropertyMap.containsKey(key))
            throw new IllegalStateException("Per world property " + key + " not found.");
        var pair =  perWorldPropertyMap.get(key);
        var map = pair.getLeft();
        var defaultProvider = pair.getRight();

        if(!map.containsKey(worldName))
            return defaultProvider.apply(this, worldName);

        return map.get(worldName);
    }
    /**
     * Get a per world property of this CraftType
     *
     * @param key Key of the per world property
     * @param world the world to check
     * @return value of the per world property
     */
    public Object getPerWorldProperty(NamespacedKey key, @NotNull World world) {
        return getPerWorldProperty(key, world.getName());
    }
    /**
     * Get a per world property of this CraftType
     *
     * @param key Key of the per world property
     * @param world the world to check
     * @return value of the per world property
     */
    public Object getPerWorldProperty(NamespacedKey key, @NotNull MovecraftWorld world) {
        return getPerWorldProperty(key, world.getName());
    }

    private Map<NamespacedKey, Set<RequiredBlockEntry>> requiredBlockPropertyMap;
    /**
     * Get a required block property of this CraftType
     *
     * @param key Key of the required block property
     * @return value of the required block property
     */
    public Set<RequiredBlockEntry> getRequiredBlockProperty(NamespacedKey key) {
        if(!requiredBlockPropertyMap.containsKey(key))
            throw new IllegalStateException("Required block property " + key + " not found.");
        return requiredBlockPropertyMap.get(key);
    }



    private static final List<Transform<?>> transforms = new ArrayList<>();

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
        /* Required properties */
        registerProperty(new StringProperty("name", NAME));
        registerProperty(new IntegerProperty("maxSize", MAX_SIZE));
        registerProperty(new IntegerProperty("minSize", MIN_SIZE));
        registerProperty(new MaterialSetProperty("allowedBlocks", ALLOWED_BLOCKS));
        registerProperty(new DoubleProperty("speed", SPEED));

        /* Optional properties */
        registerProperty(new RequiredBlockProperty("flyblocks", FLY_BLOCKS, type -> new HashSet<>()));
        registerProperty(new RequiredBlockProperty("detectionblocks", DETECTION_BLOCKS, type -> new HashSet<>()));
        registerProperty(new MaterialSetProperty("directionDependentMaterials", DIRECTIONAL_DEPENDENT_MATERIALS, type -> {
            var set = EnumSet.of(Material.LADDER, Material.LEVER, Material.GRINDSTONE);
            set.addAll(Tag.WALL_SIGNS.getValues());
            set.addAll(Tags.WALL_TORCHES);
            set.addAll(Tags.LANTERNS);
            return set;
        }));

        registerProperty(new ObjectPropertyImpl("forbiddenSignStrings", FORBIDDEN_SIGN_STRINGS,
                (data, type, fileKey, namespacedKey) -> data.getStringListOrEmpty(fileKey).stream().map(
                        String::toLowerCase).collect(Collectors.toSet()),
                craftType -> new HashSet<>()
        ));
        registerProperty(new PerWorldProperty<>("perWorldSpeed", PER_WORLD_SPEED,
                (type, worldName) -> type.getDoubleProperty(SPEED)));
        registerProperty(new MaterialSetProperty("forbiddenBlocks", FORBIDDEN_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("blockedByWater", BLOCKED_BY_WATER, type -> true));
        registerProperty(new BooleanProperty("canFly", CAN_FLY, type -> type.getBoolProperty(BLOCKED_BY_WATER)));
        registerProperty(new BooleanProperty("requireWaterContact", REQUIRE_WATER_CONTACT, type -> false));
        registerProperty(new BooleanProperty("tryNudge", TRY_NUDGE, type -> false));
        registerProperty(new RequiredBlockProperty("moveblocks", MOVE_BLOCKS, type -> new HashSet<>()));
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
        registerProperty(new PerWorldProperty<>("perWorldCruiseSkipBlocks", PER_WORLD_CRUISE_SKIP_BLOCKS,
                (type, worldName) -> type.getIntProperty(CRUISE_SKIP_BLOCKS)));
        registerProperty(new IntegerProperty("vertCruiseSkipBlocks", VERT_CRUISE_SKIP_BLOCKS,
                type -> type.getIntProperty(CRUISE_SKIP_BLOCKS)));
        registerProperty(new PerWorldProperty<>("perWorldVertCruiseSkipBlocks",
                PER_WORLD_VERT_CRUISE_SKIP_BLOCKS,
                (type, worldName) -> type.getIntProperty(VERT_CRUISE_SKIP_BLOCKS))
        );
        registerProperty(new BooleanProperty("halfSpeedUnderwater", HALF_SPEED_UNDERWATER, type -> false));
        registerProperty(new BooleanProperty("focusedExplosion", FOCUSED_EXPLOSION, type -> false));
        registerProperty(new BooleanProperty("mustBeSubcraft", MUST_BE_SUBCRAFT, type -> false));
        registerProperty(new IntegerProperty("staticWaterLevel", STATIC_WATER_LEVEL, type -> 0));
        registerProperty(new DoubleProperty("fuelBurnRate", FUEL_BURN_RATE, type -> 0D));
        registerProperty(new PerWorldProperty<>("perWorldFuelBurnRate", PER_WORLD_FUEL_BURN_RATE,
                (type, worldName) -> type.getDoubleProperty(FUEL_BURN_RATE)));
        registerProperty(new DoubleProperty("sinkPercent", SINK_PERCENT, type -> 0D));
        registerProperty(new DoubleProperty("overallSinkPercent", OVERALL_SINK_PERCENT, type -> 0D));
        registerProperty(new DoubleProperty("detectionMultiplier", DETECTION_MULTIPLIER, type -> 0D));
        registerProperty(new PerWorldProperty<>("perWorldDetectionMultiplier", PER_WORLD_DETECTION_MULTIPLIER,
                (type, worldName) -> type.getDoubleProperty(DETECTION_MULTIPLIER)));
        registerProperty(new DoubleProperty("underwaterDetectionMultiplier", UNDERWATER_DETECTION_MULTIPLIER,
                type-> type.getDoubleProperty(DETECTION_MULTIPLIER)));
        registerProperty(new PerWorldProperty<>("perWorldUnderWaterDetectionMultiplier",
                PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER,
                (type, worldName) -> type.getDoubleProperty(UNDERWATER_DETECTION_MULTIPLIER)));
        registerProperty(new DoubleProperty("sinkSpeed", SINK_SPEED, type -> 1D));
        registerProperty(new IntegerProperty("sinkRateTicks", SINK_RATE_TICKS,
                type -> (int) Math.ceil(20 / type.getDoubleProperty(SINK_SPEED))));
        registerProperty(new BooleanProperty("keepMovingOnSink", KEEP_MOVING_ON_SINK, type -> false));
        registerProperty(new IntegerProperty("smokeOnSink", SMOKE_ON_SINK, type -> 0));
        registerProperty(new FloatProperty("explodeOnCrash", EXPLODE_ON_CRASH, type -> 0F));
        registerProperty(new BooleanProperty("incendiaryOnCrash", INCENDIARY_ON_CRASH, type -> false));
        registerProperty(new FloatProperty("collisionExplosion", COLLISION_EXPLOSION, type -> 0F));
        registerProperty(new FloatProperty("underwaterCollisionExplosion", UNDERWATER_COLLISION_EXPLOSION, type -> type.getFloatProperty(COLLISION_EXPLOSION)));
        registerProperty(new IntegerProperty("minHeightLimit", MIN_HEIGHT_LIMIT, type -> Integer.MIN_VALUE));
        registerProperty(new PerWorldProperty<>("perWorldMinHeightLimit", PER_WORLD_MIN_HEIGHT_LIMIT,
                (type, worldName) -> type.getIntProperty(MIN_HEIGHT_LIMIT)));
        registerProperty(new DoubleProperty("cruiseSpeed", CRUISE_SPEED, type -> type.getDoubleProperty(SPEED)));
        registerProperty(new PerWorldProperty<>("perWorldCruiseSpeed", PER_WORLD_CRUISE_SPEED,
                (type, worldName) -> type.getDoubleProperty(CRUISE_SPEED)));
        registerProperty(new DoubleProperty("vertCruiseSpeed", VERT_CRUISE_SPEED,
                type -> type.getDoubleProperty(CRUISE_SPEED)));
        registerProperty(new PerWorldProperty<>("perWorldVertCruiseSpeed", PER_WORLD_VERT_CRUISE_SPEED,
                (type, worldName) -> type.getDoubleProperty(VERT_CRUISE_SPEED)));
        registerProperty(new IntegerProperty("maxHeightLimit", MAX_HEIGHT_LIMIT, type -> Integer.MAX_VALUE));
        registerProperty(new PerWorldProperty<>("perWorldMaxHeightLimit", PER_WORLD_MAX_HEIGHT_LIMIT,
                (type, worldName) -> {
                    var w = Bukkit.getWorld(worldName);
                    if(w == null)
                        return type.getIntProperty(MAX_HEIGHT_LIMIT);
                    return Math.min(type.getIntProperty(MAX_HEIGHT_LIMIT), w.getMaxHeight());
                }
        ));
        registerProperty(new IntegerProperty("maxHeightAboveGround", MAX_HEIGHT_ABOVE_GROUND, type -> -1));
        registerProperty(new PerWorldProperty<>("perWorldMaxHeightAboveGround",
                PER_WORLD_MAX_HEIGHT_ABOVE_GROUND,
                (type, worldName) -> {
                    var w = Bukkit.getWorld(worldName);
                    if(w == null)
                        return type.getIntProperty(MAX_HEIGHT_ABOVE_GROUND);

                    return Math.min(type.getIntProperty(MAX_HEIGHT_ABOVE_GROUND), w.getMaxHeight() - w.getMinHeight());
                }
        ));
        registerProperty(new BooleanProperty("canDirectControl", CAN_DIRECT_CONTROL, type -> true));
        registerProperty(new BooleanProperty("canHover", CAN_HOVER, type -> false));
        registerProperty(new BooleanProperty("canHoverOverWater", CAN_HOVER_OVER_WATER, type -> true));
        registerProperty(new BooleanProperty("moveEntities", MOVE_ENTITIES, type -> true));
        registerProperty(new BooleanProperty("onlyMovePlayers", ONLY_MOVE_PLAYERS, type -> true));
        registerProperty(new BooleanProperty("useGravity", USE_GRAVITY, type -> false));
        registerProperty(new BooleanProperty("useIncline", USE_INCLINE, type -> false));        
        registerProperty(new IntegerProperty("hoverLimit", HOVER_LIMIT, type -> 0));
        registerProperty(new MaterialSetProperty("harvestBlocks", HARVEST_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("harvesterBladeBlocks", HARVESTER_BLADE_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("passthroughBlocks", PASSTHROUGH_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("invertHoverOverBlocks", INVERT_HOVER_OVER_BLOCKS, type -> true));
        registerProperty(new MaterialSetProperty("allowHoverOverBlocks", ALLOW_HOVER_OVER_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new MaterialSetProperty("forbiddenHoverOverBlocks", FORBIDDEN_HOVER_OVER_BLOCKS,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new BooleanProperty("allowVerticalTakeoffAndLanding",
                ALLOW_VERTICAL_TAKEOFF_AND_LANDING,
                type -> true
        ));
        registerProperty(new DoubleProperty("dynamicLagSpeedFactor", DYNAMIC_LAG_SPEED_FACTOR, type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagPowerFactor", DYNAMIC_LAG_POWER_FACTOR, type -> 0D));
        registerProperty(new DoubleProperty("dynamicLagMinSpeed", DYNAMIC_LAG_MIN_SPEED, type -> 0D));
        registerProperty(new DoubleProperty("dynamicFlyBlockSpeedFactor", DYNAMIC_FLY_BLOCK_SPEED_FACTOR,
                type -> 0D));
        registerProperty(new MaterialSetProperty("dynamicFlyBlock", DYNAMIC_FLY_BLOCK,
                type -> EnumSet.noneOf(Material.class)));
        registerProperty(new DoubleProperty("chestPenalty", CHEST_PENALTY, type -> 0D));
        registerProperty(new IntegerProperty("gravityInclineDistance", GRAVITY_INCLINE_DISTANCE, type -> -1));
        registerProperty(new IntegerProperty("gravityDropDistance", GRAVITY_DROP_DISTANCE, type -> -8));
        registerProperty(new ObjectPropertyImpl("collisionSound", COLLISION_SOUND,
                (data, type, fileKey, namespacedKey) -> data.getSound(fileKey),
                type -> Sound.sound(Key.key("block.anvil.land"), Sound.Source.NEUTRAL, 2.0f,1.0f)
        ));
        registerProperty(new ObjectPropertyImpl("fuelTypes", FUEL_TYPES,
                (data, type, fileKey, namespacedKey) -> {
                    var map = data.getData(fileKey).getBackingData();
                    if(map.isEmpty())
                        throw new TypeData.InvalidValueException("Value for " + fileKey + " must not be an empty map");

                    Map<Material, Double> fuelTypes = new HashMap<>();
                    for(var i : map.entrySet()) {
                        EnumSet<Material> materials = Tags.parseMaterials(i.getKey());
                        Object o = i.getValue();
                        double burnRate;
                        if (o instanceof String)
                            burnRate = Double.parseDouble((String) o);
                        else if (o instanceof Integer)
                            burnRate = ((Integer) o).doubleValue();
                        else
                            burnRate = (double) o;
                        for(Material m : materials) {
                            fuelTypes.put(m, burnRate);
                        }
                    }
                    return fuelTypes;
                },
                type -> {
                    Map<Material, Double> fuelTypes = new HashMap<>();
                    fuelTypes.put(Material.COAL_BLOCK, 80.0);
                    fuelTypes.put(Material.COAL, 8.0);
                    fuelTypes.put(Material.CHARCOAL, 8.0);
                    return fuelTypes;
                }
        ));
        registerProperty(new ObjectPropertyImpl("disableTeleportToWorlds", DISABLE_TELEPORT_TO_WORLDS,
                (data, type, fileKey, namespacedKey) -> data.getStringList(fileKey),
                type -> new ArrayList<>()
        ));
        registerProperty(new IntegerProperty("teleportationCooldown", TELEPORTATION_COOLDOWN, type -> 0));
        registerProperty(new IntegerProperty("gearShifts", GEAR_SHIFTS, type -> 1));
        registerProperty(new BooleanProperty("gearShiftsAffectTickCooldown", GEAR_SHIFTS_AFFECT_TICK_COOLDOWN,
                type -> true));
        registerProperty(new BooleanProperty("gearShiftsAffectDirectMovement",
                GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT,
                type -> false
        ));
        registerProperty(new BooleanProperty("gearShiftsAffectCruiseSkipBlocks",
                GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS,
                type -> false
        ));
        registerProperty(new IntegerProperty("releaseTimeout", RELEASE_TIMEOUT, type -> 30));
        registerProperty(new BooleanProperty("mergePistonExtensions", MERGE_PISTON_EXTENSIONS, type -> false));
        registerProperty(new IntegerProperty("cruiseOnPilotLifetime", CRUISE_ON_PILOT_LIFETIME, type -> 15*20));
        registerProperty(new IntegerProperty("explosionArmingTime", EXPLOSION_ARMING_TIME, type -> 1000));
        registerProperty(new BooleanProperty("allowInternalCollisionExplosion", ALLOW_INTERNAL_COLLISION_EXPLOSION, type -> false));

        /* Craft type transforms */
        // Convert speed to TICK_COOLDOWN
        registerTypeTransform((IntegerTransform) (data, type) -> {
            int tickCooldown = (int) Math.ceil(20 / type.getDoubleProperty(SPEED));
            data.put(TICK_COOLDOWN, tickCooldown);
            return data;
        });
        // Convert canFly to blockedByWater and remove canFly
        registerTypeTransform((BooleanTransform) (data, type) -> {
            data.put(BLOCKED_BY_WATER, data.get(CAN_FLY));
            data.remove(CAN_FLY);
            return data;
        });
        // Convert cruiseSpeed to CRUISE_TICK_COOLDOWN
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put(CRUISE_TICK_COOLDOWN,
                    (int) Math.round((1.0 + data.get(CRUISE_SKIP_BLOCKS))
                            * 20.0 / type.getDoubleProperty(CRUISE_SPEED))
            );
            return data;
        });
        // Convert vertCruiseSpeed to VERT_CRUISE_TICK_COOLDOWN
        registerTypeTransform((IntegerTransform) (data, type) -> {
            data.put(VERT_CRUISE_TICK_COOLDOWN,
                    (int) Math.round((1.0 + data.get(VERT_CRUISE_SKIP_BLOCKS))
                            * 20.0 / type.getDoubleProperty(VERT_CRUISE_SPEED))
            );
            return data;
        });
        // Fix gravityDropDistance to be negative
        registerTypeTransform((IntegerTransform) (data, type) -> {
            int dropDist = data.get(GRAVITY_DROP_DISTANCE);
            data.put(GRAVITY_DROP_DISTANCE, dropDist > 0 ? -dropDist : dropDist);
            return data;
        });
        // Add WATER to PASSTHROUGH_BLOCKS if not BLOCKED_BY_WATER
        registerTypeTransform((MaterialSetTransform) (data, type) -> {
            if(type.getBoolProperty(BLOCKED_BY_WATER))
                return data;

            var passthroughBlocks = data.get(PASSTHROUGH_BLOCKS);
            passthroughBlocks.addAll(Tags.WATER);
            data.put(PASSTHROUGH_BLOCKS, passthroughBlocks);
            return data;
        });
        // Add WATER to FORBIDDEN_HOVER_OVER_BLOCKS if not canHoverOverWater
        registerTypeTransform((MaterialSetTransform) (data, type) -> {
            if(type.getBoolProperty(CAN_HOVER_OVER_WATER))
                return data;

            var forbiddenHoverOverBlocks = data.get(FORBIDDEN_HOVER_OVER_BLOCKS);
            forbiddenHoverOverBlocks.addAll(Tags.WATER);
            data.put(FORBIDDEN_HOVER_OVER_BLOCKS, forbiddenHoverOverBlocks);
            return data;
        });
        // Convert perWorldSpeed to PER_WORLD_TICK_COOLDOWN
        registerTypeTransform((PerWorldTransform) (data, type) -> {
            var map = data.get(PER_WORLD_SPEED).getLeft();
            Map<String, Object> resultMap = new HashMap<>();
            for(var i : map.entrySet()) {
                var value = i.getValue();
                if(!(value instanceof Double))
                    throw new IllegalStateException("PER_WORLD_SPEED must be of type Double");
                resultMap.put(i.getKey(), (int) Math.ceil(20 / (double) value));
            }
            var defaultProvider = (BiFunction<CraftType, String, Object>)
                    (craftType, worldName) -> craftType.getIntProperty(TICK_COOLDOWN);
            data.put(PER_WORLD_TICK_COOLDOWN, new Pair<>(resultMap, defaultProvider));
            return data;
        });
        // Convert perWorldCruiseSpeed to PER_WORLD_CRUISE_TICK_COOLDOWN
        registerTypeTransform((PerWorldTransform) (data, type) -> {
            var map = data.get(PER_WORLD_CRUISE_SPEED).getLeft();
            Map<String, Object> resultMap = new HashMap<>();
            for(var i : map.entrySet()) {
                var value = i.getValue();
                if(!(value instanceof Double))
                    throw new IllegalStateException("PER_WORLD_CRUISE_SPEED must be of type Double");
                var world = i.getKey();
                var skip = type.getPerWorldProperty(PER_WORLD_CRUISE_SKIP_BLOCKS, world);
                if(!(skip instanceof Integer))
                    throw new IllegalStateException("PER_WORLD_CRUISE_SKIP_BLOCKS must be of type Integer");
                resultMap.put(world, (int) Math.round((1.0 + (int) skip) * 20.0 / (double) value));
            }
            var defaultProvider = (BiFunction<CraftType, String, Object>)
                    (craftType, worldName) -> craftType.getIntProperty(CRUISE_TICK_COOLDOWN);
            data.put(PER_WORLD_CRUISE_TICK_COOLDOWN, new Pair<>(resultMap, defaultProvider));
            return data;
        });
        // Convert perWorldVertCruiseSpeed to PER_WORLD_VERT_CRUISE_TICK_COOLDOWN
        registerTypeTransform((PerWorldTransform) (data, type) -> {
            var map = data.get(PER_WORLD_VERT_CRUISE_SPEED).getLeft();
            Map<String, Object> resultMap = new HashMap<>();
            for(var i : map.entrySet()) {
                var value = i.getValue();
                if(!(value instanceof Double))
                    throw new IllegalStateException("PER_WORLD_VERT_CRUISE_SPEED must be of type Double");
                var world = i.getKey();
                var skip = type.getPerWorldProperty(PER_WORLD_VERT_CRUISE_SKIP_BLOCKS, world);
                if(!(skip instanceof Integer))
                    throw new IllegalStateException("PER_WORLD_VERT_CRUISE_SKIP_BLOCKS must be of type Integer");
                resultMap.put(world, (int) Math.round((1.0 + (int) skip) * 20.0 / (double) value));
            }
            var defaultProvider = (BiFunction<CraftType, String, Object>)
                    (craftType, worldName) -> craftType.getIntProperty(VERT_CRUISE_TICK_COOLDOWN);
            data.put(PER_WORLD_VERT_CRUISE_TICK_COOLDOWN, new Pair<>(resultMap, defaultProvider));
            return data;
        });
        // Remove speed, sinkSpeed, cruiseSpeed, vertCruiseSpeed, perWorldSpeed, perWorldCruiseSpeed,
        //   and perWorldVertCruiseSpeed
        registerTypeTransform((DoubleTransform) (data, type) -> {
            data.remove(SPEED);
            data.remove(PER_WORLD_SPEED);
            data.remove(SINK_SPEED);
            data.remove(CRUISE_SPEED);
            data.remove(PER_WORLD_CRUISE_SPEED);
            data.remove(VERT_CRUISE_SPEED);
            data.remove(PER_WORLD_VERT_CRUISE_SPEED);
            return data;
        });

        /* Craft type validators */
        registerTypeValidator(
                type -> type.getIntProperty(MIN_HEIGHT_LIMIT) <= type.getIntProperty(MAX_HEIGHT_LIMIT),
                "minHeightLimit must be less than or equal to maxHeightLimit"
        );
        registerTypeValidator(
                type -> type.getIntProperty(HOVER_LIMIT) >= 0,
                "hoverLimit must be greater than or equal to zero"
        );
        registerTypeValidator(
                type -> type.getIntProperty(GEAR_SHIFTS) >= 1,
                "gearShifts must be greater than or equal to one"
        );
        registerTypeValidator(
                type -> {
                    for (var i : type.perWorldPropertyMap.get(PER_WORLD_MIN_HEIGHT_LIMIT).getLeft().entrySet()) {
                        var a = i.getValue();
                        if(!(a instanceof Integer))
                            throw new IllegalStateException(
                                    "PER_WORLD_MIN_HEIGHT_LIMIT must have values of type Integer");
                        int value = (int) a;
                        var w = Bukkit.getWorld(i.getKey());
                        if(w == null)
                            throw new IllegalArgumentException("World '" + i.getKey() + "' does not exist.");
                        if(value < w.getMinHeight() || value > w.getMaxHeight())
                            return false;
                    }
                    return true;
                },
                "perWorldMinHeightLimit must be within the world height limits"
        );
        registerTypeValidator(
                type -> {
                    for (var i : type.perWorldPropertyMap.get(PER_WORLD_MAX_HEIGHT_LIMIT).getLeft().entrySet()) {
                        var a = i.getValue();
                        if(!(a instanceof Integer))
                            throw new IllegalStateException(
                                    "PER_WORLD_MAX_HEIGHT_LIMIT must have values of type Integer");
                        var value = (int) a;
                        var w = Bukkit.getWorld(i.getKey());
                        if(w == null)
                            throw new IllegalArgumentException("World '" + i.getKey() + "' does not exist.");
                        if(value < w.getMinHeight() || value > w.getMaxHeight())
                            return false;
                    }
                    return true;
                },
                "perWorldMaxHeightLimit must be within the world height limits"
        );
        registerTypeValidator(
                type -> {
                    var max = type.perWorldPropertyMap.get(PER_WORLD_MAX_HEIGHT_LIMIT).getLeft();
                    var min = type.perWorldPropertyMap.get(PER_WORLD_MIN_HEIGHT_LIMIT).getLeft();
                    var worlds = new HashSet<String>();
                    worlds.addAll(max.keySet());
                    worlds.addAll(min.keySet());
                    for (var world : worlds) {
                        if(!max.containsKey(world) || !min.containsKey(world))
                            continue; // Only worry about worlds which have both a max and a min

                        var worldMax = max.get(world);
                        var worldMin = min.get(world);
                        if (!(worldMax instanceof Integer) || !(worldMin instanceof Integer))
                            throw new IllegalStateException("PER_WORLD_MIN_HEIGHT_LIMIT and PER_WORLD_MAX_HEIGHT_LIMIT"
                                    + " must have values of type Integer");

                        int worldMaxInt = (int) worldMax;
                        int worldMinInt = (int) worldMin;
                        if (worldMaxInt < worldMinInt)
                            return false;
                    }
                    return true;
                },
                "perWorldMaxHeightLimit must be more than perWorldMinHeightLimit"
        );
    }



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
        perWorldPropertyMap = new HashMap<>();
        requiredBlockPropertyMap = new HashMap<>();

        for(var property : properties) {
            if(property instanceof StringProperty)
                stringPropertyMap.put(property.getNamespacedKey(),
                        ((StringProperty) property).load(data, this));
            else if(property instanceof IntegerProperty)
                intPropertyMap.put(property.getNamespacedKey(),
                        ((IntegerProperty) property).load(data, this));
            else if(property instanceof BooleanProperty)
                boolPropertyMap.put(property.getNamespacedKey(),
                        ((BooleanProperty) property).load(data, this));
            else if(property instanceof FloatProperty)
                floatPropertyMap.put(property.getNamespacedKey(),
                        ((FloatProperty) property).load(data, this));
            else if(property instanceof DoubleProperty)
                doublePropertyMap.put(property.getNamespacedKey(),
                        ((DoubleProperty) property).load(data, this));
            else if(property instanceof ObjectProperty)
                objectPropertyMap.put(property.getNamespacedKey(),
                        ((ObjectProperty) property).load(data, this));
            else if(property instanceof MaterialSetProperty)
                materialSetPropertyMap.put(property.getNamespacedKey(),
                        ((MaterialSetProperty) property).load(data, this));
            else if(property instanceof PerWorldProperty<?>) {
                var perWorldProperty = (PerWorldProperty<?>) property;
                var map = perWorldProperty.load(data, this);
                if(map == null)
                    continue;

                // Conversion of the map is simple, copy it to one of the right type.
                Map<String, Object> resultMap = new HashMap<>(map);
                // The defaultProvider is of type Function<CraftType, ?> which can not be cast to
                //   Function<CraftType, Object>.  We can create a Function<CraftType, Object> by chaining an identity
                //   Function<Object, Object> on the end.
                var defaultProvider = perWorldProperty.getDefaultProvider().andThen(
                        (Function<Object, Object>) o -> o);
                var pair = new Pair<>(resultMap, defaultProvider);
                perWorldPropertyMap.put(perWorldProperty.getNamespacedKey(), pair);
            }
            else if(property instanceof RequiredBlockProperty)
                requiredBlockPropertyMap.put(property.getNamespacedKey(),
                        ((RequiredBlockProperty) property).load(data, this));
        }

        // Transform craft type
        for(var transform : transforms) {
            if(transform instanceof StringTransform)
                stringPropertyMap = ((StringTransform) transform).transform(stringPropertyMap, this);
            else if(transform instanceof IntegerTransform)
                intPropertyMap = ((IntegerTransform) transform).transform(intPropertyMap, this);
            else if(transform instanceof BooleanTransform)
                boolPropertyMap = ((BooleanTransform) transform).transform(boolPropertyMap, this);
            else if(transform instanceof FloatTransform)
                floatPropertyMap = ((FloatTransform) transform).transform(floatPropertyMap, this);
            else if(transform instanceof DoubleTransform)
                doublePropertyMap = ((DoubleTransform) transform).transform(doublePropertyMap, this);
            else if(transform instanceof ObjectTransform)
                objectPropertyMap = ((ObjectTransform) transform).transform(objectPropertyMap, this);
            else if(transform instanceof MaterialSetTransform)
                materialSetPropertyMap = ((MaterialSetTransform) transform).transform(materialSetPropertyMap,
                        this);
            else if(transform instanceof PerWorldTransform)
                perWorldPropertyMap = ((PerWorldTransform) transform).transform(perWorldPropertyMap,
                        this);
            else if(transform instanceof RequiredBlockTransform)
                requiredBlockPropertyMap = ((RequiredBlockTransform) transform).transform(requiredBlockPropertyMap,
                        this);
        }

        // Validate craft type
        for(var validator : validators) {
            if(!validator.getLeft().test(this))
                throw new IllegalArgumentException(validator.getRight());
        }
    }

    public static class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }
}