package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMovecraftSign {

    private static final Map<String, AbstractMovecraftSign> SIGNS = Collections.synchronizedMap(new HashMap<>());

    public static boolean hasBeenRegistered(final String ident) {
        return SIGNS.containsKey(ident);
    }

    public static Optional<AbstractMovecraftSign> tryGet(final String ident) {
        String identToUse = ident.toUpperCase();
        if (identToUse.indexOf(":") >= 0) {
            identToUse = identToUse.split(":")[0];
        }
        return Optional.ofNullable(SIGNS.getOrDefault(identToUse, null));
    }

    public static void forceRegister(final String ident, final @Nonnull AbstractMovecraftSign instance) {
        register(ident, instance, true);
    }

    public static void register(final String ident, final @Nonnull AbstractMovecraftSign instance, boolean allowOverride) {
        if (allowOverride) {
            SIGNS.put(ident.toUpperCase(), instance);
        } else {
            SIGNS.putIfAbsent(ident.toUpperCase(), instance);
        }
    }

    protected final Optional<String> optPermission;

    public AbstractMovecraftSign() {
        this(null);
    }

    public AbstractMovecraftSign(String permissionNode) {
        this.optPermission = Optional.ofNullable(permissionNode);
    }

    // Return true to cancel the event
    public boolean processSignClick(Action clickType, Sign sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }

        return internalProcessSign(clickType, sign, player, getCraft(sign));
    }

    protected boolean canPlayerUseSign(Action clickType, Sign sign, Player player) {
        if (this.optPermission.isPresent()) {
            return player.hasPermission(this.optPermission.get());
        }
        return true;
    }

    protected Optional<Craft> getCraft(Sign sign) {
        return Optional.ofNullable(MathUtils.getCraftByPersistentBlockData(sign.getLocation()));
    }

    public abstract boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking);
    protected abstract boolean isSignValid(Action clickType, Sign sign, Player player);
    protected abstract boolean internalProcessSign(Action clickType, Sign sign, Player player, Optional<Craft> craft);
    public abstract boolean processSignChange(SignChangeEvent event);

}
