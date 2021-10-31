package net.countercraft.movecraft.craft.type.property;

import net.countercraft.movecraft.craft.type.CraftType;

@FunctionalInterface
public interface DefaultProvider<Type> {
    Type process(CraftType type);
}
