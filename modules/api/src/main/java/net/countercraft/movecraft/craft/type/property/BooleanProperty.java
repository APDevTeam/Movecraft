package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class BooleanProperty implements Property<Boolean> {
    private final String key;
    private final Function<CraftType, Boolean> defaultProvider;

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
    public BooleanProperty(@NotNull String key, @NotNull Function<CraftType, Boolean> defaultProvider) {
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
