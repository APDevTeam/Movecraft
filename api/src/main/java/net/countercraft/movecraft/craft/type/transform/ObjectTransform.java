package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface ObjectTransform extends Transform<Object> {
    Map<NamespacedKey, Object> transform(Map<NamespacedKey, Object> data, CraftType type);
}
