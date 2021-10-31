package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface BooleanTransform extends Transform<Boolean> {
    Map<NamespacedKey, Boolean> transform(Map<NamespacedKey, Boolean> data, CraftType type);
}
