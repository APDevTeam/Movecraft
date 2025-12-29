package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class Validators {

    public static Pair<Predicate<TypeSafeCraftType>, String> register(Pair<Predicate<TypeSafeCraftType>, String> validator) {
        if (!TypeSafeCraftType.VALIDATOR_REGISTRY.add(validator)) {
            throw new IllegalStateException("No duplicate validators allowed!");
        }
        return validator;
    }

    static void register(Predicate<TypeSafeCraftType> predicate, String errorMessage) {
        register(new Pair<>(predicate, errorMessage));
    }

    static void registerAll() {
        // Validator to avoid parent recursions!
        register(
                type -> {
                    TypeSafeCraftType tmp = type;
                    if (tmp.getParent() != null) {
                        do {
                            tmp = tmp.getParent();
                            if (tmp == type) {
                                return false;
                            }
                        } while(tmp != null);
                    }
                    return true;
                },
                "Type must not be used as parent in its own parent hierarchy!"
        );
        // Validator for min and max values
        register(
                new PerWorldMinMaxValidator<Integer>(PropertyKeys.MIN_HEIGHT_LIMIT, PropertyKeys.MAX_HEIGHT_LIMIT),
                "Settings for min and max height limit are not correct! Check all min values to be smaller than the max values! Per world overrides must obey this rule too!"
        );
        // Min / max size validation
        register(
                new MinMaxValidator<Integer>(PropertyKeys.MIN_SIZE, PropertyKeys.MAX_SIZE),
                "Min/Max size is invalid!"
        );
        // Hover limit validation
        register(
                type -> type.get(PropertyKeys.HOVER_LIMIT) >= 0,
                "hoverLimit must be greater than or equal to zero"
        );
        // Gears
        register(
                type -> type.get(PropertyKeys.GEAR_SHIFTS) >= 1,
                "gearShifts must be greater than or equal to one"
        );
        // Height limits against world properties
        register(
                type -> {
                    PerWorldData<Integer> worldData = type.get(PropertyKeys.MIN_HEIGHT_LIMIT);
                    for (Map.Entry<String, Integer> entry : worldData.getOverrides().entrySet()) {
                        World world = Bukkit.getWorld(entry.getKey());
                        // World could not be loaded yet, ignore those!
                        if (world != null) {
                            if (entry.getValue() < world.getMinHeight() || entry.getValue() > world.getMaxHeight()) {
                                return false;
                            }
                        }
                    }
                    return true;
                },
                "Min-Height limits are invalid for at least one world (min/max exceeds the world's height properties!)"
        );
        register(
                type -> {
                    PerWorldData<Integer> worldData = type.get(PropertyKeys.MAX_HEIGHT_LIMIT);
                    for (Map.Entry<String, Integer> entry : worldData.getOverrides().entrySet()) {
                        World world = Bukkit.getWorld(entry.getKey());
                        // World could not be loaded yet, ignore those!
                        if (world != null) {
                            if (entry.getValue() < world.getMinHeight() || entry.getValue() > world.getMaxHeight()) {
                                return false;
                            }
                        }
                    }
                    return true;
                },
                "Max-Height limits are invalid for at least one world (min/max exceeds the world's height properties!)"
        );
    }

    record MinMaxValidator<T extends Number>(PropertyKey<T> minKey, PropertyKey<T> maxKey) implements Predicate<TypeSafeCraftType> {

        @Override
        public boolean test(TypeSafeCraftType typeSafeCraftType) {
            if (!(typeSafeCraftType.hasInSelfOrAnyParent(minKey()) && typeSafeCraftType.hasInSelfOrAnyParent(maxKey()))) {
                return false;
            }
            T min = typeSafeCraftType.get(minKey());
            T max = typeSafeCraftType.get(maxKey());
            return min.doubleValue() <= max.doubleValue();
        }
    }

    record PerWorldMinMaxValidator<T extends Number>(PropertyKey<PerWorldData<T>> minKey, PropertyKey<PerWorldData<T>> maxKey) implements Predicate<TypeSafeCraftType> {

        @Override
        public boolean test(TypeSafeCraftType typeSafeCraftType) {
            if (typeSafeCraftType.hasInSelfOrAnyParent(minKey()) != typeSafeCraftType.hasInSelfOrAnyParent(maxKey())) {
                return false;
            }

            PerWorldData<T> min = typeSafeCraftType.get(minKey());
            PerWorldData<T> max = typeSafeCraftType.get(maxKey());

            if (min.getDefaultFallback().doubleValue() > max.getDefaultFallback().doubleValue()) {
                return false;
            }

            Set<String> worldNames = new HashSet<>(min.getOverrides().keySet());
            worldNames.addAll(max.getOverrides().keySet());

            for (String worldName : worldNames) {
                T minVal = min.get(worldName);
                T maxVal = max.get(worldName);
                if (minVal == null || maxVal == null) {
                    return false;
                }
                if (minVal.doubleValue() > maxVal.doubleValue()) {
                    return false;
                }
            }

            return true;
        }
    }

}
