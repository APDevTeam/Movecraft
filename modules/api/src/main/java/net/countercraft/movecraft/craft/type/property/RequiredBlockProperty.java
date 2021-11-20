package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.craft.type.TypeData;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class RequiredBlockProperty implements Property<Set<RequiredBlockEntry>> {
    private final String fileKey;
    private final NamespacedKey namespacedKey;
    private final Function<CraftType, Set<RequiredBlockEntry>> defaultProvider;

    /**
     * Construct a RequiredBlockProperty
     * <p>Note: this constructor makes this a required property.
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     */
    public RequiredBlockProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey) {
        this.fileKey = fileKey;
        this.namespacedKey = namespacedKey;
        this.defaultProvider = null;
    }

    /**
     * Construct a RequiredBlockProperty
     *
     * @param fileKey the key for this property
     * @param namespacedKey the namespaced key for this property
     * @param defaultProvider the provider for the default value of this property
     */
    public RequiredBlockProperty(@NotNull String fileKey, @NotNull NamespacedKey namespacedKey, @NotNull Function<CraftType, Set<RequiredBlockEntry>> defaultProvider) {
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
    public Set<RequiredBlockEntry> load(@NotNull TypeData data, @NotNull CraftType type) {
        try {
            return data.getRequiredBlockEntrySet(fileKey);
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
