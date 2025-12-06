package net.countercraft.movecraft.craft.type;

import io.papermc.paper.registry.RegistryKey;
import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.SerializationUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SerializableAs("BlocksToDoubleMapping")
public class BlocksToDoubleMapping extends HashMap<BlockSetProperty, Double> implements ConfigurationSerializable {

    public static @NotNull BlocksToDoubleMapping deserialize(@NotNull Map<String, Object> args) {
        final BlocksToDoubleMapping result = new BlocksToDoubleMapping();

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            // TODO: Check if this can actually work!
            Set<NamespacedKey> keys = SerializationUtil.deserializeNamespacedKeySet(entry.getKey(), new HashSet<>(), RegistryKey.BLOCK);
            Object value = entry.getValue();
            double doubleVal = NumberConversions.toDouble(value);
            result.put(new BlockSetProperty(keys), doubleVal);
        }

        return result;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>(this.size());

        for (Map.Entry<BlockSetProperty, Double> entry : this.entrySet()) {
            // TODO: Implement properly in BlockSetProperty!
        }

        return result;
    }

    public boolean contains(final NamespacedKey key) {
        for (BlockSetProperty collection : this.keySet()) {
            if (collection.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public double get(final NamespacedKey key) {
        for (BlockSetProperty collection : this.keySet()) {
            if (collection.contains(key)) {
                return this.get(collection);
            }
        }
        return 0.0D;
    }

}
