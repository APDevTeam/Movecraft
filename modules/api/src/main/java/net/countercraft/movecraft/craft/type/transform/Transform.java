package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.NamespacedKey;

import java.util.Map;

@FunctionalInterface
public interface Transform<Type> {
    Map<NamespacedKey, Type> transform(Map<NamespacedKey, Type> data, CraftType type);
}
