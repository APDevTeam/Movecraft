package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PerWorldData<T> {

    private T defaultFallback;
    private final Map<String, T> worldMapping;

    public PerWorldData(T defaultFallback) {
        this.defaultFallback = defaultFallback;
        this.worldMapping = new HashMap<>();
    }

    public PerWorldData(T defaultFallback, Map<String, T> override) {
        this.defaultFallback = defaultFallback;
        this.worldMapping = new HashMap<>(override);
    }

    private PerWorldData(PerWorldData<T> backing) {
        this(backing.defaultFallback, backing.worldMapping);
    }

    private PerWorldData<T> clone(Function<T,T> elementCloneFunction) {
        T clonedDefault = elementCloneFunction.apply(this.defaultFallback);
        // TODO: Is there a nicer way?
        Map<String, T> overrides = Map.copyOf(this.worldMapping);
        this.worldMapping.forEach((s, t) -> {
            overrides.put(s, elementCloneFunction.apply(t));
        });
        return this.createNew(clonedDefault, overrides);
    }

    protected PerWorldData<T> createNew(T defaultValue, Map<String, T> overrides) {
        return new PerWorldData<>(defaultValue, overrides);
    }

    public static <T> Function<PerWorldData<T>, PerWorldData<T>> createCloneFunction(Function<T,T> elementCloneFunction) {
        return (pwd) -> pwd.clone(elementCloneFunction);
    }

    public T get() {
        return this.defaultFallback;
    }

    public T get(String worldName) {
        return this.worldMapping.getOrDefault(worldName, this.defaultFallback);
    }

    public T get(MovecraftWorld movecraftWorld) {
        return get(movecraftWorld.getName());
    }

    public T get(World world) {
        return get(world.getName());
    }

    T getDefaultFallback() {
        return this.defaultFallback;
    }

    public boolean set(T value) {
        return this.set(value, null);
    }

    // Returns true if it applied the change
    public boolean set(T value, String worldName) {
        if (worldName == null) {
            this.defaultFallback = value;
        } else {
            this.worldMapping.put(worldName, value);
        }
        return true;
    }

    Map<String, T> getOverrides() {
        return new HashMap<>(this.worldMapping);
    }

    public PerWorldData<T> immutable() {
        return new ImmutablePerWorldData<>(this);
    }

    private class ImmutablePerWorldData<T> extends PerWorldData<T> {

        public ImmutablePerWorldData(PerWorldData<T> backing) {
            super(backing);
        }

        @Override
        public boolean set(T value, String worldName) {
            return false;
        }

        @Override
        protected PerWorldData<T> createNew(T defaultValue, Map<String, T> overrides) {
            return super.createNew(defaultValue, overrides).immutable();
        }
    }

    public static <T> Function<TypeSafeCraftType, PerWorldData<T>> createDefaultSupplier(final Function<TypeSafeCraftType, T> elementDefaultProvider) {
        return (type) -> {
            return new PerWorldData<>(elementDefaultProvider.apply(type));
        };
    }

    public static <T> BiFunction<Object, TypeSafeCraftType, PerWorldData<T>> createDeserializer(BiFunction<Object, TypeSafeCraftType, T> singleElementDeserializer) {
        return (yamlObj, type) -> {
            if (yamlObj instanceof Map) {
                // Parse map and pass it
                Map<String, Object> mapping;
                try {
                    mapping = (Map<String, Object>) yamlObj;
                } catch(ClassCastException cce) {
                    // TODO: Log error
                    return null;
                }
                T defaultObj = singleElementDeserializer.apply(mapping.getOrDefault("_default", null), type);
                if (defaultObj == null) {
                    // TODO: Log error
                    return null;
                }
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
                    return new PerWorldData<T>(defaultObj, overrides);
                } else {
                    return new PerWorldData<T>(defaultObj);
                }
            } else {
                return new PerWorldData<T>((T)yamlObj);
            }
        };
    }

    public static <T> Function<PerWorldData<T>, Object> createSerializer(Function<T, Object> singleElementSerializer) {
        return (pwd) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("_default", pwd.defaultFallback);
            pwd.worldMapping.forEach(result::put);
            return result;
        };
    }

}
