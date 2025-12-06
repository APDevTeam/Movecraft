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

@SerializableAs("NamespacedKeyToDoubleProperty")
public class NamespacedKeyToDoubleProperty implements ConfigurationSerializable {

    private final Map<NamespacedKey, Double> mapping = new HashMap<>();

    public NamespacedKeyToDoubleProperty(NamespacedKeyToDoubleProperty toClone) {
        this(toClone.mapping);
    }

    public NamespacedKeyToDoubleProperty() {
        super();
    }

    public NamespacedKeyToDoubleProperty(Map<NamespacedKey, Double> copyFrom) {
        super();
        for (Map.Entry<NamespacedKey, Double> entry : copyFrom.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    protected double put(NamespacedKey key, double value) {
        return this.mapping.put(key, value);
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
            Set<NamespacedKey> keys = SerializationUtil.deserializeNamespacedKeySet(strings, new HashSet<>(), RegistryKey.BLOCK);
            Object value = entry.getValue();
            double doubleVal = NumberConversions.toDouble(value);
            for (NamespacedKey keyTmp : keys) {
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

    @SerializableAs("NamespacedKeyToDoublePropertyMutable")
    public static class Mutable extends NamespacedKeyToDoubleProperty {
        public Mutable() {
            super();
        }

        public Mutable(Map<NamespacedKey, Double> copyFrom) {
            super(copyFrom);
        }

        @Override
        public double put(NamespacedKey key, double value) {
            return super.put(key, value);
        }
    }

}
