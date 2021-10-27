package net.countercraft.movecraft.craft;

public interface DefaultProvider<Type> {
    Type process(CraftType type);
}
