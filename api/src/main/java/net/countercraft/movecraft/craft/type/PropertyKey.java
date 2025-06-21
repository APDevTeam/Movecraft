package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.PrimitiveType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

// R: the returned type for the property supplier
// T: The parsed type

// TODO: Rework to return the same type as it parses as => its cleaner that way! Also make generic type required to implement Cloneable for non primitives!
public class PropertyKey<T> extends TypedKey<T> {

    public static interface ImmutableKey {

    }

    protected final Function<TypeSafeCraftType, T> defaultProviderFunction;

    protected final int registrationIndex = TypeSafeCraftType.PROPERTY_REGISTRY.size();

    protected final BiFunction<Object, TypeSafeCraftType, T> deserializeFunction;
    protected final Function<T, Object> serializeFunction;

    protected final Function<T, T> cloneFunction;

    private PropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction, Function<T, T> cloneFunction) {
        super(key);
        this.defaultProviderFunction = defaultProvider;
        this.deserializeFunction = deserializeFunction;
        this.serializeFunction = serializeFunction;
        this.cloneFunction = cloneFunction;
    }

    public int getRegistrationIndex() {
        return this.registrationIndex;
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

    @NotNull T createCopy(T original) {
        return this.cloneFunction.apply(original);
    }

    public PropertyKey<T> immutable() {
        return new ImmutablePropertyKey<T>(this);
    }

    public PropertyKey<PerWorldData<T>> perWorld() {
        return new PerWorldPropertyKey<T>(this.key(), this.defaultProviderFunction, this.deserializeFunction, this.serializeFunction, this.cloneFunction);
    }

    public static PropertyKey<String> stringPropertyKey(NamespacedKey key) {
        return PropertyKey.stringPropertyKey(key, null);
    }

    public static PropertyKey<Integer> intPropertyKey(NamespacedKey key) {
        return PropertyKey.intPropertyKey(key, null);
    }

    public static PropertyKey<Boolean> boolPropertyKey(NamespacedKey key) {
        return PropertyKey.boolPropertyKey(key, null);
    }

    public static PropertyKey<Double> doublePropertyKey(NamespacedKey key) {
        return PropertyKey.doublePropertyKey(key, null);
    }

    public static PropertyKey<Float> floatPropertyKey(NamespacedKey key) {
        return PropertyKey.floatPropertyKey(key, null);
    }

    public static PropertyKey<RequiredBlockEntry> requiredBlockEntryKey(NamespacedKey key) {
        return requiredBlockEntryKey(key, null);
    }

    public static PropertyKey<Set<RequiredBlockEntry>> requiredBlockEntrySetKey(NamespacedKey key) {
        return requiredBlockEntrySetKey(key, null);
    }

    public static PropertyKey<EnumSet<Material>> materialSetKey(NamespacedKey key) {
        return materialSetKey(key, null);
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
                return new HashSet<>((Set<RequiredBlockEntry>)obj);
            }
            return defaultProvider.apply(type);
        }, (s) -> s, copySet(RequiredBlockEntry::new));
    }

    // TODO: This is ugly!
    private static <T> Function<Set<T>, Set<T>> copySet(Function<T, T> elementCloner) {
        return (set) -> {
            Set<T> copy = Set.copyOf(set);
            copy.clear();
            set.forEach((e) -> copy.add(elementCloner.apply(e)));
            return copy;
        };
    }

    public static PropertyKey<EnumSet<Material>> materialSetKey(NamespacedKey key, Function<TypeSafeCraftType, EnumSet<Material>> defaultProvider) {
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
        }, (s) -> s, EnumSet::copyOf);
    }

//    public PropertyKey<T> perWorld(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Map<String, Object>> serializeFunction) {
//        return new PerWorldPropertyKey<>(key, defaultProvider, deserializeFunction, serializeFunction);
//    }

    class ImmutablePropertyKey<T> extends PropertyKey<T> implements ImmutableKey {

        private final PropertyKey<T> backing;

        public ImmutablePropertyKey(PropertyKey<T> backing) {
            super(backing.key(), null, null, null, null);
            this.backing = backing;
        }

        @Override
        public T getDefault(TypeSafeCraftType type) {
            return this.backing.getDefault(type);
        }

        @Override
        public @Nullable T read(Object yamlType, TypeSafeCraftType type) {
            return this.backing.read(yamlType, type);
        }
    }

    private class PerWorldPropertyKey<T> extends PropertyKey<PerWorldData<T>> {

        public PerWorldPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction, Function<T, T> cloneFunction) {
            super(key, PerWorldData.<T>createDefaultSupplier(defaultProvider), PerWorldData.<T>createDeserializer(deserializeFunction), PerWorldData.createSerializer(serializeFunction), PerWorldData.<T>createCloneFunction(cloneFunction));
        }

        @Override
        public @Nullable PerWorldData<T> read(Object yamlType, TypeSafeCraftType type) {
            return super.read(yamlType, type);
        }
    }

    private class ImmutablePerWorldPropertyKey<T> extends PerWorldPropertyKey<T> implements ImmutableKey {

        public ImmutablePerWorldPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction, Function<T, T> cloneFunction) {
            super(key, defaultProvider, deserializeFunction, serializeFunction, cloneFunction);
        }

        @Override
        public @Nullable PerWorldData<T> read(Object yamlType, TypeSafeCraftType type) {
            return super.read(yamlType, type).immutable();
        }

        @Override
        public PerWorldData<T> getDefault(TypeSafeCraftType type) {
            return super.getDefault(type).immutable();
        }
    }


}
