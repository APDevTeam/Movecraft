package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.property.*;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private PropertyInstance(final Property<T> property, CraftType craftTypeInstance) {
        this.defaultValue = null;
        this.worldOverrides = new HashMap<>();

        final NamespacedKey key = property.getNamespacedKey();

        if(property instanceof StringProperty)
            defaultValue = (T) craftTypeInstance.getStringProperty(key);
        else if(property instanceof IntegerProperty)
            defaultValue = (T) ((Integer) craftTypeInstance.getIntProperty(key));
        else if(property instanceof BooleanProperty)
            defaultValue = (T) ((Boolean) craftTypeInstance.getBoolProperty(key));
        else if(property instanceof FloatProperty)
            defaultValue = (T) ((Float) craftTypeInstance.getFloatProperty(key));
        else if(property instanceof DoubleProperty)
            defaultValue = (T) ((Double) craftTypeInstance.getDoubleProperty(key));
        else if(property instanceof ObjectProperty)
            defaultValue = (T) craftTypeInstance.getObjectProperty(key);
        else if(property instanceof MaterialSetProperty)
            defaultValue = (T) craftTypeInstance.getMaterialSetProperty(key);
        else if(property instanceof PerWorldProperty<?>) {
            throw new IllegalStateException("Entered branch for PerWorldProperty! Has to use own constructor!");
        }
        else if(property instanceof RequiredBlockProperty)
            defaultValue = (T) craftTypeInstance.getRequiredBlockProperty(key);

        if (defaultValue == null) {
            throw new IllegalStateException("Cant resolve property type!");
        }
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
