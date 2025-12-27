package net.countercraft.movecraft.craft.type;

import io.papermc.paper.registry.RegistryKey;
import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import net.countercraft.movecraft.craft.type.property.NamespacedKeyToDoubleProperty;
import net.countercraft.movecraft.util.SerializationUtil;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.util.NumberConversions;

import java.util.*;
import java.util.function.Function;

public class PropertyKeyTypes {

    public static PropertyKey<String> stringPropertyKey(NamespacedKey key) {
        return stringPropertyKey(key, "");
    }

    public static PropertyKey<String> stringPropertyKey(NamespacedKey key, String defaultValue) {
        return stringPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<Integer> intPropertyKey(NamespacedKey key) {
        return intPropertyKey(key, 0);
    }

    public static PropertyKey<Integer> intPropertyKey(NamespacedKey key, int defaultValue) {
        return intPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<Boolean> boolPropertyKey(NamespacedKey key) {
        return boolPropertyKey(key, false);
    }

    public static PropertyKey<Boolean> boolPropertyKey(NamespacedKey key, boolean defaultValue) {
        return boolPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<Double> doublePropertyKey(NamespacedKey key) {
        return doublePropertyKey(key, 0.0D);
    }

    public static PropertyKey<Double> doublePropertyKey(NamespacedKey key, double defaultValue) {
        return doublePropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<Float> floatPropertyKey(NamespacedKey key) {
        return floatPropertyKey(key, 0.0F);
    }

    public static PropertyKey<Float> floatPropertyKey(NamespacedKey key, float defaultValue) {
        return floatPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<RequiredBlockEntry> requiredBlockEntryKey(NamespacedKey key) {
        return requiredBlockEntryKey(key, (RequiredBlockEntry) null);
    }

    public static PropertyKey<RequiredBlockEntry> requiredBlockEntryKey(NamespacedKey key, RequiredBlockEntry defaultValue) {
        return requiredBlockEntryKey(key, t -> defaultValue);
    }

    public static PropertyKey<Set<RequiredBlockEntry>> requiredBlockEntrySetKey(NamespacedKey key) {
        return requiredBlockEntrySetKey(key, new HashSet<>());
    }

    public static PropertyKey<Set<RequiredBlockEntry>> requiredBlockEntrySetKey(NamespacedKey key, Set<RequiredBlockEntry> defaultValue) {
        return requiredBlockEntrySetKey(key, t -> defaultValue);
    }

    public static PropertyKey<List<String>> stringListPropertyKey(NamespacedKey key) {
        return stringListPropertyKey(key, new ArrayList<>());
    }

    public static PropertyKey<List<String>> stringListPropertyKey(NamespacedKey key, List<String> defaultValue) {
        return stringListPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<EnumSet<Material>> materialSetKey(NamespacedKey key) {
        return materialSetKey(key, EnumSet.noneOf(Material.class));
    }

    public static PropertyKey<EnumSet<Material>> materialSetKey(NamespacedKey key, EnumSet<Material> defaultValue) {
        return materialSetKey(key, t -> defaultValue);
    }

    public static PropertyKey<BlockSetProperty> blockSetPropertyKey(NamespacedKey key) {
        return blockSetPropertyKey(key, new BlockSetProperty());
    }

    public static PropertyKey<BlockSetProperty> blockSetPropertyKey(NamespacedKey key, BlockSetProperty defaultValue) {
        return blockSetPropertyKey(key, t -> defaultValue);
    }

    public static PropertyKey<ConfiguredSound> configuredSoundPropertyKey(NamespacedKey key, String defaultSound, SoundCategory defaultSource, float defaultVolume, float defaultPitch) {
        return configuredSoundPropertyKey(key, t -> new ConfiguredSound(defaultSound, defaultSource, defaultVolume, defaultVolume, defaultPitch, defaultPitch));
    }

    public static PropertyKey<NamespacedKeyToDoubleProperty> namespacedKeyToDoublePropertyKey(NamespacedKey key) {
        return namespacedKeyToDoublePropertyKey(key, Map.of());
    }

    public static PropertyKey<NamespacedKeyToDoubleProperty> namespacedKeyToDoublePropertyKey(NamespacedKey key, Map<NamespacedKey, Double> defaultValue) {
        return namespacedKeyToDoublePropertyKey(key, t -> {
            NamespacedKeyToDoubleProperty result = new NamespacedKeyToDoubleProperty();
            result.fill(defaultValue);
            return result;
        });
    }

    public static PropertyKey<Integer> intPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, Integer> defaultProvider) {
        return new PropertyKey<Integer>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toInt(obj);
        }, (i) -> i, Integer::valueOf);
    }

    public static PropertyKey<Boolean> boolPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, Boolean> defaultProvider) {
        return new PropertyKey<Boolean>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof Boolean)) {
                return((Boolean) obj).booleanValue();
            }
            return defaultProvider.apply(type);
        }, (b) -> b, Boolean::valueOf);
    }

    public static PropertyKey<Double> doublePropertyKey(NamespacedKey key, Function<TypeSafeCraftType, Double> defaultProvider) {
        return new PropertyKey<Double>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toDouble(obj);
        }, (d) -> d, Double::valueOf);
    }

    public static PropertyKey<Float> floatPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, Float> defaultProvider) {
        return new PropertyKey<Float>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toFloat(obj);
        }, (f) -> f, Float::valueOf);
    }

    public static PropertyKey<String> stringPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, String> defaultProvider) {
        return new PropertyKey<String>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof String)) {
                return (String) obj;
            }
            return defaultProvider.apply(type);
        }, (s) -> s, String::valueOf);
    }

    public static PropertyKey<RequiredBlockEntry> requiredBlockEntryKey(NamespacedKey key, Function<TypeSafeCraftType, RequiredBlockEntry> defaultProvider) {
        return new PropertyKey<RequiredBlockEntry>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof RequiredBlockEntry)) {
                // RequiredBlockEntry is serializable!
                return (RequiredBlockEntry)obj;
            }
            return defaultProvider.apply(type);
        }, (r) -> r, RequiredBlockEntry::new);
    }

    public static PropertyKey<Set<RequiredBlockEntry>> requiredBlockEntrySetKey(NamespacedKey key, Function<TypeSafeCraftType, Set<RequiredBlockEntry>> defaultProvider) {
        return new PropertyKey<Set<RequiredBlockEntry>>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof List)) {
                // RequiredBlockEntry is serializable!
                return new HashSet<>((List<RequiredBlockEntry>)obj);
            }
            return defaultProvider.apply(type);
        }, (s) -> s, copySet(RequiredBlockEntry::new));
    }

    public static PropertyKey<List<String>> stringListPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, List<String>> defaultProvider) {
        return new PropertyKey<>(key, defaultProvider, (obj, type) -> {
            return SerializationUtil.deserializeStringList(obj, defaultProvider.apply(type));
        }, (s) -> s, ArrayList::new);
    }

    // TODO: This is ugly! Find better solution, but this should work
    private static <T> Function<Set<T>, Set<T>> copySet(Function<T, T> elementCloner) {
        return (set) -> {
            Set<T> copy = Set.copyOf(set);
            copy.clear();
            set.forEach((e) -> copy.add(elementCloner.apply(e)));
            return copy;
        };
    }

    /*Use blockSetPropertyKey!*/
    @Deprecated(forRemoval = true)
    public static PropertyKey<EnumSet<Material>> materialSetKey(NamespacedKey key, Function<TypeSafeCraftType, EnumSet<Material>> defaultProvider) {
        return new PropertyKey<EnumSet<Material>>(key, defaultProvider, (obj, type) -> {
            BlockSetProperty result = new BlockSetProperty();
            Set<NamespacedKey> namespacedKeys = SerializationUtil.deserializeNamespacedKeySet(obj, new HashSet<>(), RegistryKey.BLOCK);
            result.addAll(namespacedKeys);
            if (result.isEmpty()) {
                return defaultProvider.apply(type);
            }
            return result.get();
        }, (s) -> s, EnumSet::copyOf);
    }

    public static PropertyKey<BlockSetProperty> blockSetPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, BlockSetProperty> defaultProvider) {
        return new PropertyKey<>(key, defaultProvider, (obj, type) -> {
            BlockSetProperty result = new BlockSetProperty();
            Set<NamespacedKey> namespacedKeys = SerializationUtil.deserializeNamespacedKeySet(obj, new HashSet<>(), RegistryKey.BLOCK);
            result.addAll(namespacedKeys);
            if (result.isEmpty()) {
                return defaultProvider.apply(type);
            }
            return result;
        }, (s) -> s, BlockSetProperty::new);
    }

    public static PropertyKey<ConfiguredSound> configuredSoundPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, ConfiguredSound> defaultProvider) {
        return new PropertyKey<>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof ConfiguredSound)) {
                // ConfiguredSound is serializable!
                return (ConfiguredSound)obj;
            }
            return defaultProvider.apply(type);
        }, (s) -> s, t -> new ConfiguredSound(t.sound(), t.category(), t.minVolume(), t.maxVolume(), t.minPitch(), t.maxPitch()));
    }

    public static PropertyKey<NamespacedKeyToDoubleProperty> namespacedKeyToDoublePropertyKey(NamespacedKey key, Function<TypeSafeCraftType, NamespacedKeyToDoubleProperty> defaultProvider) {
        return new PropertyKey<>(key, defaultProvider, (obj, type) -> {
            if (obj != null && (obj instanceof NamespacedKeyToDoubleProperty)) {
                // ConfiguredSound is serializable!
                return (NamespacedKeyToDoubleProperty)obj;
            }
            return defaultProvider.apply(type);
        }, (s) -> s, NamespacedKeyToDoubleProperty::new);
    }

}
