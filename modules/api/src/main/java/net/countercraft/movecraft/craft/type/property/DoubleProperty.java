package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleProperty {
    private final String key;
    private final DefaultProvider<Double> defaultProvider;

    /**
     * Construct a DoubleProperty
     *
     * @param key the key for this property
     */
    public DoubleProperty(@NotNull String key) {
        this.key = key;
        this.defaultProvider = null;
    }

    /**
     * Construct a DoubleProperty
     *
     * @param key the key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public DoubleProperty(@NotNull String key, @NotNull DefaultProvider<Double> defaultProvider) {
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
    public Double load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getDouble(key);
        }
        catch (TypeData.KeyNotFoundException e) {
            if(defaultProvider == null)
                throw e;

            return defaultProvider.process(type);
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
