package net.countercraft.movecraft.craft.type.property;

import io.papermc.paper.registry.RegistryKey;
import net.countercraft.movecraft.util.SerializationUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SerializableAs("Movecraft_NamespacedKeyToDoubleProperty")
public class NamespacedKeyToDoubleProperty implements ConfigurationSerializable {

    private final Map<NamespacedKey, Double> mapping = new HashMap<>();

    public NamespacedKeyToDoubleProperty(NamespacedKeyToDoubleProperty toClone) {
        this.copy();
    }

    public NamespacedKeyToDoubleProperty() {
        super();
    }

    public NamespacedKeyToDoubleProperty copy() {
        NamespacedKeyToDoubleProperty result = new NamespacedKeyToDoubleProperty();
        for (Map.Entry<NamespacedKey, Double> entry : this.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    protected double put(NamespacedKey key, double value) {
        return this.mapping.put(key, value);
    }

    public void fill(Map<NamespacedKey, Double> values) {
        if (this.mapping.isEmpty()) {
            this.mapping.putAll(values);
        }
    }

    @Nullable
    public Double get(NamespacedKey key) {
        return this.mapping.get(key);
    }

    @Nullable
    public Double getOrDefault(NamespacedKey key, Double defaultValue) {
        return this.mapping.getOrDefault(key, defaultValue);
    }

    public boolean contains(NamespacedKey key) {
        return this.mapping.containsKey(key);
    }

    public static @NotNull NamespacedKeyToDoubleProperty deserialize(@NotNull Map<String, Object> args) {
        final NamespacedKeyToDoubleProperty result = new NamespacedKeyToDoubleProperty();

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            List<String> strings = Arrays.asList(entry.getKey().split(","));
            Set<NamespacedKey> blockKeys = SerializationUtil.deserializeNamespacedKeySet(strings, new HashSet<>(), RegistryKey.BLOCK);
            Set<NamespacedKey> itemKeys = SerializationUtil.deserializeNamespacedKeySet(strings, new HashSet<>(), RegistryKey.ITEM);
            Object value = entry.getValue();
            double doubleVal = NumberConversions.toDouble(value);
            for (NamespacedKey keyTmp : blockKeys) {
                result.put(keyTmp, doubleVal);
            }
            for (NamespacedKey keyTmp : itemKeys) {
                result.put(keyTmp, doubleVal);
            }
        }

        return result;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>(this.mapping.size());

        for (Map.Entry<NamespacedKey, Double> entry : this.mapping.entrySet()) {
            // TODO: Move to BlockSetProperty and allow passing it the original value it was read as
            result.put(entry.getKey().toString(), entry.getValue());
        }

        return result;
    }

    public boolean isEmpty() {
        return this.mapping.isEmpty();
    }

    public Iterable<? extends Map.Entry<NamespacedKey, Double>> entrySet() {
        return this.mapping.entrySet();
    }

    @SerializableAs("NamespacedKeyToDoublePropertyMutable")
    public static class Mutable extends NamespacedKeyToDoubleProperty {
        public Mutable() {
            super();
        }

        @Override
        public double put(NamespacedKey key, double value) {
            return super.put(key, value);
        }
    }

}
