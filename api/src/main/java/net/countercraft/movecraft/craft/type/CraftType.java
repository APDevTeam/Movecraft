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

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.property.*;
import net.countercraft.movecraft.craft.type.transform.*;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.registration.TypedKey;
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
import java.util.function.Predicate;

// Use TypeSafeCraftType and CraftProperties instead!
@Deprecated(forRemoval = true)
final public class CraftType extends TypeSafeCraftType {
    //region Property Keys
    public static final NamespacedKey NAME = PropertyKeys.NAME.key();
    public static final NamespacedKey MAX_SIZE = PropertyKeys.MAX_SIZE.key();
    public static final NamespacedKey MIN_SIZE = PropertyKeys.MIN_SIZE.key();
    public static final NamespacedKey ALLOWED_BLOCKS = PropertyKeys.ALLOWED_BLOCKS.key();
    private static final NamespacedKey SPEED = PropertyKeys.SPEED.key();
        // Private key used to calculate TICK_COOLDOWN
    private static final NamespacedKey TICK_COOLDOWN = PropertyKeys.TICK_COOLDOWN.key();
        // Private key used as default for PER_WORLD_TICK_COOLDOWN
    public static final NamespacedKey FLY_BLOCKS = PropertyKeys.FLY_BLOCKS.key();
    public static final NamespacedKey DETECTION_BLOCKS = PropertyKeys.DETECTION_BLOCKS.key();
    public static final NamespacedKey FORBIDDEN_SIGN_STRINGS = PropertyKeys.FORBIDDEN_SIGN_STRINGS.key();
    private static final NamespacedKey PER_WORLD_SPEED = PropertyKeys.SPEED.key();
        // Private key used to calculate PER_WORLD_TICK_COOLDOWN
    public static final NamespacedKey PER_WORLD_TICK_COOLDOWN = PropertyKeys.TICK_COOLDOWN.key();
    public static final NamespacedKey FORBIDDEN_BLOCKS = PropertyKeys.FORBIDDEN_BLOCKS.key();
    public static final NamespacedKey BLOCKED_BY_WATER = PropertyKeys.BLOCKED_BY_WATER.key();
    private static final NamespacedKey CAN_FLY = PropertyKeys.CAN_FLY.key();
        // Private key used to calculate BLOCKED_BY_WATER
    @Deprecated(forRemoval = true)
    /*
     * Use contact blocks and traction blocks instead!
     */
    public static final NamespacedKey REQUIRE_WATER_CONTACT = PropertyKeys.REQUIRE_WATER_CONTACT.key();
    public static final NamespacedKey REQUIRED_CONTACT_BLOCKS = PropertyKeys.REQUIRED_CONTACT_BLOCKS.key();
    public static final NamespacedKey TRACTION_BLOCKS = PropertyKeys.TRACTION_BLOCKS.key();
    public static final NamespacedKey TRY_NUDGE = PropertyKeys.TRY_NUDGE.key();
    public static final NamespacedKey MOVE_BLOCKS = PropertyKeys.MOVE_BLOCKS.key();
    public static final NamespacedKey CAN_CRUISE = PropertyKeys.CAN_CRUISE.key();
    public static final NamespacedKey CAN_TELEPORT = PropertyKeys.CAN_TELEPORT.key();
    public static final NamespacedKey CAN_SWITCH_WORLD = PropertyKeys.CAN_SWITCH_WORLD.key();
    public static final NamespacedKey CAN_BE_NAMED = PropertyKeys.CAN_BE_NAMED.key();
    public static final NamespacedKey CRUISE_ON_PILOT = PropertyKeys.CRUISE_ON_PILOT.key();
    public static final NamespacedKey CRUISE_ON_PILOT_VERT_MOVE = PropertyKeys.CRUISE_ON_PILOT_VERT_MOVE.key();
    public static final NamespacedKey ALLOW_VERTICAL_MOVEMENT = PropertyKeys.ALLOW_VERTICAL_MOVEMENT.key();
    public static final NamespacedKey ROTATE_AT_MIDPOINT = PropertyKeys.ROTATE_AT_MIDPOINT.key();
    public static final NamespacedKey ALLOW_HORIZONTAL_MOVEMENT = PropertyKeys.ALLOW_HORIZONTAL_MOVEMENT.key();
    public static final NamespacedKey ALLOW_REMOTE_SIGN = PropertyKeys.ALLOW_REMOTE_SIGN.key();
    public static final NamespacedKey CAN_STATIC_MOVE = PropertyKeys.CAN_STATIC_MOVE.key();
    public static final NamespacedKey MAX_STATIC_MOVE = PropertyKeys.MAX_STATIC_MOVE.key();
    private static final NamespacedKey CRUISE_SKIP_BLOCKS = PropertyKeys.CRUISE_SKIP_BLOCKS.key();
        // Private key used as default for PER_WORLD_CRUISE_SKIP_BLOCKS
    public static final NamespacedKey PER_WORLD_CRUISE_SKIP_BLOCKS = PropertyKeys.CRUISE_SKIP_BLOCKS.key();
    private static final NamespacedKey VERT_CRUISE_SKIP_BLOCKS = PropertyKeys.VERT_CRUISE_SKIP_BLOCKS.key();
        // Private key used as default for PER_WORLD_VERT_CRUISE_SKIP_BLOCKS
    public static final NamespacedKey PER_WORLD_VERT_CRUISE_SKIP_BLOCKS = PropertyKeys.VERT_CRUISE_SKIP_BLOCKS.key();
    public static final NamespacedKey HALF_SPEED_UNDERWATER = PropertyKeys.HALF_SPEED_UNDERWATER.key();
    public static final NamespacedKey FOCUSED_EXPLOSION = PropertyKeys.FOCUSED_EXPLOSION.key();
    public static final NamespacedKey MUST_BE_SUBCRAFT = PropertyKeys.MUST_BE_SUBCRAFT.key();
    public static final NamespacedKey STATIC_WATER_LEVEL = PropertyKeys.STATIC_WATER_LEVEL.key();
    private static final NamespacedKey FUEL_BURN_RATE = PropertyKeys.FUEL_BURN_RATE.key();
        // Private key used as default for PER_WORLD_FUEL_BURN_RATE
    public static final NamespacedKey PER_WORLD_FUEL_BURN_RATE = PropertyKeys.FUEL_BURN_RATE.key();
    public static final NamespacedKey SINK_PERCENT = PropertyKeys.SINK_PERCENT.key();
    public static final NamespacedKey OVERALL_SINK_PERCENT = PropertyKeys.OVERALL_SINK_PERCENT.key();
    private static final NamespacedKey DETECTION_MULTIPLIER = PropertyKeys.DETECTION_MULTIPLIER.key();
        // Private key used as default for PER_WORLD_DETECTION_MULTIPLIER
    public static final NamespacedKey PER_WORLD_DETECTION_MULTIPLIER = PropertyKeys.DETECTION_MULTIPLIER.key();
    private static final NamespacedKey UNDERWATER_DETECTION_MULTIPLIER = PropertyKeys.UNDERWATER_DETECTION_MULTIPLIER.key();
        // Private key used as default for PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER
    public static final NamespacedKey PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER = PropertyKeys.UNDERWATER_DETECTION_MULTIPLIER.key();
    private static final NamespacedKey SINK_SPEED = PropertyKeys.SINK_SPEED.key();
        // Private key used to calculate SINK_RATE_TICKS
    public static final NamespacedKey SINK_RATE_TICKS = PropertyKeys.SINK_RATE_TICKS.key();
    public static final NamespacedKey KEEP_MOVING_ON_SINK = PropertyKeys.KEEP_MOVING_ON_SINK.key();
    public static final NamespacedKey SMOKE_ON_SINK = PropertyKeys.SMOKE_ON_SINK.key();
    public static final NamespacedKey EXPLODE_ON_CRASH = PropertyKeys.EXPLODE_ON_CRASH.key();
    public static final NamespacedKey INCENDIARY_ON_CRASH = PropertyKeys.INCENDIARY_ON_CRASH.key();
    public static final NamespacedKey COLLISION_EXPLOSION = PropertyKeys.COLLISION_EXPLOSION.key();
    public static final NamespacedKey UNDERWATER_COLLISION_EXPLOSION = PropertyKeys.UNDERWATER_COLLISION_EXPLOSION.key();
    private static final NamespacedKey MIN_HEIGHT_LIMIT = PropertyKeys.MIN_HEIGHT_LIMIT.key();
        // Private key used as default for PER_WORLD_MIN_HEIGHT_LIMIT
    public static final NamespacedKey PER_WORLD_MIN_HEIGHT_LIMIT = PropertyKeys.MIN_HEIGHT_LIMIT.key();
    private static final NamespacedKey CRUISE_SPEED = PropertyKeys.CRUISE_SPEED.key();
        // Private key used to calculate CRUISE_TICK_COOLDOWN
    private static final NamespacedKey CRUISE_TICK_COOLDOWN = PropertyKeys.CRUISE_TICK_COOLDOWN.key();
        // Private key used as default for PER_WORLD_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey PER_WORLD_CRUISE_SPEED = PropertyKeys.CRUISE_SPEED.key();
        // Private key used to calculate PER_WORLD_CRUISE_TICK_COOLDOWN
    public static final NamespacedKey PER_WORLD_CRUISE_TICK_COOLDOWN = PropertyKeys.CRUISE_TICK_COOLDOWN.key();
    private static final NamespacedKey VERT_CRUISE_SPEED = PropertyKeys.VERT_CRUISE_SPEED.key();
        // Private key used to calculate VERT_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey VERT_CRUISE_TICK_COOLDOWN = PropertyKeys.VERT_CRUISE_TICK_COOLDOWN.key();
        // Private key used as default for PER_WORLD_VERT_CRUISE_TICK_COOLDOWN
    private static final NamespacedKey PER_WORLD_VERT_CRUISE_SPEED = PropertyKeys.VERT_CRUISE_SPEED.key();
        // Private key used to calculate PER_WORLD_VERT_CRUISE_SPEED
    public static final NamespacedKey PER_WORLD_VERT_CRUISE_TICK_COOLDOWN = PropertyKeys.VERT_CRUISE_TICK_COOLDOWN.key();
    private static final NamespacedKey MAX_HEIGHT_LIMIT = PropertyKeys.MAX_HEIGHT_LIMIT.key();
        // Private key used as default for PER_WORLD_MAX_HEIGHT_LIMIT
    public static final NamespacedKey PER_WORLD_MAX_HEIGHT_LIMIT = PropertyKeys.MAX_HEIGHT_LIMIT.key();
    private static final NamespacedKey MAX_HEIGHT_ABOVE_GROUND = PropertyKeys.MAX_HEIGHT_ABOVE_GROUND.key();
        // Private key used as default for PER_WORLD_MAX_HEIGHT_ABOVE_GROUND
    public static final NamespacedKey PER_WORLD_MAX_HEIGHT_ABOVE_GROUND = PropertyKeys.MAX_HEIGHT_ABOVE_GROUND.key();
    public static final NamespacedKey CAN_DIRECT_CONTROL = PropertyKeys.CAN_DIRECT_CONTROL.key();
    public static final NamespacedKey CAN_HOVER = PropertyKeys.CAN_HOVER.key();
    public static final NamespacedKey CAN_HOVER_OVER_WATER = PropertyKeys.CAN_HOVER_OVER_WATER.key();
    public static final NamespacedKey MOVE_ENTITIES = PropertyKeys.CAN_MOVE_ENTITIES.key();
    public static final NamespacedKey ONLY_MOVE_PLAYERS = PropertyKeys.ONLY_MOVE_PLAYERS.key();
    public static final NamespacedKey USE_GRAVITY = PropertyKeys.USE_GRAVITY.key();
    public static final NamespacedKey HOVER_LIMIT = PropertyKeys.HOVER_LIMIT.key();
    public static final NamespacedKey HARVEST_BLOCKS = PropertyKeys.HARVEST_BLOCKS.key();
    public static final NamespacedKey HARVESTER_BLADE_BLOCKS = PropertyKeys.HARVESTER_BLADE_BLOCKS.key();
    public static final NamespacedKey PASSTHROUGH_BLOCKS = PropertyKeys.PASSTHROUGH_BLOCKS.key();
    public static final NamespacedKey FORBIDDEN_HOVER_OVER_BLOCKS = PropertyKeys.FORBIDDEN_HOVER_OVER_BLOCKS.key();
    public static final NamespacedKey ALLOW_VERTICAL_TAKEOFF_AND_LANDING = PropertyKeys.ALLOW_VERTICAL_TAKEOFF_AND_LANDING.key();
    public static final NamespacedKey DYNAMIC_LAG_SPEED_FACTOR = PropertyKeys.DYNAMIC_LAG_SPEED_FACTOR.key();
    public static final NamespacedKey DYNAMIC_LAG_POWER_FACTOR = PropertyKeys.DYNAMIC_LAG_POWER_FACTOR.key();
    public static final NamespacedKey DYNAMIC_LAG_MIN_SPEED = PropertyKeys.DYNAMIC_LAG_MIN_SPEED.key();
    public static final NamespacedKey DYNAMIC_FLY_BLOCK_SPEED_FACTOR = PropertyKeys.DYNAMIC_FLY_BLOCK_SPEED_FACTOR.key();
    public static final NamespacedKey DYNAMIC_FLY_BLOCK = PropertyKeys.DYNAMIC_FLY_BLOCKS.key();
    private static final NamespacedKey SPEED_MODIFIER_MAX_SPEED = PropertyKeys.SPEED_MODIFIER_MAX_SPEED.key();
    public static final NamespacedKey PER_WORLD_MODIFIER_MAX_SPEED = PropertyKeys.SPEED_MODIFIER_MAX_SPEED.key();
    public static final NamespacedKey SPEED_MODIFIER_BLOCKS = PropertyKeys.SPEED_MODIFIER_BLOCKS.key();
    public static final NamespacedKey CHEST_PENALTY = PropertyKeys.CHEST_PENALTY.key();
    public static final NamespacedKey GRAVITY_INCLINE_DISTANCE = PropertyKeys.GRAVITY_INCLINE_DISTANCE.key();
    public static final NamespacedKey GRAVITY_DROP_DISTANCE = PropertyKeys.GRAVITY_DROP_DISTANCE.key();
    public static final NamespacedKey COLLISION_SOUND = PropertyKeys.COLLISION_SOUND.key();
    public static final NamespacedKey FUEL_TYPES = PropertyKeys.FUEL_TYPES.key();
    public static final NamespacedKey SINK_WHEN_OUT_OF_FUEL = PropertyKeys.SINK_WHEN_OUT_OF_FUEL.key();
    public static final NamespacedKey DISABLE_TELEPORT_TO_WORLDS = PropertyKeys.DISABLE_TELEPORT_TO_WORLDS.key();
    public static final NamespacedKey TELEPORTATION_COOLDOWN = PropertyKeys.TELEPORTATION_COOLDOWN.key();
    public static final NamespacedKey GEAR_SHIFTS = PropertyKeys.GEAR_SHIFTS.key();
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_TICK_COOLDOWN = PropertyKeys.GEAR_SHIFT_AFFECT_TICK_COOLDOWN.key();
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT = PropertyKeys.GEAR_SHIFT_AFFECT_DIRECT_MOVEMENT.key();
    public static final NamespacedKey GEAR_SHIFTS_AFFECT_CRUISE_SKIP_BLOCKS = PropertyKeys.GEAR_SHIFT_AFFECT_AFFECT_CRUISE_SKIP_BLOCKS.key();
    public static final NamespacedKey RELEASE_TIMEOUT = PropertyKeys.RELEASE_TIMEOUT.key();
    public static final NamespacedKey MERGE_PISTON_EXTENSIONS = PropertyKeys.MERGE_PISTON_EXTENSIONS.key();

