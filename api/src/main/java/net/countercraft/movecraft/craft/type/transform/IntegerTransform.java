package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface IntegerTransform extends Transform<Integer> {
    Map<NamespacedKey, Integer> transform(Map<NamespacedKey, Integer> data, CraftType type);
}
