package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Function;

public class MaterialSetProperty implements Property<EnumSet<Material>> {
    private final String key;
    private final Function<CraftType, EnumSet<Material>> defaultProvider;

    /**
     * Construct a MaterialSetProperty
     *
     * @param key the key for this property
     */
    public MaterialSetProperty(@NotNull String key) {
        this.key = key;
        this.defaultProvider = null;
    }

    /**
     * Construct a MaterialSetProperty
     *
     * @param key the key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public MaterialSetProperty(@NotNull String key, @NotNull Function<CraftType, EnumSet<Material>> defaultProvider) {
        this.key = key;
        this.defaultProvider = defaultProvider;
    }

    /**
     * Load and validate the property from data
     *
     * @param data TypeData to read the property from
     * @param type CrafType to provide to defaultProvider
     * @return the value
     */
    @Nullable
    public EnumSet<Material> load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getMaterials(key);
        }
        catch (TypeData.KeyNotFoundException e) {
            if(defaultProvider == null)
                throw e;

            return defaultProvider.apply(type);
        }
    }

    /**
     * Get the string key for this property
     *
     * @return the key
     */
    @NotNull
    public String getKey() {
        return key;
    }
}
