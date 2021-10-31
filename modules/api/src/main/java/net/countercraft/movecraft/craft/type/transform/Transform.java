package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.CraftType;

import java.util.Map;

@FunctionalInterface
public interface Transform<Type> {
    Map<String, Type> transform(Map<String, Type> data, CraftType type);
}
