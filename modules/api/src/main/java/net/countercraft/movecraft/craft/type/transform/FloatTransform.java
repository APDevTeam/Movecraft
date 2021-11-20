package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface FloatTransform extends Transform<Float> {
    Map<NamespacedKey, Float> transform(Map<NamespacedKey, Float> data, CraftType type);
}
