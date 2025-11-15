package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.transform.TypeSafeTransform;

public class Transformers {

    public static <T> TypeSafeTransform<T> register(TypeSafeTransform<T> transform) {
        if (!TypeSafeCraftType.TRANSFORM_REGISTRY.add(transform)) {
            throw new IllegalStateException("No duplicate transformers allowed!");
        }
        return transform;
    }

    static void registerAll() {

    }

}
