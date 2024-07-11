package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import org.bukkit.NamespacedKey;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface RequiredBlockTransform extends Transform<Set<RequiredBlockEntry>> {
    Map<NamespacedKey, Set<RequiredBlockEntry>> transform(Map<NamespacedKey, Set<RequiredBlockEntry>> data, CraftType type);
}
