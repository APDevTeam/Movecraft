package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.PropertyKey;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@FunctionalInterface
public interface TypeSafeTransform {

    boolean transform(TypeSafeCraftType type, BiConsumer<PropertyKey, Object> setter, Set<PropertyKey> toDelete);
}
