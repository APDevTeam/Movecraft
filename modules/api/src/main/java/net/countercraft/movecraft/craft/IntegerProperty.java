package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class IntegerProperty {
    private final String key;
    private final DefaultProvider<Integer> defaultProvider;

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     */
    IntegerProperty(@NotNull String key) {
        this.key = key;
        this.defaultProvider = null;
    }

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     * @param defaultProvider the provider for the default value of this key
     */
    IntegerProperty(@NotNull String key, @NotNull DefaultProvider<Integer> defaultProvider) {
        this.key = key;
        this.defaultProvider = defaultProvider;
    }

    /**
     * Load and validate the property from data
     *
     * @param data TypeData to read the property from
     * @return the value
     */
    @Nullable Integer load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getInt(key);
        }
        catch (IllegalArgumentException e) {
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
    @NotNull String getKey() {
        return key;
    }
}