    public static final NamespacedKey CRUISE_ON_PILOT_LIFETIME = PropertyKeys.CRUISE_ON_PILOT_LIFETIME.key();

    public static final NamespacedKey EXPLOSION_ARMING_TIME = PropertyKeys.EXPLOSION_ARMING_TIME.key();
    public static final NamespacedKey DIRECTIONAL_DEPENDENT_MATERIALS = PropertyKeys.DIRECTIONAL_DEPENDENT_MATERIALS.key();
    public static final NamespacedKey ALLOW_INTERNAL_COLLISION_EXPLOSION = PropertyKeys.ALLOW_INTERNAL_EXPLOSION.key();

    public static final NamespacedKey MOVE_BREAK_BLOCKS = PropertyKeys.MOVE_BREAK_BLOCKS.key();

    public static final NamespacedKey FALL_OUT_OF_WORLD_BLOCK_CHANCE = PropertyKeys.FALL_OUT_OF_WORLD_BLOCK_CHANCE.key();

    // TODO: Create a explosion property => min, max power, incendiary, etc
    public static final NamespacedKey USE_ALTERNATIVE_SINKING_PROCESS = PropertyKeys.USE_ALTERNATIVE_SINKING_PROCESS.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_TIME_BEFORE_DISINTEGRATION = PropertyKeys.ALTERNATIVE_SINKING_TIME_BEFORE_DISINITEGRATION.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_MIN_DISINTEGRATE_BLOCKS = PropertyKeys.ALTERNATIVE_SINKING_MIN_DISINTEGRATE_BLOCKS.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_MAX_DISINTEGRATE_BLOCKS = PropertyKeys.ALTERNATIVE_SINKING_MAX_DISINTEGRATE_BLOCKS.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_MIN_EXPLOSIONS = PropertyKeys.ALTERNATIVE_SINKING_MIN_EXPLOSIONS.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_MAX_EXPLOSIONS = PropertyKeys.ALTERNATIVE_SINKING_MAX_EXPLOSIONS.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_EXPLOSION_CHANCE = PropertyKeys.ALTERNATIVE_SINKING_EXPLOSION_CHANCE.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_DISINTEGRATION_SOUND = PropertyKeys.ALTERNATIVE_SINKING_DISINTEGRATION_SOUND.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_DISINTEGRATION_CHANCE = PropertyKeys.ALTERNATIVE_SINKING_DISINTEGRATION_CHANCE.key();
    public static final NamespacedKey ALTERNATIVE_SINKING_SINK_MAX_REMAINING_PERCENTAGE = PropertyKeys.ALTERNATIVE_SINKING_SINK_MAX_REMAINING_PERCENTAGE.key();
    //endregion

