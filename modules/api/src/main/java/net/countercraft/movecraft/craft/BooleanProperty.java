package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BooleanProperty {
    private final String key;
    private final DefaultProvider<Boolean> defaultProvider;

    /**
     * Construct a BooleanProperty
     *
     * @param key the key for this property
     */
    public BooleanProperty(@NotNull String key) {
        this.key = key;
        this.defaultProvider = null;
    }

    /**
     * Construct a BooleanProperty
     *
     * @param key the key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public BooleanProperty(@NotNull String key, @NotNull DefaultProvider<Boolean> defaultProvider) {
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
    public Boolean load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getBoolean(key);
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
    @NotNull
    public String getKey() {
        return key;
    }
}
