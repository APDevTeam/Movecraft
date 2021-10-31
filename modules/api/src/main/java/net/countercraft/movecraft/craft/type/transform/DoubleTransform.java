package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface DoubleTransform extends Transform<Double> {
    Map<String, Double> transform(Map<String, Double> data, CraftType type);
}
