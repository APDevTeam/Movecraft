package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import net.countercraft.movecraft.util.functions.QuadFunction;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class ObjectPropertyImpl implements ObjectProperty {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final QuadFunction<TypeData, CraftType, String, NamespacedKey, Object> loadProvider;
    private final Function<CraftType, Object> defaultProvider;

    /**
     * Construct an ObjectProperty
     * <p>Note: this constructor makes this a required property.
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param loadProvider the function to load the property
     */
    public ObjectPropertyImpl(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull QuadFunction<TypeData, CraftType, String, NamespacedKey, Object> loadProvider) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.loadProvider = loadProvider;
        this.defaultProvider = null;
    }

    /**
     * Construct an ObjectProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param loadProvider the function to load the property
     * @param defaultProvider the provider for the default value of this property
     */
    public ObjectPropertyImpl(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull QuadFunction<TypeData, CraftType, String, NamespacedKey, Object> loadProvider, @NotNull Function<CraftType, Object> defaultProvider) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.loadProvider = loadProvider;
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
            return loadProvider.apply(data, type, fileKey, namespacedKey);
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
