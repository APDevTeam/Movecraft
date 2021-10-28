package net.countercraft.movecraft.craft.type;

@FunctionalInterface
public interface DefaultProvider<Type> {
    Type process(CraftType type);
}
