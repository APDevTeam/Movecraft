package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.SimpleRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

public class MovecraftSignRegistry extends SimpleRegistry<String, AbstractMovecraftSign> {

    public static final @NotNull MovecraftSignRegistry INSTANCE = new MovecraftSignRegistry();

    public void registerCraftPilotSigns(Set<CraftType> loadedTypes, Function<CraftType, AbstractCraftPilotSign> signFactory) {
        _register.entrySet().removeIf(entry ->  entry.getValue() instanceof AbstractCraftPilotSign);
        // Now, add all types...
        for (CraftType type : loadedTypes) {
            AbstractCraftPilotSign sign = signFactory.apply(type);
            register(type.getStringProperty(CraftType.NAME), sign, true);
        }
    }

    public @NotNull AbstractMovecraftSign register(@NotNull String key, @NotNull AbstractMovecraftSign value, String... aliases) throws IllegalArgumentException {
        return register(key, value, false, aliases);
    }

    public @NotNull AbstractMovecraftSign register(@NotNull String key, @NotNull AbstractMovecraftSign value, boolean override, String... aliases) throws IllegalArgumentException {
        AbstractMovecraftSign result = this.register(key, value, override);
        for (String alias : aliases) {
            this.register(alias, result, override);
        }
        return result;
    }

    public @Nullable AbstractMovecraftSign get(Component key) {
        if (key == null) {
            return null;
        }
        final String identStr = PlainTextComponentSerializer.plainText().serialize(key);
        return get(identStr);
    }

    @Override
    public @Nullable AbstractMovecraftSign get(@NotNull String key) {
        String identToUse = key.toUpperCase();
        if (identToUse.contains(":")) {
            identToUse = identToUse.split(":")[0];
            // Re-add the : cause things should be registered with : at the end
            identToUse = identToUse + ":";
        }
        return super.get(identToUse);
    }

    // Helper method for the listener
    public @Nullable AbstractCraftSign getCraftSign(final Component ident) {
        if (ident == null) {
            return null;
        }
        final String identStr = PlainTextComponentSerializer.plainText().serialize(ident);
        return this.getCraftSign(identStr);
    }

    public @Nullable AbstractCraftSign getCraftSign(final String ident) {
        AbstractMovecraftSign tmp = this.get(ident);
        if (tmp != null && tmp instanceof AbstractCraftSign acs) {
            return acs;
        }
        return null;
    }


    @Override
    public @NotNull AbstractMovecraftSign register(@NotNull String key, @NotNull AbstractMovecraftSign value, boolean override) throws IllegalArgumentException {
        return super.register(key.toUpperCase(), value, override);
    }
}
