package net.countercraft.movecraft.craft.datatag;

import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CraftDataTagContainer extends HashMap<CraftDataTagKey<?>, Object> {

    public static final Map<NamespacedKey, CraftDataTagKey<?>> REGISTERED_TAGS = new HashMap<>();

    public static <T extends ICraftDataTag> CraftDataTagKey<T> tryRegisterTagKey(final NamespacedKey key, final Supplier<T> supplier) {
        if (REGISTERED_TAGS.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate keys are not allowed!");
        } else {
            CraftDataTagKey<T> result = new CraftDataTagKey<T>(key, supplier);
            REGISTERED_TAGS.put(key, result);
            return result;
        }
    }

    public <T extends ICraftDataTag> T get(CraftDataTagKey<T> tagKey) {
        if (!REGISTERED_TAGS.containsKey(tagKey.key)) {
            // TODO: Log error
            return null;
        }
        T result = null;
        if (!this.containsKey(tagKey)) {
            result = tagKey.createNew();
            this.put(tagKey, result);
        } else {
            Object stored = this.getOrDefault(tagKey, tagKey.createNew());
            try {
                T temp = (T) stored;
                result = temp;
            } catch (ClassCastException cce) {
                // TODO: Log error
                result = tagKey.createNew();
                this.put(tagKey, result);
            }
        }
        return result;
    }

}
