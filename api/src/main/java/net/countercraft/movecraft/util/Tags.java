package net.countercraft.movecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Tags {
    public static final EnumSet<Material> WATER = EnumSet.of(Material.WATER, Material.BUBBLE_COLUMN);
    public static final EnumSet<Material> FLUID = EnumSet.of(Material.WATER, Material.BUBBLE_COLUMN, Material.LAVA);
    public static final EnumSet<Material> CHESTS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
    public static final EnumSet<Material> FURNACES = EnumSet.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER);
    public static final EnumSet<Material> SINKING_PASSTHROUGH = EnumSet.of(Material.TALL_GRASS, Material.GRASS);
    public static final EnumSet<Material> FRAGILE_MATERIALS = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> FALL_THROUGH_BLOCKS = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> BUCKETS = EnumSet.of(Material.LAVA_BUCKET, Material.WATER_BUCKET, Material.MILK_BUCKET, Material.COD_BUCKET, Material.PUFFERFISH_BUCKET, Material.SALMON_BUCKET, Material.TROPICAL_FISH_BUCKET);

    static {
        FRAGILE_MATERIALS.add(Material.PISTON_HEAD);
        FRAGILE_MATERIALS.add(Material.TORCH);
        FRAGILE_MATERIALS.add(Material.REDSTONE_WIRE);
        FRAGILE_MATERIALS.add(Material.LADDER);
        FRAGILE_MATERIALS.addAll(Tag.DOORS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.CARPETS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.RAILS.getValues());
        FRAGILE_MATERIALS.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());

        for (Material m : Material.values()) {
            if (!m.isAir())
                continue;

            FALL_THROUGH_BLOCKS.add(m);
        }
        FALL_THROUGH_BLOCKS.add(Material.DEAD_BUSH);
        FALL_THROUGH_BLOCKS.addAll(Tag.CORAL_PLANTS.getValues());
        FALL_THROUGH_BLOCKS.add(Material.BROWN_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.RED_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.TORCH);
        FALL_THROUGH_BLOCKS.add(Material.FIRE);
        FALL_THROUGH_BLOCKS.add(Material.REDSTONE_WIRE);
        FALL_THROUGH_BLOCKS.add(Material.LADDER);
        FALL_THROUGH_BLOCKS.addAll(Tag.SIGNS.getValues());
        FALL_THROUGH_BLOCKS.add(Material.LEVER);
        FALL_THROUGH_BLOCKS.add(Material.STONE_BUTTON);
        FALL_THROUGH_BLOCKS.add(Material.SNOW);
        FALL_THROUGH_BLOCKS.add(Material.CARROT);
        FALL_THROUGH_BLOCKS.add(Material.POTATO);
        FALL_THROUGH_BLOCKS.addAll(Tag.FENCES.getValues());
        FALL_THROUGH_BLOCKS.addAll(FLUID);
    }

    @Nullable
    public static EnumSet<Material> parseBlockRegistry(@NotNull String string) {
        if (!string.startsWith("#"))
            return null;

        String nameKey = string.substring(1);
        var key = keyFromString(nameKey);
        if (key == null)
            throw new IllegalArgumentException("Entry " + string + " is not a valid namespace key!");

        var tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
        if (tag == null)
            throw new IllegalArgumentException("Entry " + string + " is not a valid tag!");

        var tags = tag.getValues();
        return tags.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(tags);
    }

    @Nullable
    public static <T extends Enum<T> & Keyed> EnumSet<T> parseRegistry(@NotNull String identifier, String registry, Class<T> clazz) {
        if (!identifier.startsWith("#")) {
            return null;
        }
        String nameKey = identifier.substring(1);
        var key = keyFromString(nameKey);
        if (key == null) {
            throw new IllegalArgumentException("Entry " + identifier + " is not a valid tag!");
        }
        var tag = Bukkit.getTag(registry, key, clazz);
        if (tag == null) {
            throw new IllegalArgumentException("Entry " + identifier + " is not a valid tag!");
        }
        var tagged = tag.getValues();
        return tagged.isEmpty() ? EnumSet.noneOf(clazz) : EnumSet.copyOf(tagged);
    }

    /**
     * Gets a NamespacedKey from the supplied string with a default namespace of minecraft.
     * This is intended to be used to parse NamespacedKeys from user input before the API existed in 1.16
     *
     * @param string the string to convert to a NamespacedKey
     * @return the created NamespacedKey, or null if invalid
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static NamespacedKey keyFromString(@NotNull String string) {
        try {
            if (string.contains(":")) {
                int index = string.indexOf(':');
                String namespace = string.substring(0, index);
                String key = string.substring(index + 1);
                // While a string based constructor is not supposed to be used,
                // their does not exist any other method for doing this in < 1.16
                return new NamespacedKey(namespace, key);
            } else {
                return NamespacedKey.minecraft(string);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Searches for a tag which matches the provided materialName.  Failing that, it attempts to load a matching singular Material directly.
     *
     * @param materialName Material name or tag
     * @return the set of materials the tag/material resolves to
     */
    @NotNull
    public static EnumSet<Material> parseMaterials(@NotNull String materialName) {
        EnumSet<Material> returnSet = EnumSet.noneOf(Material.class);
        var tagged = parseBlockRegistry(materialName);
        if (tagged != null) {
            returnSet.addAll(tagged);
        } else {
            returnSet.add(Material.valueOf(materialName.toUpperCase()));
        }
        return returnSet;
    }
}
