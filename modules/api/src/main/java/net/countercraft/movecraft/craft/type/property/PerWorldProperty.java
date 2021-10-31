package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PerWorldProperty<Type> implements Property<Map<String, Type>> {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final Function<CraftType, Map<String, Type>> defaultProvider;

    /**
     * Construct a PerWorldProperty
     * <p>Note: this constructor makes this a required property.
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     */
    public PerWorldProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.defaultProvider = null;
    }

    /**
     * Construct a PerWorldProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public PerWorldProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull Function<CraftType, Map<String, Type>> defaultProvider) {
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
    public Map<String, Type> load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return stringToMapFromObject(data.getDataOrEmpty(fileKey).getBackingData());
        }
        catch (TypeData.KeyNotFoundException e) {
            if(defaultProvider == null)
                throw e;

            return defaultProvider.apply(type);
        }
    }

    @NotNull
    private Map<String, Type> stringToMapFromObject(@NotNull Map<String, Object> objMap) {
        HashMap<String, Type> returnMap = new HashMap<>();
        for (String key : objMap.keySet()) {
            if(key == null)
                throw new TypeData.InvalidValueException("Entry in " + fileKey + " is null");
            Object o = objMap.get(key);
            if(o == null)
                throw new TypeData.InvalidValueException("Entry in " + fileKey + " for " + key + " is null");
            Type t = (Type) o;
            returnMap.put(key, t);
        }
        return returnMap;
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
