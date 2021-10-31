package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface FloatTransform extends Transform<Float> {
    Map<String, Float> transform(Map<String, Float> data, CraftType type);
}
