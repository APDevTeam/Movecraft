package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class FloatProperty implements Property<Float> {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final Function<CraftType, Float> defaultProvider;

    /**
     * Construct a FloatProperty
     * <p>Note: this constructor makes this a required property.
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     */
    public FloatProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.defaultProvider = null;
    }

    /**
     * Construct a FloatProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public FloatProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull Function<CraftType, Float> defaultProvider) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
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
    public Float load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return (float) data.getDouble(fileKey);
        }
        catch (TypeData.KeyNotFoundException e) {
            if(defaultProvider == null)
                throw e;

            return defaultProvider.apply(type);
        }
    }

    /**
     * Get the file key for this property
     *
     * @return the file key
     */
    @NotNull
    public String getFileKey() {
        return fileKey;
    }

    /**
     * Get the NamespacedKey for this property
     *
     * @return the NamespacedKey
     */
    @NotNull
    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }
}
