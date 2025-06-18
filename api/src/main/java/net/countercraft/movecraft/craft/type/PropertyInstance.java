package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.property.ImmutableProperty;
import net.countercraft.movecraft.craft.type.property.PerWorldProperty;
import net.countercraft.movecraft.craft.type.property.Property;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class PropertyInstance<T> {

    private T defaultValue;
    private final Map<String, T> worldOverrides;

    // TODO: This is icky, but making it prettier requires rewriting the entire CraftType
    private PropertyInstance(final PerWorldProperty<T> perWorldProperty, CraftType craftTypeInstance) {
        Pair<Map<String, Object>, BiFunction<CraftType, String, Object>> data = craftTypeInstance.perWorldPropertyMap.getOrDefault(perWorldProperty.getNamespacedKey(), null);
        if (data == null) {
            throw new IllegalStateException("No data found for perworldproperty <" + perWorldProperty.getNamespacedKey() + ">!");
        }
        defaultValue = (T) data.getRight().apply(craftTypeInstance, "");
        worldOverrides = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.getLeft().entrySet()) {
            worldOverrides.put(entry.getKey(), (T) entry.getValue());
        }
    }

    // TODO: This is icky, but making it prettier requires rewriting the entire CraftType
    private PropertyInstance(final PropertyKey<T> property, TypeSafeCraftType craftTypeInstance) {
        this.defaultValue = null;
        this.worldOverrides = new HashMap<>();

        final NamespacedKey key = property.key();

        //defaultValue = property.read(craftTypeInstance);
    }

    public T getValue(final String worldName) {
        if (this.worldOverrides.isEmpty()) {
            return this.defaultValue;
        } else {
            return this.worldOverrides.computeIfAbsent(worldName, s -> defaultValue);
        }
    }

    public boolean set(final T value) {
        this.defaultValue = value;
        return true;
    }

    public boolean set(final String worldName, final T value) {
        this.worldOverrides.put(worldName, value);
        return true;
    }

    private static class Immutable<T2> extends PropertyInstance<T2> {

        public Immutable(Property<T2> property, CraftType craftType) {
            super(property, craftType);
        }

        @Override
        public boolean set(T2 value) {
            return false;
            // TODO: Throw exception? Log warning?
        }

        public boolean set(final String worldName, final T2 value) {
            return false;
            // TODO: Throw exception? Log warning?
        }
    }

    public static <T3> PropertyInstance<T3> of(final Property<T3> property, CraftType craftType) {
        if (property instanceof ImmutableProperty<T3>) {
            return new Immutable<T3>(property, craftType);
        } else {
            return new PropertyInstance<T3>(property, craftType);
        }
    }

}
