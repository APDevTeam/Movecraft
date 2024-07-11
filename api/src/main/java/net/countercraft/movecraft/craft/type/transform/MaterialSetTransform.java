package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.EnumSet;
import java.util.Map;

@FunctionalInterface
public interface MaterialSetTransform extends Transform<EnumSet<Material>> {
    Map<NamespacedKey, EnumSet<Material>> transform(Map<NamespacedKey, EnumSet<Material>> data, CraftType type);
}
