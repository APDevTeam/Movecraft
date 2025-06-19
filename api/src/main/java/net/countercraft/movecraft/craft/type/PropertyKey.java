package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PropertyKey<T> extends TypedKey<T> {

    protected final Function<TypeSafeCraftType, T> defaultProviderFunction;

    protected final BiFunction<Object, TypeSafeCraftType, T> deserializeFunction;
    protected final Function<T, Object> serializeFunction;

    private PropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction) {
        super(key);
        this.defaultProviderFunction = defaultProvider;
        this.deserializeFunction = deserializeFunction;
        this.serializeFunction = serializeFunction;
    }

    public PropertyInstance<T> createInstance(TypeSafeCraftType type) {
        return null;
    }

    @Nullable
    public T read(Object yamlType, TypeSafeCraftType type) {
        if (this.deserializeFunction == null) {
            return null;
        }
        Object tmp = this.deserializeFunction.apply(yamlType, type);
        try {
            T result = (T) tmp;
            return result;
        } catch(ClassCastException cce) {
            // TODO: Log error
            return null;
        }
    }

    public T getDefault(TypeSafeCraftType type) {
        return this.defaultProviderFunction.apply(type);
    }

    public PropertyKey<T> immutable() {
        // TODO: Change to accept the instance as only argument so we can wrap it!
        return new ImmutablePropertyKey<>(this.key, this.defaultProviderFunction, this.deserializeFunction, this.serializeFunction);
    }

    public PropertyKey<T> perWorld() {
        // TODO: Implement
        return null;
    }

    public static <T> PropertyKey<T> key(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction) {
        return new PropertyKey<T>(key, defaultProvider, deserializeFunction, serializeFunction);
    }

    public static PropertyKey<Integer> intProperty(NamespacedKey key, Function<TypeSafeCraftType, Integer> defaultProvider) {
        return new PropertyKey<Integer>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toInt(obj);
        }, (i) -> i);
    }

    public static PropertyKey<Boolean> boolProperty(NamespacedKey key, Function<TypeSafeCraftType, Boolean> defaultProvider) {
        return new PropertyKey<Boolean>(key, defaultProvider, (obj, type) -> {
            boolean def = defaultProvider.apply(type);
            if (obj == null)
                return def;
            if (obj != null && (obj instanceof Boolean)) {
                return((Boolean) obj).booleanValue();
            }
            return def;
        }, (b) -> b);
    }

    public static PropertyKey<Double> doubleProperty(NamespacedKey key, Function<TypeSafeCraftType, Double> defaultProvider) {
        return new PropertyKey<Double>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toDouble(obj);
        }, (d) -> d);
    }

    public static PropertyKey<Float> floatProperty(NamespacedKey key, Function<TypeSafeCraftType, Float> defaultProvider) {
        return new PropertyKey<Float>(key, defaultProvider, (obj, type) -> {
            if (obj == null)
                return defaultProvider.apply(type);
            return NumberConversions.toFloat(obj);
        }, (f) -> f);
    }

    public static PropertyKey<String> stringPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, String> defaultProvider) {
        return new PropertyKey<String>(key, defaultProvider, (obj, type) -> {
            String def = defaultProvider.apply(type);
            if (obj == null)
                return def;
            if (obj != null && (obj instanceof String)) {
                return (String) obj;
            }
            return def;
        }, (s) -> s);
    }

    public static PropertyKey<RequiredBlockEntry> requiredBlockEntry(NamespacedKey key, Function<TypeSafeCraftType, RequiredBlockEntry> defaultProvider) {
        return new PropertyKey<RequiredBlockEntry>(key, defaultProvider, (obj, type) -> {
            RequiredBlockEntry def = defaultProvider.apply(type);
            if (obj == null)
                return def;
            if (obj != null && (obj instanceof RequiredBlockEntry)) {
                // RequiredBlockEntry is serializable!
                return (RequiredBlockEntry)obj;
            }
            return def;
        }, (r) -> r);
    }

    public static PropertyKey<Set<RequiredBlockEntry>> requiredBlockEntrySet(NamespacedKey key, Function<TypeSafeCraftType, Set<RequiredBlockEntry>> defaultProvider) {
        return new PropertyKey<Set<RequiredBlockEntry>>(key, defaultProvider, (obj, type) -> {
            Set<RequiredBlockEntry> def = defaultProvider.apply(type);
            if (obj == null)
                return def;
            if (obj != null && (obj instanceof List)) {
                // RequiredBlockEntry is serializable!
                return new HashSet<>((Set<RequiredBlockEntry>)obj);
            }
            return def;
        }, (s) -> s);
    }

    public static PropertyKey<EnumSet<Material>> materialSet(NamespacedKey key, Function<TypeSafeCraftType, EnumSet<Material>> defaultProvider) {
        return new PropertyKey<EnumSet<Material>>(key, defaultProvider, (obj, type) -> {
            EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
            if(!(obj instanceof ArrayList))
                throw new TypeData.InvalidValueException("key " + key + " must be a list of materials.");
            for(Object object : (ArrayList<?>) obj){
                if (!(object instanceof String)) {
                    if(object == null)
                        throw new TypeData.InvalidValueException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
                    throw new TypeData.InvalidValueException("Entry " + object + " must be a material for key " + key);
                }
                String materialName = (String) object;
                EnumSet<Material> materials = Tags.parseMaterials(materialName);
                if(materials.isEmpty())
                    throw new TypeData.InvalidValueException("Entry " + object + " describes an empty or non-existent Tag for key " + key);
                returnList.addAll(materials);
            }
            return returnList;
        }, (s) -> s);
    }

