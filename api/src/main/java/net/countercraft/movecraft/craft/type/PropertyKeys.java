package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.controller.AbstractRotationController;
import net.countercraft.movecraft.craft.controller.rotation.DefaultRotationController;
import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import net.countercraft.movecraft.craft.type.property.NamespacedKeyToDoubleProperty;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemType;

import java.util.*;

public class PropertyKeys {

    // region required base settings
    @Deprecated(forRemoval = true)
    /**
     * Use TypeSafeCraftType#getName() instead
     */
    public static final PropertyKey<String> NAME =
            register(PropertyKeyTypes.stringPropertyKey(key("general/name"), t -> t.getName()).immutable());
    public static final PropertyKey<Integer> MAX_SIZE =
            register(PropertyKeyTypes.intPropertyKey(key("constraints/size/max")).immutable());
    public static final PropertyKey<Integer> MIN_SIZE =
            register(PropertyKeyTypes.intPropertyKey(key("constraints/size/min")).immutable());
    public static final PropertyKey<BlockSetProperty> ALLOWED_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("general/block/allowed")).immutable());
    public static final PropertyKey<PerWorldData<Double>> SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("movement/speed/speed")).perWorld().immutable());
    // endregion required base settings

    // region block constraints
    // TODO: Rename to something like "keepalive" blocks, that is more fitting
    public static final PropertyKey<Set<RequiredBlockEntry>> FLY_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("constraints/block/flyblocks"), t -> new HashSet<>()
            ).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> DETECTION_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("constraints/block/detection_blocks"), t -> new HashSet<>()
            ).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> MOVE_BLOCKS =
            register(PropertyKeyTypes.requiredBlockEntrySetKey(
                    key("constraints/block/move_blocks"), t -> new HashSet<>()
            ).immutable());
    // endregion block constraints

    // region block sets
    public static final PropertyKey<BlockSetProperty> DIRECTIONAL_DEPENDENT_MATERIALS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("general/block/directional")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("general/block/forbidden")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVEST_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("harvester/breakable_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVESTER_BLADE_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("harvester/harvester_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> PASSTHROUGH_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("movement/passthrough_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_HOVER_OVER_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("constraints/movement/hover/forbidden_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> DYNAMIC_FLY_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("movement/dynamic_fly_blocks/blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> REQUIRED_CONTACT_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("constraints/movement/required_contact_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> TRACTION_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("constraints/movement/traction_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> MOVE_BREAK_BLOCKS =
            register(PropertyKeyTypes.blockSetPropertyKey(key("movement/move_break_blocks"), new BlockSetProperty(EnumSet.of(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR))).immutable());
    // endregion block sets

    // region idk if we should keep these
    public static final PropertyKey<Boolean> CAN_FLY =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/movement/can_fly")).immutable());
    public static final PropertyKey<Boolean> BLOCKED_BY_WATER =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/movement/blocked_by_water")).immutable());
    public static final PropertyKey<PerWorldData<Integer>> TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("movement/speed/tick_cooldown")).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> CRUISE_TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("movement/cruise/tick_cooldown")).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> VERT_TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/speed/vert_tick_cooldown"), t -> t.get(PropertyKeys.TICK_COOLDOWN).get() * 2
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> VERT_CRUISE_TICK_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/cruise/vert_tick_cooldown"), t -> t.get(PropertyKeys.CRUISE_TICK_COOLDOWN).get() * 2
            ).perWorld().immutable());
    public static final PropertyKey<Boolean> REQUIRE_WATER_CONTACT =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/require_water_contact")).immutable());
    public static final PropertyKey<Boolean> TRY_NUDGE =
            register(PropertyKeyTypes.boolPropertyKey(key("try_nudge")).immutable());
    // TODO: Change to be a multiplier based on height levels and/or biomes
    public static final PropertyKey<Boolean> HALF_SPEED_UNDERWATER =
            register(PropertyKeyTypes.boolPropertyKey(key("movement/speed/half_when_underwater")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_SPEED_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag/speed_factor")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_POWER_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag/power_factor")).immutable());
    public static final PropertyKey<Double> DYNAMIC_LAG_MIN_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("dynamic_lag/min_speed")).immutable());
    public static final PropertyKey<Double> DYNAMIC_FLY_BLOCK_SPEED_FACTOR =
            register(PropertyKeyTypes.doublePropertyKey(key("movement/dynamic_fly_blocks/block_speed_factor")).immutable());
    public static final PropertyKey<Double> CHEST_PENALTY =
            register(PropertyKeyTypes.doublePropertyKey(key("movement/chest_penalty")).immutable());
    // endregion idk if we should keep these

    public static final PropertyKey<List<String>> FORBIDDEN_SIGN_STRINGS =
            register(PropertyKeyTypes.stringListPropertyKey(key("constraints/forbidden_sign_strings")).immutable());
    public static final PropertyKey<Boolean> CAN_CRUISE =
            register(PropertyKeyTypes.boolPropertyKey(key("movement/cruise/enabled"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_TELEPORT =
            register(PropertyKeyTypes.boolPropertyKey(key("teleport/enabled"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_SWITCH_WORLD =
            register(PropertyKeyTypes.boolPropertyKey(key("general/can_switch_world"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_BE_NAMED =
            register(PropertyKeyTypes.boolPropertyKey(key("general/can_be_named"), t -> true).immutable());
    // TODO: replace behavior like this by directly specifying the craft class in the type
    public static final PropertyKey<Boolean> CRUISE_ON_PILOT =
            register(PropertyKeyTypes.boolPropertyKey(key("movement/cruise/on_pilot/enabled"), t -> false).immutable());
    // TODO: Replace with vector (left/right, up/down, forward/backward) or something
    public static final PropertyKey<Integer> CRUISE_ON_PILOT_VERT_MOVE =
            register(PropertyKeyTypes.intPropertyKey(key("movement/cruise/on_pilot/vert_move"), t -> 0).immutable());
    public static final PropertyKey<Boolean> ALLOW_VERTICAL_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/movement/allow_vertical"), t -> true).immutable());
    public static final PropertyKey<Boolean> ALLOW_HORIZONTAL_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/movement/allow_horizontal"), t -> true).immutable());
    public static final PropertyKey<Boolean> ROTATE_AT_MIDPOINT =
            register(PropertyKeyTypes.boolPropertyKey(key("general/rotate_at_midpoint"), t -> false).immutable());
    public static final PropertyKey<Boolean> ALLOW_REMOTE_SIGN =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/allow_remote_sign"), t -> true).immutable());
    public static final PropertyKey<Boolean> CAN_STATIC_MOVE =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/movement/static_move/enabled"), t -> false).immutable());
    public static final PropertyKey<Integer> MAX_STATIC_MOVE =
            register(PropertyKeyTypes.intPropertyKey(key("constraints/movement/static_move/max"), t -> 1).immutable());
    public static final PropertyKey<PerWorldData<Integer>> CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(key("movement/cruise/skip_blocks"), t -> 0).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> VERT_CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/cruise/vert_skip_blocks"), t -> t.get(CRUISE_SKIP_BLOCKS).get()
            ).perWorld().immutable());
    public static final PropertyKey<Boolean> FOCUSED_EXPLOSION =
            register(PropertyKeyTypes.boolPropertyKey(key("movement/collision/explosion/focused"), t -> false).immutable());
    public static final PropertyKey<Boolean> MUST_BE_SUBCRAFT =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/must_be_subcraft"), t -> false).immutable());
    public static final PropertyKey<Boolean> CAN_BE_SUBCRAFT =
            register(PropertyKeyTypes.boolPropertyKey(key("constraints/can_be_subcraft"), t -> true).immutable());
    // TODO: Remove, worlds have their own waterlevel which should be used instead
    public static final PropertyKey<Integer> STATIC_WATER_LEVEL =
            register(PropertyKeyTypes.intPropertyKey(key("static_water_level"), t -> 0).immutable());
    public static final PropertyKey<PerWorldData<Double>> FUEL_BURN_RATE =
            register(PropertyKeyTypes.doublePropertyKey(key("fuel/burn_rate"), t -> 0D).perWorld().immutable());
    public static final PropertyKey<Double> INACTIVE_FUEL_BURN_RATE =
            register(PropertyKeyTypes.doublePropertyKey(key("fuel/passive_burn_rate"), t -> 0D));
    public static final PropertyKey<Boolean> FURNACE_FUEL_VISUALIZATION =
            register(PropertyKeyTypes.boolPropertyKey(key("fuel/furnace_visualization"), t -> false));
    // TODO: Change to be part of the actual constraints themselves
    public static final PropertyKey<Double> SINK_PERCENT =
            register(PropertyKeyTypes.doublePropertyKey(key("sinking/constraint_intolerance"), t -> 0D).immutable());
    public static final PropertyKey<Double> OVERALL_SINK_PERCENT =
            register(PropertyKeyTypes.doublePropertyKey(key("sinking/lowest_possible_integrity"), t -> 0D).immutable());
    public static final PropertyKey<PerWorldData<Double>> DETECTION_MULTIPLIER =
            register(PropertyKeyTypes.doublePropertyKey(key("contacts/detection/multiplier"), t -> 0D).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> UNDERWATER_DETECTION_MULTIPLIER =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("contacts/detection/underwater_multiplier"), t -> t.get(DETECTION_MULTIPLIER).get()
            ).perWorld().immutable());
    public static final PropertyKey<ConfiguredSound> NEW_CONTACT_SOUND =
            register(PropertyKeyTypes.configuredSoundPropertyKey(
                    key("contacts/new_contact_sound"), "block.anvil.land", SoundCategory.NEUTRAL, 2.0F, 1.0F
            ));
    // TODO: Move to sinkhandler
    public static final PropertyKey<Double> SINK_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(key("sinking/speed"), t -> 1D).immutable());
    // TODO: Move to sinkhandler
    public static final PropertyKey<Integer> SINK_RATE_TICKS =
            register(PropertyKeyTypes.intPropertyKey(
            key("sinking/tickrate"), t -> (int) Math.ceil(20 / t.get(SINK_SPEED))
            ).immutable());
    // TODO: Move to sinkhandler
    public static final PropertyKey<Boolean> KEEP_MOVING_ON_SINK =
            register(PropertyKeyTypes.boolPropertyKey(key("sinking/keep_moving"), t -> false).immutable());
    // TODO: Change to per world
    // TODO: Incorporate into sinkhandler
    public static final PropertyKey<Boolean> SINK_WHEN_OUT_OF_FUEL =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("sinking/sink_on_out_of_fuel")
            ).immutable());
    // TODO: Move to sinkhandler
    public static final PropertyKey<Integer> SMOKE_ON_SINK =
            register(PropertyKeyTypes.intPropertyKey(key("sinking/smoke"), t -> 0).immutable());
    public static final PropertyKey<Float> EXPLODE_ON_CRASH =
            register(PropertyKeyTypes.floatPropertyKey(key("sinking/explosion/strength"), t -> 0F).immutable());
    public static final PropertyKey<Boolean> INCENDIARY_ON_CRASH =
            register(PropertyKeyTypes.boolPropertyKey(key("sinking/explosion/incendiary"), t -> false).immutable());
    public static final PropertyKey<Float> COLLISION_EXPLOSION =
            register(PropertyKeyTypes.floatPropertyKey(key("movement/collision/explosion/strength"), t -> 0F).immutable());
    public static final PropertyKey<Float> UNDERWATER_COLLISION_EXPLOSION =
            register(PropertyKeyTypes.floatPropertyKey(
                    key("movement/collision/explosion/underwater_strength"), t -> t.get(COLLISION_EXPLOSION)
            ).immutable());
    public static final PropertyKey<PerWorldData<Integer>> MIN_HEIGHT_LIMIT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("constraints/movement/height_limit/min"), t -> Integer.MIN_VALUE
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> CRUISE_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("movement/cruise/speed"), t -> t.get(SPEED).get()
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Double>> VERT_CRUISE_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("movement/cruise/vert_speed"), t -> t.get(CRUISE_SPEED).get()
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> MAX_HEIGHT_LIMIT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("constraints/movement/height_limit/max"), t -> Integer.MAX_VALUE
            ).perWorld().immutable());
    public static final PropertyKey<PerWorldData<Integer>> MAX_HEIGHT_ABOVE_GROUND =
            register(PropertyKeyTypes.intPropertyKey(
                    key("constraints/movement/max_ground_distance"), t -> -1
            ).perWorld().immutable());
    public static final PropertyKey<Boolean> CAN_DIRECT_CONTROL =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("movement/can_direct_control"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> CAN_HOVER =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("constraints/movement/hover/enabled"), t -> false
            ).immutable());
    // TODO: make per world
    public static final PropertyKey<Integer> HOVER_LIMIT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("constraints/movement/hover/limit"), 0
            ).immutable());
    // TODO: Replace with a "allowed hover over" list or blacklist
    public static final PropertyKey<Boolean> CAN_HOVER_OVER_WATER =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("constraints/movement/hover/can_hover_over_water"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> CAN_MOVE_ENTITIES =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("movement/entities/enabled"), t -> true
            ).immutable());
    // TODO: Replace with black/whitelist of entity types
    public static final PropertyKey<Boolean> ONLY_MOVE_PLAYERS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("movement/entities/only_players"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> USE_GRAVITY =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("movement/gravity/enabled"), t -> false
            ).immutable());
    public static final PropertyKey<Boolean> ALLOW_VERTICAL_TAKEOFF_AND_LANDING =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("constraints/movement/allow_vertical_takeoff_and_landing"), t -> true
            ).immutable());
    public static final PropertyKey<Integer> GRAVITY_INCLINE_DISTANCE =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/gravity/incline_distance"), t -> -1
            ).immutable());
    // TODO: Make perWorld property?
    public static final PropertyKey<Integer> GRAVITY_DROP_DISTANCE =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/gravity/drop_distance"), t -> -8
            ).immutable());
    public static final PropertyKey<ConfiguredSound> COLLISION_SOUND =
            register(PropertyKeyTypes.configuredSoundPropertyKey(
                    key("movement/collision/sound"), "block.anvil.land", SoundCategory.NEUTRAL, 2.0F, 1.0F
            ).immutable());
    public static final PropertyKey<NamespacedKeyToDoubleProperty> FUEL_TYPES =
            register(PropertyKeyTypes.namespacedKeyToDoublePropertyKey(
                    key("fuel/types"), Map.of(
                            BlockType.COAL_BLOCK.getKey(), 80.0D,
                            ItemType.COAL.getKey(), 8.0D,
                            ItemType.CHARCOAL.getKey(), 8.0D
                    )
            ).immutable());
    // Modified from original
    public static final PropertyKey<List<String>> DISABLE_TELEPORT_TO_WORLDS =
            register(PropertyKeyTypes.stringListPropertyKey(key("teleport/disabled_destination_worlds")).immutable());
    public static final PropertyKey<Integer> TELEPORTATION_COOLDOWN =
            register(PropertyKeyTypes.intPropertyKey(key("teleport/cooldown")).immutable());
    public static final PropertyKey<Integer> GEAR_SHIFTS =
            register(PropertyKeyTypes.intPropertyKey(key("gear_shifts/count"), t -> 1).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_TICK_COOLDOWN =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shifts/modify/tick_cooldown"), t -> true
            ).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_DIRECT_MOVEMENT =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shifts/modify/direct_movement"), t -> false
            ).immutable());
    public static final PropertyKey<Boolean> GEAR_SHIFT_AFFECT_CRUISE_SKIP_BLOCKS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("gear_shifts/modify/cruise_skip_blocks"), t -> false
            ).immutable());
    public static final PropertyKey<Integer> RELEASE_TIMEOUT =
            register(PropertyKeyTypes.intPropertyKey(
                    key("general/release_timeout"), t -> 30
            ).immutable());
    public static final PropertyKey<Boolean> MERGE_PISTON_EXTENSIONS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("general/merge_piston_extensions"), t -> false
            ).immutable());
    public static final PropertyKey<Integer> CRUISE_ON_PILOT_LIFETIME =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/cruise/on_pilot/lifetime"), t -> 15*20
            ).immutable());
    public static final PropertyKey<Integer> EXPLOSION_ARMING_TIME =
            register(PropertyKeyTypes.intPropertyKey(
                    key("movement/collision/explosion/arming_time"), t -> 1000
            ).immutable());
    public static final PropertyKey<Boolean> ALLOW_INTERNAL_EXPLOSION =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("movement/collision/explosion/allow_internal"), t -> false
            ).immutable());

    // region speed modifier blocks
    public static final PropertyKey<PerWorldData<Double>> SPEED_MODIFIER_MAX_SPEED =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("movement/speed/modifiers/max_speed")
            ).perWorld().immutable());
    public static final PropertyKey<NamespacedKeyToDoubleProperty> SPEED_MODIFIER_BLOCKS =
            register(PropertyKeyTypes.namespacedKeyToDoublePropertyKey(
                    key("movement/speed/modifiers/blocks")
            ).immutable());
    // endregion speed modifier blocks

    // region Alternative sinking process
    // TODO: Replace sinking logic by configuring a sinking handler which creates the task and so on
    public static final PropertyKey<Double> FALL_OUT_OF_WORLD_BLOCK_CHANCE =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("sinking/falling_block_chance")
            ).immutable());
    public static final PropertyKey<Boolean> USE_ALTERNATIVE_SINKING_PROCESS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("sinking/pretty/enabled")
            ).immutable());
    public static final PropertyKey<Integer> ALTERNATIVE_SINKING_TIME_BEFORE_DISINITEGRATION =
            register(PropertyKeyTypes.intPropertyKey(
                    key("sinking/pretty/disintegration/delay")
            ).immutable());
    public static final PropertyKey<Double> ALTERNATIVE_SINKING_EXPLOSION_CHANCE =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("sinking/pretty/explosion/chance")
            ).immutable());
    public static final PropertyKey<Integer> ALTERNATIVE_SINKING_MIN_EXPLOSIONS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("sinking/pretty/explosion/min"), 1
            ).immutable());
    public static final PropertyKey<Integer> ALTERNATIVE_SINKING_MAX_EXPLOSIONS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("sinking/pretty/explosion/max"), 4
            ).immutable());
    public static final PropertyKey<Double> ALTERNATIVE_SINKING_DISINTEGRATION_CHANCE =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("sinking/pretty/disintegration/chance"), 0.5D
            ).immutable());
    public static final PropertyKey<Integer> ALTERNATIVE_SINKING_MIN_DISINTEGRATE_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("sinking/pretty/disintegration/min"), 25
            ).immutable());
    public static final PropertyKey<Integer> ALTERNATIVE_SINKING_MAX_DISINTEGRATE_BLOCKS =
            register(PropertyKeyTypes.intPropertyKey(
                    key("sinking/pretty/disintegration/max"), 100
            ).immutable());
    public static final PropertyKey<String> ALTERNATIVE_SINKING_DISINTEGRATION_SOUND =
            register(PropertyKeyTypes.stringPropertyKey(
                    key("sinking/pretty/disintegration/sound"), ""
            ).immutable());
    public static final PropertyKey<Double> ALTERNATIVE_SINKING_SINK_MAX_REMAINING_PERCENTAGE =
            register(PropertyKeyTypes.doublePropertyKey(
                    key("sinking/pretty/max_remaining_percentage"), 0.25D
            ).immutable());
    // endregion Alternative sinking process

    // region disabled stuff
    public static final PropertyKey<Boolean> ALLOW_BLOCK_BREAKING_WHEN_DISABLED =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("disabled_state/allow_block_breaking_when_disabled"), true
            ).immutable());
    public static final PropertyKey<Boolean> REQUIRE_DISABLED_TO_BREAK_BLOCKS =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("disabled_state/require_disabled_to_break_blocks"), true
            ).immutable());
    public static final PropertyKey<Boolean> CAN_BE_UN_DISABLED =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("disabled_state/can_be_un_disabled"), true
            ).immutable());
    // endregion disabled stuff

    // region movement controllers
    public static final PropertyKey<AbstractRotationController> ROTATION_CONTROLLER =
            register(
                    new PropertyKey<AbstractRotationController>(
                            key("movement/controller/rotation"),
                            type -> new DefaultRotationController(),
                            (obj, type) -> {
                                if (obj != null && (obj instanceof AbstractRotationController)) {
                                    // AbstractRotationController is serializable!
                                    return (AbstractRotationController)obj;
                                }
                                return new DefaultRotationController();
                            },
                            (s) -> s,
                            AbstractRotationController::clone
                    )
            );
    // endregion movement controllers
    // region serialization
    public static final PropertyKey<PerWorldData<Boolean>> SAVE_TO_DISK =
            register(PropertyKeyTypes.boolPropertyKey(
                    key("serialization/save_to_disk"), false
            )).perWorld();
    // endregion serialization

    public static <T> PropertyKey<T> register(PropertyKey<T> propertyKey) {
        return TypeSafeCraftType.PROPERTY_REGISTRY.register(propertyKey.key(), propertyKey);
    }

    private static NamespacedKey key(String path) {
        return new NamespacedKey("movecraft", path);
    }

    static void registerAll() {

    }

}
