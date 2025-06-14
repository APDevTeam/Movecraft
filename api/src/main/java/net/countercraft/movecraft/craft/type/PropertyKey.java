package net.countercraft.movecraft.craft.type;

import org.bukkit.NamespacedKey;

import java.util.function.Function;

public class PropertyKey<T> {

    private final NamespacedKey key;
    private final Function<CraftType, T> defaultProviderFunction;

    public PropertyKey(NamespacedKey key, Function<CraftType, T> defaultProvider) {
        this.key = key;
        this.defaultProviderFunction = defaultProvider;
    }

    public PropertyInstance<T> createInstance(CraftType type) {
        re
    }

    private class ImmutablePropertyKey<T> extends PropertyKey<T> {

        public ImmutablePropertyKey(NamespacedKey key, Function<CraftType, T> defaultProvider) {
            super(key, defaultProvider);
        }
    }


}
