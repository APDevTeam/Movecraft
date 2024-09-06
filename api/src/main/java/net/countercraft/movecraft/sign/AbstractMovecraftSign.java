package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

// DONE: In 1.21 signs can have multiple sides! This requires us to pass the clicked side through or well the relevant lines and the set method for the clicked side => Resolved using the SignWrapper
/*
 * Base class for all signs
 *
 * A instance of a sign needs to be registered using the register function.
 * Signs react to the following events:
 *  - SignChangeEvent
 *  - PlayerInteractEvent, if the clicked block is a sign
 *  - CraftDetectEvent
 *  - SignTranslateEvent (if the sign is a subclass of AbstractCraftSign)
 *
 *  Whenenver one of those events are cought by the AbstractSignListener instance, it is attempted to retrieve the relevant AbstractMovecraftSign instance.
 *  For that, the first line of the sign's clicked side is extracted and formatting removed. If it matches the format "foo: bar", only "foo:" will be used.
 *  With that ident, the sign is attempted to be retrieved vy tryGet(). If that returns something, the object's relevant method is called.
 */
public abstract class AbstractMovecraftSign {

    private static final Map<String, AbstractMovecraftSign> SIGNS = Collections.synchronizedMap(new HashMap<>());

    public static boolean hasBeenRegistered(final String ident) {
        return SIGNS.containsKey(ident);
    }

    // Special case for pilot signs, they are registered via the crafttypes name
    public static void registerCraftPilotSigns(Set<CraftType> loadedTypes, Function<CraftType, AbstractCraftPilotSign> signFactory) {
        SIGNS.entrySet().removeIf(entry ->  entry.getValue() instanceof AbstractCraftPilotSign);
        // Now, add all types...
        for (CraftType type : loadedTypes) {
            AbstractCraftPilotSign sign = signFactory.apply(type);
            register(type.getStringProperty(CraftType.NAME), sign, true);
        }
    }

    public static Optional<AbstractMovecraftSign> tryGet(final Component ident) {
        if (ident == null) {
            return Optional.empty();
        }
        final String identStr = PlainTextComponentSerializer.plainText().serialize(ident);
        return tryGet(identStr);
    }

    // Attempts to find a AbstractMovecraftSign instance, if something has been registered
    // If the ident follows the format "foo: bar", only "foo:" is used as ident to search for
    public static Optional<AbstractMovecraftSign> tryGet(final String ident) {
        String identToUse = ident.toUpperCase();
        if (identToUse.contains(":")) {
            identToUse = identToUse.split(":")[0];
            // Re-add the : cause things should be registered with : at the end
            identToUse = identToUse + ":";
        }
        return Optional.ofNullable(SIGNS.getOrDefault(identToUse, null));
    }

    // Registers a sign in all cases
    public static void register(final String ident, final @Nonnull AbstractMovecraftSign instance) {
        register(ident, instance, true);
    }

    // Registers a sign
    // If @param overrideIfAlreadyRegistered is set to false, it won't be registered if something has elready been registered using that name
    public static void register(final String ident, final @Nonnull AbstractMovecraftSign instance, boolean overrideIfAlreadyRegistered) {
        if (overrideIfAlreadyRegistered) {
            SIGNS.put(ident.toUpperCase(), instance);
        } else {
            SIGNS.putIfAbsent(ident.toUpperCase(), instance);
        }
    }

    // Optional permission for this sign
    // Note that this is only checked against in normal processSignClick by default
    // When using the default constructor, the permission will not be set
    @Nullable
    protected final String permissionString;

    public AbstractMovecraftSign() {
        this(null);
    }

    public AbstractMovecraftSign(String permissionNode) {
        this.permissionString = permissionNode;
    }

    // Utility function to retrieve the ident of a a given sign instance
    // DO NOT call this for unregistered instances!
    // It is a good idea to cache the return value of this function cause otherwise a loop over all registered sign instances will be necessary
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

    // Called whenever a player clicks the sign
    // SignWrapper wraps the relevant clicked side of the sign and the sign block itself
    // If true is returned, the event will be cancelled
    public boolean processSignClick(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }

        return internalProcessSign(clickType, sign, player, getCraft(sign));
    }

    // Validation method
    // By default this checks if the player has the set permission
    protected boolean canPlayerUseSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (this.permissionString == null || this.permissionString.isBlank()) {
            return true;
        }
        return player.hasPermission(this.permissionString);
    }

    // Helper method, simply calls the existing methods
    @Nullable
    protected Craft getCraft(AbstractSignListener.SignWrapper sign) {
        return MathUtils.getCraftByPersistentBlockData(sign.block().getLocation());
    }

    public enum EventType {
        SIGN_CREATION,
        SIGN_EDIT,
        SIGN_EDIT_ON_CRAFT(true),
        SIGN_CLICK,
        SIGN_CLICK_ON_CRAFT(true);

        private boolean onCraft;

        EventType() {
            this(false);
        }

        EventType(boolean onCraft) {
            this.onCraft = onCraft;
        }
    }

    // Used by the event handler to determine if the event should be cancelled
    // processingSuccessful is the output of processSignClick() or processSignChange()
    // This is only called for the PlayerInteractEvent and the SignChangeEvent
    public abstract boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking, EventType eventType);

    // Validation method, called by default in processSignClick
    // If false is returned, nothing will be processed
    protected abstract boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player);

    // Called by processSignClick after validation. At this point, isSignValid() and canPlayerUseSign() have been called already
    // If the sign belongs to a craft, that craft is given in the @param craft argument
    // Return true, if everything was ok
    protected abstract boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @Nullable Craft craft);

    // Called by the event handler when SignChangeEvent is being cought
    // Return true, if everything was ok
    public abstract boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign);
}
