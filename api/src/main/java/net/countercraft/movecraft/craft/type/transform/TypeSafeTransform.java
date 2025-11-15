package net.countercraft.movecraft.craft.type.transform;

import net.countercraft.movecraft.craft.type.PropertyKey;

import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface TypeSafeTransform<T> {

    boolean transform(Function<PropertyKey<T>, T> valueRetrieverFunction, Map<PropertyKey<T>, T> transformedOutput);
}
