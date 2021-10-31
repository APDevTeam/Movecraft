package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObjectPropertyImpl implements ObjectProperty {
    private final String key;
    private final DefaultProvider<Object> defaultProvider;

    /**
     * Construct an ObjectProperty
     *
     * @param key the key for this property
     */
    public ObjectPropertyImpl(@NotNull String key) {
        this.key = key;
        this.defaultProvider = null;
    }

    /**
     * Construct an ObjectProperty
     *
     * @param key the key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public ObjectPropertyImpl(@NotNull String key, @NotNull DefaultProvider<Object> defaultProvider) {
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
    public Object load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            var backing = data.getBackingData();
            if(!backing.containsKey(key))
                throw new IllegalArgumentException("No key found for " + key);

            return backing.get(key);
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
