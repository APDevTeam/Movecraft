package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.property.*;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class CraftProperties {

    private final WeakReference<CraftType> craftTypeWeakReference;
    private final WeakReference<Craft> craftWeakReference;

    private final Map<Property<?>, PropertyInstance<?>> propertyMap = new HashMap<>();

    public CraftProperties(final CraftType craftType, final Craft craft) {
        this.craftTypeWeakReference = new WeakReference<>(craftType);
        this.craftWeakReference = new WeakReference<>(craft);
    }

    @Nullable
    public Property<?> getPropByID(final NamespacedKey key) {
        return this.craftTypeWeakReference.get().properties.getOrDefault(key, null);
    }

    protected <T> PropertyInstance<T> getOrCreatePropertyInstance(final Property<T> property) {
        PropertyInstance<T> propertyInstance = (PropertyInstance<T>) this.propertyMap.computeIfAbsent(property, p -> PropertyInstance.of(p, this.craftTypeWeakReference.get()));
        return propertyInstance;
    }

    public <T> boolean set(final Property<T> property, final T value) {
        return getOrCreatePropertyInstance(property).set(this.craftWeakReference.get().getWorld().getName(), value);
    }

    @Nullable
    public <T> T getValue(final Property<T> property) throws ClassCastException {
        return this.getOrCreatePropertyInstance(property).getValue(this.craftWeakReference.get().getWorld().getName());
    }

}
