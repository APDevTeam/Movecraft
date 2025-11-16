package net.countercraft.movecraft.craft.type;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class PropertyKeys {

    // region required base settings
    public static final PropertyKey<String> NAME = register(ProperteyKeyTypes.stringPropertyKey(key("name")).immutable());
    public static final PropertyKey<Integer> MAX_SIZE = register(ProperteyKeyTypes.intPropertyKey(key("max_size")).immutable());
    public static final PropertyKey<Integer> MIN_SIZE = register(ProperteyKeyTypes.intPropertyKey(key("min_size")).immutable());
    public static final PropertyKey<EnumSet<Material>> ALLOWED_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("allowed_blocks")).immutable());
    public static final PropertyKey<PerWorldData<Double>> SPEED = register(ProperteyKeyTypes.doublePropertyKey(key("speed")).perWorld().immutable());
    // endregion required base settings

    // region block constraints
    public static final PropertyKey<Set<RequiredBlockEntry>> FLY_BLOCKS = register(ProperteyKeyTypes.requiredBlockEntrySetKey(key("flyblocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> DETECTION_BLOCKS = register(ProperteyKeyTypes.requiredBlockEntrySetKey(key("detection_blocks"), t -> new HashSet<>()).immutable());
    public static final PropertyKey<Set<RequiredBlockEntry>> MOVE_BLOCKS = register(ProperteyKeyTypes.requiredBlockEntrySetKey(key("move_blocks"), t -> new HashSet<>()).immutable());
    // endregion block constraints

    // region block sets
    public static final PropertyKey<EnumSet<Material>> DIRECTIONAL_DEPENDENT_MATERIALS = register(ProperteyKeyTypes.materialSetKey(key("directional_dependent_materials"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> FORBIDDEN_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("forbidden_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> HARVEST_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("harvest_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> HARVESTER_BLADE_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("harvester_blade_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> PASSTHROUGH_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("passthrough_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> FORBIDDEN_HOVER_OVER_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("forbidden_hover_over_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
    public static final PropertyKey<EnumSet<Material>> DYNAMIC_FLY_BLOCKS = register(ProperteyKeyTypes.materialSetKey(key("dynamic_fly_blocks"), t -> EnumSet.noneOf(Material.class)).immutable());
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
