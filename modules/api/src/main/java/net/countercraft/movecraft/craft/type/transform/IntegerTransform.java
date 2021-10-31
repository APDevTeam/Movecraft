package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface IntegerTransform extends Transform<Integer> {
    Map<String, Integer> transform(Map<String, Integer> data, CraftType type);
}
