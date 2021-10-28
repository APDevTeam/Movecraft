package net.countercraft.movecraft.craft;

@FunctionalInterface
public interface DefaultProvider<Type> {
    Type process(CraftType type);
}
