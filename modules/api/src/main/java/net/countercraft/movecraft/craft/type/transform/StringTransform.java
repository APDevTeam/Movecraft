package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface StringTransform extends Transform<String> {
    Map<String, String> transform(Map<String, String> data, CraftType type);
}
