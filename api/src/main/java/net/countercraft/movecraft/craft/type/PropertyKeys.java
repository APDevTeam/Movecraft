package net.countercraft.movecraft.craft.type;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class PropertyKeys {

    // region required base settings
    public static final PropertyKey<String> NAME = register(PropertyKey.stringPropertyKey(key("name")).immutable());
    public static final PropertyKey<Integer> MAX_SIZE = register(PropertyKey.intPropertyKey(key("max_size")).immutable());
    public static final PropertyKey<Integer> MIN_SIZE = register(PropertyKey.intPropertyKey(key("min_size")).immutable());
    public static final PropertyKey<EnumSet<Material>> ALLOWED_BLOCKS = register(PropertyKey.materialSetKey(key("allowed_blocks")).immutable());
    public static final PropertyKey<PerWorldData<Double>> SPEED = register(PropertyKey.doublePropertyKey(key("speed")).perWorld().immutable());
    // endregion required base settings

    // region block constraints
    public static final PropertyKey<Set<RequiredBlockEntry>> FLY_BLOCKS = register(PropertyKey.requiredBlockEntrySetKey(key("flyblocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> DETECTION_BLOCKS = register(PropertyKey.requiredBlockEntrySetKey(key("detection_blocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> MOVE_BLOCKS = register(PropertyKey.requiredBlockEntrySetKey(key("move_blocks"), t -> new HashSet<>()).immutable());
    // endregion block constraints

    // region block sets
    public static final PropertyKey<EnumSet<Material>> DIRECTIONAL_DEPENDENT_MATERIALS = register(PropertyKey.materialSetKey(key("directional_dependent_materials"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> FORBIDDEN_BLOCKS = register(PropertyKey.materialSetKey(key("forbidden_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> HARVEST_BLOCKS = register(PropertyKey.materialSetKey(key("harvest_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> HARVESTER_BLADE_BLOCKS = register(PropertyKey.materialSetKey(key("harvester_blade_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> PASSTHROUGH_BLOCKS = register(PropertyKey.materialSetKey(key("passthrough_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> FORBIDDEN_HOVER_OVER_BLOCKS = register(PropertyKey.materialSetKey(key("forbidden_hover_over_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> DYNAMIC_FLY_BLOCKS = register(PropertyKey.materialSetKey(key("dynamic_fly_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
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
