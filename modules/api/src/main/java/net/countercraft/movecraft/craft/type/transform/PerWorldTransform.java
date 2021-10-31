package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.NamespacedKey;

import java.util.Map;
import java.util.function.Function;

public interface PerWorldTransform extends Transform<Pair<Map<String, Object>, Function<CraftType, Object>>> {
    Map<NamespacedKey, Pair<Map<String, Object>, Function<CraftType, Object>>> transform(Map<NamespacedKey, Pair<Map<String, Object>, Function<CraftType, Object>>> data, CraftType type);
}
