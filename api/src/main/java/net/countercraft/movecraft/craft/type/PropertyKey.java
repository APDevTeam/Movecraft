package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.SerializationUtil;
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

    public PropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction, Function<T, T> cloneFunction) {
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

//    public PropertyKey<T> perWorld(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Map<String, Object>> serializeFunction) {
//        return new PerWorldPropertyKey<>(key, defaultProvider, deserializeFunction, serializeFunction);
//    }

    static class ImmutablePropertyKey<T> extends PropertyKey<T> implements ImmutableKey {

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

    private static class PerWorldPropertyKey<T> extends PropertyKey<PerWorldData<T>> {

        public PerWorldPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider, BiFunction<Object, TypeSafeCraftType, T> deserializeFunction, Function<T, Object> serializeFunction, Function<T, T> cloneFunction) {
            super(key, PerWorldData.<T>createDefaultSupplier(defaultProvider), PerWorldData.<T>createDeserializer(deserializeFunction), PerWorldData.createSerializer(serializeFunction), PerWorldData.<T>createCloneFunction(cloneFunction));
        }

        @Override
        public @Nullable PerWorldData<T> read(Object yamlType, TypeSafeCraftType type) {
            return super.read(yamlType, type);
        }
    }

    private static class ImmutablePerWorldPropertyKey<T> extends PerWorldPropertyKey<T> implements ImmutableKey {

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
