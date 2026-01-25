package net.countercraft.movecraft.craft.datatag;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record DataTagSerializer<T>(
        Function<Object, T> deserializer,
        Function<T, Object> serialzer
) {

    @Nullable
    public T deserialize(Object yamlObject) {
        if (this.deserializer != null) {
            return this.deserializer.apply(yamlObject);
        }
        return null;
    }

    @Nullable
    public Object serialize(T value) {
        if (this.serialzer != null) {
            return this.serialzer.apply(value);
        }
        return null;
    }

}