//    public PropertyKey<T> perWorld(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Map<String, Object>> serializeFunction) {
//        return new PerWorldPropertyKey<>(key, defaultProvider, deserializeFunction, serializeFunction);
//    }

    private class ImmutablePropertyKey<T> extends PropertyKey<T> {

        public ImmutablePropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction) {
            super(key, defaultProvider, deserializeFunction, serializeFunction);
        }

        @Override
        public PropertyInstance<T> createInstance(TypeSafeCraftType type) {
            // TODO: Implement
            return null;
        }
    }

    // TODO: Properly implement! WE dont need a function to serialize or deserialize the entire per world object, just the one for the generic type!
    private class PerWorldPropertyKey<T> extends PropertyKey<PerWorldData<T>> {

        public PerWorldPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, PerWorldData<T>> defaultProvider, BiFunction<Object, TypeSafeCraftType, PerWorldData<T>> deserializeFunction, Function<PerWorldData<T>, Object> serializeFunction) {
            super(key, defaultProvider, deserializeFunction, serializeFunction);
        }

        @Override
        public PerWorldData<T> read(Object yamlType, TypeSafeCraftType type) {
            if (yamlType instanceof Map) {
                // Parse map and pass it
                Map<String, Object> mapping;
                try {
                    mapping = (Map<String, Object>) yamlType;
                } catch(ClassCastException cce) {
                    // TODO: Log error
                    return null;
                }
                Object defaultObj = mapping.getOrDefault("_default", null);
                if (defaultObj == null) {
                    // TODO: Log error
                    return null;
                }
                T defaultCasted = (T) defaultObj;
                if (mapping.size() > 1) {
                    Map<String, T> overrides = new HashMap<>(mapping.size() - 1);
                    for (Map.Entry<String, Object> entry : mapping.entrySet()) {
                        if (entry.getKey() == "_default") {
                            continue;
                        }
                        try {
                            overrides.put(entry.getKey(), (T) entry.getValue());
                        } catch(ClassCastException cce) {
                            // TODO: Log error
                        }
                    }
                    return new PerWorldData<>(defaultCasted, overrides);
                } else {
                    return new PerWorldData<>(defaultCasted);
                }
            } else {
                return new PerWorldData<>((T)yamlType);
            }
        }
    }


}
