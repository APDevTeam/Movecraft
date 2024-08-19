package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

// TODO: In 1.21 signs can have multiple sides! This requires us to pass the clicked side through or well the relevant lines and the set method for the clicked side
public abstract class AbstractMovecraftSign {

    private static final Map<String, AbstractMovecraftSign> SIGNS = Collections.synchronizedMap(new HashMap<>());

    public static boolean hasBeenRegistered(final String ident) {
        return SIGNS.containsKey(ident);
    }

    public static void registerCraftPilotSigns(Set<CraftType> loadedTypes, Function<CraftType, AbstractCraftPilotSign> signFactory) {
        SIGNS.entrySet().removeIf(entry ->  entry.getValue() instanceof AbstractCraftPilotSign);
        // Now, add all types...
        for (CraftType type : loadedTypes) {
            AbstractCraftPilotSign sign = signFactory.apply(type);
            register(type.getStringProperty(CraftType.NAME), sign, true);
        }
    }

    public static Optional<AbstractMovecraftSign> tryGet(final String ident) {
        String identToUse = ident.toUpperCase();
        if (identToUse.contains(":")) {
            identToUse = identToUse.split(":")[0];
            // Re-add the : cause things should be registered with : at the end
            identToUse = identToUse + ":";
        }
        return Optional.ofNullable(SIGNS.getOrDefault(identToUse, null));
    }

    public static void register(final String ident, final @Nonnull AbstractMovecraftSign instance) {
        register(ident, instance, true);
    }

    public static void register(final String ident, final @Nonnull AbstractMovecraftSign instance, boolean overrideIfAlreadyRegistered) {
        if (overrideIfAlreadyRegistered) {
            SIGNS.put(ident.toUpperCase(), instance);
        } else {
            SIGNS.putIfAbsent(ident.toUpperCase(), instance);
        }
    }

    @Nullable
    protected final String permissionString;

    public AbstractMovecraftSign() {
        this(null);
    }

    public AbstractMovecraftSign(String permissionNode) {
        this.permissionString = permissionNode;
    }

    public static String findIdent(AbstractMovecraftSign instance) {
        if (!SIGNS.containsValue(instance)) {
            throw new IllegalArgumentException("MovecraftSign instance must be registered!");
        }
        for (Map.Entry<String, AbstractMovecraftSign> entry : SIGNS.entrySet()) {
            if (entry.getValue() == instance) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Somehow didn't find a key for a value that is in the map!");
    }

    // Return true to cancel the event
    public boolean processSignClick(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }

        return internalProcessSign(clickType, sign, player, getCraft(sign));
    }

    protected boolean canPlayerUseSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (this.permissionString == null || this.permissionString.isBlank()) {
            return true;
        }
        return player.hasPermission(this.permissionString);
    }

    @Nullable
    protected Craft getCraft(AbstractSignListener.SignWrapper sign) {
        return MathUtils.getCraftByPersistentBlockData(sign.block().getLocation());
    }

    public abstract boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking);
    protected abstract boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player);
    protected abstract boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @Nullable Craft craft);
    public abstract boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign);
}
