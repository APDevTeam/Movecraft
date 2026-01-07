package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.registration.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

// Represents the runtime crafttype of a craft. Will copy each value of the crafttype when needed
public class CraftProperties extends TypeSafeCraftType{

    private final TypeSafeCraftType craftTypeReference;
    private final WeakReference<Craft> craftWeakReference;

    public CraftProperties(final TypeSafeCraftType craftType, final Craft craft) {
        // Change typeretriever to directly reroute back to the backing type for this case!
        super(craftType.parentName, (s) -> craftType);

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
        // If this property is immutable or we dont have it in ourselves (our parent is the underlying crafttype itself, so it's result does not concern us), we return the backing's value
        if ((key instanceof PropertyKey.ImmutableKey) || !this.has(key)) {
            return this.craftTypeReference.get(key);
        }
        T result = super.get(key);
        if (result == null) {
            result = this.craftTypeReference.get(key);
        }
        return result;
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
