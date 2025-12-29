package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Function;

public class CraftDataTagContainer extends TypedContainer<CraftDataTagKey<?>> {
    public CraftDataTagContainer(){
        super();
    }

    @Override
    /**
     * Set the value associated with the provided tagKey on the associated craft.
     *
     * @param tagKey the tagKey to use for storing the relevant data
     * @param value the value to set for future lookups
     * @param <T> the type of the value
     */
    public <T> void set(@NotNull TypedKey<T> tagKey, @NotNull T value) {
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(tagKey.key())) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", tagKey));
        }
        super.set(tagKey, value);
    }

    @Override
    protected <T> @Nullable T get(@NotNull TypedKey<T> key, Function<CraftDataTagKey<?>, T> defaultSupplier) {
        if (!CraftDataTagRegistry.INSTANCE.isRegistered(key.key())) {
            throw new IllegalArgumentException(String.format("The provided key %s was not registered.", key));
        }
        return super.get(key, defaultSupplier);
    }

    /**
     * Gets the data value associated with the provided tagKey from a craft.
     *
     * @param craft the craft to perform a lookup against
     * @param tagKey the tagKey to use for looking up the relevant data
     * @return the tag value associate with the provided tagKey on the specified craft
     * @param <T> the value type of the registered data key
     * @throws IllegalArgumentException when the provided tagKey is not registered
     * @throws IllegalStateException when the provided tagKey does not match the underlying tag value
     */
    public <T> T get(final @NotNull Craft craft, @NotNull CraftDataTagKey<T> tagKey) {
        return this.get(tagKey, (k) -> tagKey.createNew(craft));
    }

}
