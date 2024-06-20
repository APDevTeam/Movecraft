package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface DoubleTransform extends Transform<Double> {
    Map<NamespacedKey, Double> transform(Map<NamespacedKey, Double> data, CraftType type);
}
