package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PropertyKey<T> extends TypedKey<T> {

    private final Function<TypeSafeCraftType, T> defaultProviderFunction;

    public PropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider) {
        super(key);
        this.defaultProviderFunction = defaultProvider;
    }

    public PropertyInstance<T> createInstance(TypeSafeCraftType type) {
        return null;
    }

    public T read(Object yamlType, TypeSafeCraftType type) {
        // TODO: Implement!
        return null;
    }

    private class ImmutablePropertyKey<T> extends PropertyKey<T> {

        public ImmutablePropertyKey(NamespacedKey key, Function<TypeSafeCraftType, T> defaultProvider) {
            super(key, defaultProvider);
        }

        @Override
        public PropertyInstance<T> createInstance(TypeSafeCraftType type) {
            // TODO: Implement
            return null;
        }
    }

    private class PerWorldPropertyKey<T> extends PropertyKey<PerWorldData<T>> {

        public PerWorldPropertyKey(NamespacedKey key, Function<TypeSafeCraftType, PerWorldData<T>> defaultProvider) {
            super(key, defaultProvider);
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
