package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Map;

@FunctionalInterface
public interface MaterialSetTransform extends Transform<EnumSet<Material>> {
    Map<String, EnumSet<Material>> transform(Map<String, EnumSet<Material>> data, CraftType type);
}
