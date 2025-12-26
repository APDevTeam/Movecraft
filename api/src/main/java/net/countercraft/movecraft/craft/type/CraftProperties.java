package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.property.*;
import net.countercraft.movecraft.util.registration.TypedContainer;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

// Represents the runtime crafttype of a craft. Will copy each value of the crafttype when needed
public class CraftProperties extends TypeSafeCraftType{

    private final TypeSafeCraftType craftTypeReference;
    private final WeakReference<Craft> craftWeakReference;

    public CraftProperties(final TypeSafeCraftType craftType, final Craft craft) {
        super(craftType.parentName, craftType.typeRetriever);

        this.craftTypeReference = craftType;
        this.craftWeakReference = new WeakReference<>(craft);
    }

    @Override
    protected <T> void set(@NotNull TypedKey<T> key, @NotNull T value) {
        if (key instanceof PropertyKey.ImmutableKey) {
            // TODO: Log warning
            return;
        }
        super.set(key, value);
    }

    @Override
    public <T> T get(@NotNull PropertyKey<T> key) {
        if (key instanceof PropertyKey.ImmutableKey) {
            return this.craftTypeReference.get(key);
        }
        return super.get(key);
    }

    @Override
    public String getName() {
        return this.craftTypeReference.getName();
    }

    @Override
    public CraftProperties createCraftProperties(final Craft craft) {
        return this;
    }
}
