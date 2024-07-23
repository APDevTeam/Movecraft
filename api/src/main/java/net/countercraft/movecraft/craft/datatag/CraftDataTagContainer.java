package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CraftDataTagContainer extends HashMap<CraftDataTagKey<?>, Object> {

    public static final Map<NamespacedKey, CraftDataTagKey<?>> REGISTERED_TAGS = new HashMap<>();

    public static <T> CraftDataTagKey<T> tryRegisterTagKey(final NamespacedKey key, final Function<Craft, T> supplier) {
        if (REGISTERED_TAGS.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate keys are not allowed!");
        } else {
            CraftDataTagKey<T> result = new CraftDataTagKey<T>(key, supplier);
            REGISTERED_TAGS.put(key, result);
            return result;
        }
    }

    public <T> T get(final Craft craft, CraftDataTagKey<T> tagKey) {
        if (!REGISTERED_TAGS.containsKey(tagKey.key)) {
            // TODO: Log error
            return null;
        }
        T result = null;
        if (!this.containsKey(tagKey)) {
            result = tagKey.createNew(craft);
            this.put(tagKey, result);
        } else {
            Object stored = this.getOrDefault(tagKey, tagKey.createNew(craft));
            try {
                T temp = (T) stored;
                result = temp;
            } catch (ClassCastException cce) {
                // TODO: Log error
                result = tagKey.createNew(craft);
                this.put(tagKey, result);
            }
        }
        return result;
    }

    public <T> void set(CraftDataTagKey<T> tagKey, @NotNull T value) {
        this.put(tagKey, value);
    }

}
