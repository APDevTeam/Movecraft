package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.*;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PerWorldProperty<Type> implements Property<PerWorldData<Type>> {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final BiFunction<CraftType, String, Type> defaultProvider;

    /**
     * Construct a PerWorldProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public PerWorldProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull BiFunction<CraftType, String, Type> defaultProvider) {
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
    public PerWorldData<Type> load(@NotNull TypeData data, @NotNull CraftType type) {
        final Type defaultValue = this.defaultProvider.apply(type, "");
        try {
            return stringToMapFromObject(data.getDataOrEmpty(fileKey).getBackingData(), defaultValue);
        }
        catch (TypeData.KeyNotFoundException e) {
            return new PerWorldData<>(defaultValue);
        }
    }

    @NotNull
    private PerWorldData<Type> stringToMapFromObject(@NotNull Map<String, Object> objMap, Type defaultValue) {
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
        return new PerWorldData<>(defaultValue, returnMap);
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

    @NotNull
    public BiFunction<CraftType, String, Type> getDefaultProvider() {
        return defaultProvider;
    }

    @Override
    public PropertyKey<PerWorldData<Type>> asTypeSafeKey() {
        // TODO: Implement!
        throw new NotImplementedException("Migrate to the new property system ASAP!");
    }
}
