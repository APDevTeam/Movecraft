package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface StringTransform extends Transform<String> {
    Map<NamespacedKey, String> transform(Map<NamespacedKey, String> data, CraftType type);
}