    @Contract("_ -> new")
    private static @NotNull NamespacedKey buildKey(String key) {
        return new NamespacedKey("movecraft", key);
    }



    static final Map<NamespacedKey, Property<?>> properties = new HashMap<>();

    /**
     * Register a property with Movecraft
     *
     * @param property property to register
     */
    @Deprecated(forRemoval = true)
    public static void registerProperty(Property<?> property) {
        //properties.put(property.getNamespacedKey(), property);
        PropertyKey<?> propertyKey = property.asTypeSafeKey();
        TypeSafeCraftType.PROPERTY_REGISTRY.register(propertyKey.key(), propertyKey, false);
    }

    private final TypeSafeCraftType backing;

    private Map<NamespacedKey, String> stringPropertyMap;
    /**
     * Get a string property of this CraftType
     *
     * @param key Key of the string property
     * @return value of the string property
     */
    @Deprecated(forRemoval = true)
    public String getStringProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                String result = this.backing.get((PropertyKey<String>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a string property!");
            }
        } else {
            throw new IllegalStateException("String property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, Integer> intPropertyMap;
    /**
     * Get an integer property of this CraftType
     *
     * @param key Key of the integer property
     * @return value of the integer property
     */
    @Deprecated(forRemoval = true)
    public int getIntProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Integer result = this.backing.get((PropertyKey<Integer>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a int property!");
            }
        } else {
            throw new IllegalStateException("Int property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, Boolean> boolPropertyMap;
    /**
     * Get a boolean property of this CraftType
     *
     * @param key Key of the boolean property
     * @return value of the boolean property
     */
    @Deprecated(forRemoval = true)
    public boolean getBoolProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Boolean result = this.backing.get((PropertyKey<Boolean>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a boolean property!");
            }
        } else {
            throw new IllegalStateException("Boolean property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, Float> floatPropertyMap;
    /**
     * Get a float property of this CraftType
     *
     * @param key Key of the float property
     * @return value of the float property
     */
    @Deprecated(forRemoval = true)
    public float getFloatProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Float result = this.backing.get((PropertyKey<Float>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a float property!");
            }
        } else {
            throw new IllegalStateException("Float property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, Double> doublePropertyMap;
    /**
     * Get a double property of this CraftType
     *
     * @param key Key of the double property
     * @return value of the double property
     */
    @Deprecated(forRemoval = true)
    public double getDoubleProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Double result = this.backing.get((PropertyKey<Double>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a double property!");
            }
        } else {
            throw new IllegalStateException("Double property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, Object> objectPropertyMap;
    /**
     * Get an object property of this CraftType
     * Note: Object properties have no type safety, it is expected that the addon developer handle type safety
     *
     * @param key Key of the object property
     * @return value of the object property
     */
    @Deprecated(forRemoval = true)
    @Nullable
    public Object getObjectProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Object result = this.backing.get((PropertyKey<? extends Object>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a object property!");
            }
        } else {
            throw new IllegalStateException("Object property <" + key + "> is not registered!");
        }
    }

    private Map<NamespacedKey, EnumSet<Material>> materialSetPropertyMap;
    /**
     * Get a material set property of this CraftType
     *
     * @param key Key of the material set property
     * @return value of the material set property
     */
    @Deprecated(forRemoval = true)
    public EnumSet<Material> getMaterialSetProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Object result = this.backing.get((PropertyKey<? extends Object>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                if (result != null) {
                    if (result instanceof BlockSetProperty blockSetProperty) {
                        return blockSetProperty.get();
                    } else
                    if (result instanceof EnumSet matSet) {
                        if (matSet.isEmpty()) {
                            return matSet;
                        } else {
                            Object first = matSet.toArray()[0];
                            if (first instanceof Material) {
                                return (EnumSet<Material>) result;
                            }
                        }
                    }
                }
                throw new RuntimeException();
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a materialset property!");
            }
        } else {
            throw new IllegalStateException("MaterialSet property <" + key + "> is not registered!");
        }
    }

    Map<NamespacedKey, Pair<Map<String, Object>, BiFunction<CraftType, String, Object>>> perWorldPropertyMap;
    @Deprecated(forRemoval = true)
    private Object getPerWorldProperty(NamespacedKey key, String worldName) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Object returnData = this.backing.get(TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                if (returnData instanceof PerWorldData<? extends Object> pwd) {
                    returnData = pwd.get(worldName);
                }
                return returnData;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a per world property!");
            }
        } else {
            throw new IllegalStateException("Per-world property <" + key + "> is not registered!");
        }
    }
    /**
     * Get a per world property of this CraftType
     *
     * @param key Key of the per world property
     * @param world the world to check
     * @return value of the per world property
     */
    @Deprecated(forRemoval = true)
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
    @Deprecated(forRemoval = true)
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
    @Deprecated(forRemoval = true)
    public Set<RequiredBlockEntry> getRequiredBlockProperty(NamespacedKey key) {
        if (TypeSafeCraftType.PROPERTY_REGISTRY.isRegistered(key)) {
            try {
                Set<RequiredBlockEntry> result = this.backing.get((PropertyKey<Set<RequiredBlockEntry>>)TypeSafeCraftType.PROPERTY_REGISTRY.get(key));
                return result;
            } catch(Exception exception) {
                throw new IllegalStateException("Property <" + key + "> is not a requiredBlockEntry property!");
            }
        } else {
            throw new IllegalStateException("RequiredBlockEntry property <" + key + "> is not registered!");
        }
    }



    private static final List<Transform<?>> transforms = new ArrayList<>();

    /**
     * Register a craft type transform
     *
     * @param transform transform to modify the craft type
     */
    @Deprecated(forRemoval = true)
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
    @Deprecated(forRemoval = true)
    public static void registerTypeValidator(final Predicate<CraftType> validator, String errorMessage) {
        Predicate<TypeSafeCraftType> typeSafeCraftTypePredicate = (typeSafe) -> {
            if (typeSafe instanceof CraftType craftType) {
                return validator.test(craftType);
            } else {
                return validator.test(new CraftType(typeSafe));
            }
        };
        TypeSafeCraftType.VALIDATOR_REGISTRY.add(new Pair<>(typeSafeCraftTypePredicate, errorMessage));
    }



    static {
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

    public CraftType(final TypeSafeCraftType backing) {
        super(backing.getName(), backing.typeRetriever);
        this.backing = backing;
    }

    public static class TypeNotFoundException extends RuntimeException {
        public TypeNotFoundException(String s) {
            super(s);
        }
    }

    @Override
    protected <T> void set(@NotNull TypedKey<T> tagKey, @NotNull T value) {
        this.backing.set(tagKey, value);
    }

    @Override
    protected <T> T getWithoutParent(@NotNull PropertyKey<T> key) {
        return this.backing.getWithoutParent(key);
    }

    @Override
    public <T> T get(@NotNull PropertyKey<T> key) {
        return this.backing.get(key);
    }

    @Override
    protected <T> T get(@NotNull PropertyKey<T> key, TypeSafeCraftType type) {
        return this.backing.get(key, type);
    }

    @Override
    public CraftProperties createCraftProperties(final Craft craft) {
        return this.backing.createCraftProperties(craft);
    }

    @Override
    Set<Map.Entry<PropertyKey<?>, Object>> entrySet() {
        return this.backing.entrySet();
    }

    @Override
    public <T> boolean hasInSelfOrAnyParent(PropertyKey<T> key) {
        return this.backing.hasInSelfOrAnyParent(key);
    }

    @Override
    public String getName() {
        return this.backing.getName();
    }
}
