package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import org.bukkit.NamespacedKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropertyKeys {

    // region required base settings
    @Deprecated(forRemoval = true)
    /**
     * Use TypeSafeCraftType#getName() instead
     */
    public static final PropertyKey<String> NAME =
            register(PropertyKeyTypes.stringPropertyKey(key("name"), t -> t.getName()).immutable());
    public static final PropertyKey<Integer> MAX_SIZE =
            register(PropertyKeyTypes.intPropertyKey(key("max_size")).immutable());
    public static final PropertyKey<Integer> MIN_SIZE =
            register(PropertyKeyTypes.intPropertyKey(key("min_size")).immutable());
    public static final PropertyKey<BlockSetProperty> ALLOWED_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("allowed_blocks")).immutable());
    public static final PropertyKey<PerWorldData<Double>> SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("speed")).perWorld().immutable());
    // endregion required base settings

    // region block constraints
    public static final PropertyKey<Set<RequiredBlockEntry>> FLY_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("flyblocks"), t -> new HashSet<>()
            ).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> DETECTION_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("detection_blocks"), t -> new HashSet<>()
            ).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> MOVE_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("move_blocks"), t -> new HashSet<>()
            ).immutable());
    // endregion block constraints

    // region block sets
    public static final PropertyKey<BlockSetProperty> DIRECTIONAL_DEPENDENT_MATERIALS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("directional_dependent_materials")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("forbidden_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVEST_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("harvest_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVESTER_BLADE_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("harvester_blade_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> PASSTHROUGH_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("passthrough_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_HOVER_OVER_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("forbidden_hover_over_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> DYNAMIC_FLY_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("dynamic_fly_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> REQUIRED_CONTACT_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("required_contact_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> TRACTION_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("traction_blocks")).immutable());
    // endregion block sets

    // idk if we should keep these

    public static final PropertyKey<Boolean> CAN_FLY =
            register(PropertyKeyTypes.boolPropertyKey(key("can_fly")).immutable());
    public static final PropertyKey<Boolean> BLOCKED_BY_WATER =
            register(PropertyKeyTypes.boolPropertyKey(key("blocked_by_water")).immutable());
    public static final PropertyKey<Integer> TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("tick_cooldown")).immutable());
    public static final PropertyKey<PerWorldData<Integer>> PER_WORLD_TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("per_world_tick_cooldown")).perWorld().immutable());
    public static final PropertyKey<Boolean> REQUIRE_WATER_CONTACT =
            register(PropertyKeyTypes.boolPropertyKey(key("require_water_contact")).immutable());
    public static final PropertyKey<Boolean> TRY_NUDGE =
            register(PropertyKeyTypes.boolPropertyKey(key("try_nudge")).immutable());
    public static final PropertyKey<Boolean> HALF_SPEED_UNDERWATER =
            register(PropertyKeyTypes.boolPropertyKey(key("half_speed_underwater")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_SPEED_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag_speed_factor")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_POWER_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag_power_factor")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_MIN_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag_min_speed")).immutable());
    public static final PropertyKey<Double> DYNAMIC_FLY_BLOCK_SPEED_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_fly_block_speed_factor")).immutable());
    public static final PropertyKey<BlockSetProperty> DYNAMIC_FLY_BLOCK =
            register(PropertyKeyTypes.blockSetPropertyKey(key("dynamic_fly_block")).immutable());
    public static final PropertyKey<Double> CHEST_PENALTY =
            register(PropertyKeyTypes.doublePropertyKey(key("chest_penalty")).immutable());
    // endidk if we should keep these

    public static final PropertyKey<List<String>> FORBIDDEN_SIGN_STRINGS =
            register(PropertyKeyTypes.stringListPropertyKey(key("forbidden_sign_strings")).immutable());
    public static final PropertyKey<Boolean> CAN_CRUISE =
            register(PropertyKeyTypes.boolPropertyKey(key("can_cruise"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_TELEPORT =
            register(PropertyKeyTypes.boolPropertyKey(key("can_teleport"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_SWITCH_WORLD =
            register(PropertyKeyTypes.boolPropertyKey(key("can_switch_world"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_BE_NAMED =
            register(PropertyKeyTypes.boolPropertyKey(key("can_be_named"), t -> true).immutable());
    public static final PropertyKey<Boolean> CRUISE_ON_PILOT =
            register(PropertyKeyTypes.boolPropertyKey(key("cruise_on_pilot"), t -> false).immutable());
    public static final PropertyKey<Integer> CRUISE_ON_PILOT_VERT_MOVE =
            register(PropertyKeyTypes.intPropertyKey(key("cruise_on_pilot_vert_move"), t -> 0).immutable());
    public static final PropertyKey<Boolean> ALLOW_VERTICAL_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(key("allow_vertical_movement"), t -> true).immutable());
    public static final PropertyKey<Boolean> ALLOW_HORIZONTAL_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(key("allow_horizontal_movement"), t -> true).immutable());
    public static final PropertyKey<Boolean> ROTATE_AT_MIDPOINT =
            register(PropertyKeyTypes.boolPropertyKey(key("rotate_at_midpoint"), t -> true).immutable());
    public static final PropertyKey<Boolean> ALLOW_REMOTE_SIGN =
            register(PropertyKeyTypes.boolPropertyKey(key("allow_remote_sign"), t -> true).immutable());
    public static final PropertyKey<Boolean> CAN_STATIC_MOVE =
            register(PropertyKeyTypes.boolPropertyKey(key("can_static_move"), t -> false).immutable());
    public static final PropertyKey<Integer> MAX_STATIC_MOVE =
            register(PropertyKeyTypes.intPropertyKey(key("max_static_move"), t -> 10000).immutable());
    public static final PropertyKey<PerWorldData<Integer>> CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(key("cruise_skip_blocks"), t -> 0).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> VERT_CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("vert_cruise_skip_blocks"), t -> t.get(CRUISE_SKIP_BLOCKS).get()
            ).perWorld().immutable());
    public static final PropertyKey<Boolean> FOCUSED_EXPLOSION =
            register(PropertyKeyTypes.boolPropertyKey(key("focused_explosion"), t -> false).immutable());
    public static final PropertyKey<Boolean> MUST_BE_SUBCRAFT =
            register(PropertyKeyTypes.boolPropertyKey(key("must_be_subcraft"), t -> false).immutable());
    public static final PropertyKey<Integer> STATIC_WATER_LEVEL =
            register(PropertyKeyTypes.intPropertyKey(key("static_water_level"), t -> 0).immutable());
    public static final PropertyKey<PerWorldData<Double>> FUEL_BURN_RATE =
            register(PropertyKeyTypes.doublePropertyKey(key("fuel_burn_rate"), t -> 0D).perWorld().immutable());
    public static final PropertyKey<Double> SINK_PERCENT =
            register(PropertyKeyTypes.doublePropertyKey(key("sink_percent"), t -> 0D).immutable());
    public static final PropertyKey<Double> OVERALL_SINK_PERCENT =
            register(PropertyKeyTypes.doublePropertyKey(key("overall_sink_percent"), t -> 0D).immutable());
    public static final PropertyKey<PerWorldData<Double>> DETECTION_MULTIPLIER =
            register(PropertyKeyTypes.doublePropertyKey(key("detection_multiplier"), t -> 0D).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> UNDERWATER_DETECTION_MULTIPLIER =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("underwater_detection_multiplier"), t -> t.get(DETECTION_MULTIPLIER).get()
            ).perWorld().immutable());
    public static final PropertyKey<Double> SINK_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("sink_speed"), t -> 1D).immutable());
    public static final PropertyKey<Integer> SINK_RATE_TICKS =
            register(PropertyKeyTypes.intPropertyKey(
            key("sink_rate_ticks"), t -> (int) Math.ceil(20 / t.get(SINK_SPEED))
            ).immutable());
    public static final PropertyKey<Boolean> KEEP_MOVING_ON_SINK =
            register(PropertyKeyTypes.boolPropertyKey(key("keep_moving_on_sink"), t -> false).immutable());
    public static final PropertyKey<Integer> SMOKE_ON_SINK =
            register(PropertyKeyTypes.intPropertyKey(key("smoke_on_sink"), t -> 0).immutable());
    public static final PropertyKey<Float> EXPLODE_ON_CRASH =
            register(PropertyKeyTypes.floatPropertyKey(key("explode_on_crash"), t -> 0F).immutable());
    public static final PropertyKey<Boolean> INCENDIARY_ON_CRASH =
            register(PropertyKeyTypes.boolPropertyKey(key("incendiary_on_crash"), t -> false).immutable());
    public static final PropertyKey<Float> COLLISION_EXPLOSION =
            register(PropertyKeyTypes.floatPropertyKey(key("collision_explosion"), t -> 0F).immutable());
    public static final PropertyKey<Float> UNDERWATER_COLLISION_EXPLOSION =
            register(PropertyKeyTypes.floatPropertyKey(
                    key("underwater_collision_explosion"), t -> t.get(COLLISION_EXPLOSION)
            ).immutable());
    public static final PropertyKey<PerWorldData<Integer>> MIN_HEIGHT_LIMIT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("min_height_limit"), t -> Integer.MIN_VALUE
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> CRUISE_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("cruise_speed"), t -> t.get(SPEED).get()
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> VERT_CRUISE_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("vert_cruise_speed"), t -> t.get(CRUISE_SPEED).get()
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> MAX_HEIGHT_LIMIT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("max_height_limit"), t -> Integer.MAX_VALUE
            ).perWorld().immutable());
    public static final PropertyKey<Integer> MAX_HEIGHT_ABOVE_GROUND =
            register(PropertyKeyTypes.intPropertyKey(
                    key("max_height_above_ground"), t -> -1
            ).immutable());
    public static final PropertyKey<Boolean> CAN_DIRECT_CONTROL =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("can_direct_control"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> CAN_HOVER =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("can_hover"), t -> false
            ).immutable());
    public static final PropertyKey<Boolean> CAN_HOVER_OVER_WATER =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("can_hover_over_water"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> CAN_MOVE_ENTITIES =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("can_move_entities"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> ONLY_MOVE_PLAYERS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("only_move_players"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> USE_GRAVITY =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("use_gravity"), t -> false
            ).immutable());
    public static final PropertyKey<Boolean> ALLOW_VERTICAL_TAKEOFF_AND_LANDING =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("allow_vertical_takeoff_and_landing"), t -> true
            ).immutable());
    public static final PropertyKey<Integer> GRAVITY_INCLINE_DISTANCE =
            register(PropertyKeyTypes.intPropertyKey(
                    key("gravity_incline_distance"), t -> -1
            ).immutable());
    public static final PropertyKey<Integer> GRAVITY_DROP_DISTANCE =
            register(PropertyKeyTypes.intPropertyKey(
                    key("gravity_drop_distance"), t -> -8
            ).immutable());
    public static final PropertyKey<Object> COLLISION_SOUND = null; //TODO: Needs implementation
    public static final PropertyKey<Object> FUEL_TYPES = null; //TODO: Also needs implementation
    // Modified from original
    public static final PropertyKey<List<String>> DISABLE_TELEPORT_TO_WORLDS =
            register(PropertyKeyTypes.stringListPropertyKey(key("disable_teleport_to_worlds")).immutable());
    public static final PropertyKey<Integer> TELEPORTATION_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("teleportation_cooldown")).immutable());
    public static final PropertyKey<Integer> GEAR_SHIFTS =
            register(PropertyKeyTypes.intPropertyKey(key("gear_shifts"), t -> 1).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_TICK_COOLDOWN =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shift_affect_tick_cooldown"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_DIRECT_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shift_affect_direct_movement"), t -> false
            ).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_AFFECT_CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shift_affect_cruise_skip_blocks"), t -> false
            ).immutable());
    public static final PropertyKey<Integer> RELEASE_TIMEOUT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("release_timeout"), t -> 30
            ).immutable());
    public static final PropertyKey<Boolean> MERGE_PISTON_EXTENSIONS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("merge_piston_extensions"), t -> false
            ).immutable());
    public static final PropertyKey<Integer> CRUISE_ON_PILOT_LIFETIME =
            register(PropertyKeyTypes.intPropertyKey(
                    key("release_timeout"), t -> 15*20
            ).immutable());
    public static final PropertyKey<Integer> EXPLOSION_ARMING_TIME =
            register(PropertyKeyTypes.intPropertyKey(
                    key("release_timeout"), t -> 1000
            ).immutable());
    public static final PropertyKey<Boolean> ALLOW_INTERNAL_EXPLOSION =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("merge_piston_extensions"), t -> false
            ).immutable());

    public static <T> PropertyKey<T> register(PropertyKey<T> propertyKey) {
        return TypeSafeCraftType.PROPERTY_REGISTRY.register(propertyKey.key(), propertyKey);
    }

    private static NamespacedKey key(String path) {
        return new NamespacedKey("movecraft", path);
    }

    static void registerAll() {

    }

}
