package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface ObjectTransform extends Transform<Object> {
    Map<String, Object> transform(Map<String, Object> data, CraftType type);
}
