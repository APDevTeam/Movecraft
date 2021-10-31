package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface BooleanTransform extends Transform<Boolean> {
    Map<String, Boolean> transform(Map<String, Boolean> data, CraftType type);
}
