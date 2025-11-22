package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import org.bukkit.NamespacedKey;

import java.util.HashSet;
import java.util.Set;

public class PropertyKeys {

    // region required base settings
    @Deprecated(forRemoval = true)
    /**
     * Use TypeSafeCraftType#getName() instead
     */
    public static final PropertyKey<String> NAME = register(PropertyKeyTypes.stringPropertyKey(key("name"), t -> t.getName()).immutable());
    public static final PropertyKey<Integer> MAX_SIZE = register(PropertyKeyTypes.intPropertyKey(key("max_size")).immutable());
    public static final PropertyKey<Integer> MIN_SIZE = register(PropertyKeyTypes.intPropertyKey(key("min_size")).immutable());
    public static final PropertyKey<BlockSetProperty> ALLOWED_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("allowed_blocks")).immutable());
    public static final PropertyKey<PerWorldData<Double>> SPEED = register(PropertyKeyTypes.doublePropertyKey(key("speed")).perWorld().immutable());
    // endregion required base settings

    // region block constraints
    public static final PropertyKey<Set<RequiredBlockEntry>> FLY_BLOCKS = register(PropertyKeyTypes.requiredBlockEntrySetKey(key("flyblocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> DETECTION_BLOCKS = register(PropertyKeyTypes.requiredBlockEntrySetKey(key("detection_blocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> MOVE_BLOCKS = register(PropertyKeyTypes.requiredBlockEntrySetKey(key("move_blocks"), t -> new HashSet<>()).immutable());
    // endregion block constraints

    // region block sets
    public static final PropertyKey<BlockSetProperty> DIRECTIONAL_DEPENDENT_MATERIALS = register(PropertyKeyTypes.blockSetPropertyKey(key("directional_dependent_materials")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("forbidden_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVEST_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("harvest_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> HARVESTER_BLADE_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("harvester_blade_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> PASSTHROUGH_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("passthrough_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> FORBIDDEN_HOVER_OVER_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("forbidden_hover_over_blocks")).immutable());
    public static final PropertyKey<BlockSetProperty> DYNAMIC_FLY_BLOCKS = register(PropertyKeyTypes.blockSetPropertyKey(key("dynamic_fly_blocks")).immutable());
    // endregion block sets



    public static <T> PropertyKey<T> register(PropertyKey<T> propertyKey) {
        return TypeSafeCraftType.PROPERTY_REGISTRY.register(propertyKey.key(), propertyKey);
    }

    private static NamespacedKey key(String path) {
        return new NamespacedKey("movecraft", path);
    }

    static void registerAll() {

    }

}
