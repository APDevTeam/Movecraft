package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import javax.annotation.Nullable;
import java.util.Optional;

/*
 * Extension of @AbstractMovecraftSign
 * The difference is, that for this sign to work, it must exist on a craft instance
 *
 * Also this will react to the SignTranslate event
 */
public abstract class AbstractCraftSign extends AbstractMovecraftSign {

    // Helper method for the listener
    public static @Nullable AbstractCraftSign getCraftSign(final Component ident) {
        if (ident == null) {
            return null;
        }
        final String identStr = PlainTextComponentSerializer.plainText().serialize(ident);
        return getCraftSign(identStr);
    }

    public static @Nullable AbstractCraftSign getCraftSign(final String ident) {
        AbstractMovecraftSign tmp = AbstractCraftSign.get(ident);
        if (tmp != null && tmp instanceof AbstractCraftSign acs) {
            return acs;
        }
        return null;
    }

    protected final boolean ignoreCraftIsBusy;

    public AbstractCraftSign(boolean ignoreCraftIsBusy) {
        this(null, ignoreCraftIsBusy);
    }

    public AbstractCraftSign(final String permission, boolean ignoreCraftIsBusy) {
        super(permission);
        this.ignoreCraftIsBusy = ignoreCraftIsBusy;
    }

    // Similar to the super class variant
    // In addition a check for a existing craft is being made
    // If the craft is a player craft that is currently processing and ignoreCraftIsBusy is set to false, this will quit early and call onCraftIsBusy()
    // If no craft is found, onCraftNotFound() is called
    // Return true to cancel the event
    @Override
    public boolean processSignClick(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }
        Craft craft = this.getCraft(sign);
        if (craft == null) {
            this.onCraftNotFound(player, sign);
            return false;
        }

        if (craft instanceof PlayerCraft pc) {
            if (!pc.isNotProcessing() && !this.ignoreCraftIsBusy) {
                this.onCraftIsBusy(player, craft);
                return false;
            }
        }

        return internalProcessSign(clickType, sign, player, craft);
    }

    // Implementation of the standard method.
    // The craft instance is required here and it's existance is being confirmed in processSignClick() in beforehand
    // After that, canPlayerUseSignOn() is being called. If that is successful, the result of internalProcessSignWithCraft() is returned
    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @Nullable Craft craft) {
        if (craft == null) {
            throw new IllegalStateException("Somehow craft is not set here. It should always be present here!");
        }
        if (this.canPlayerUseSignOn(player, craft)) {
            return this.internalProcessSignWithCraft(clickType, sign, craft, player);
        }
        return false;
    }

    // Called when the craft is a player craft and is processing and ignoreCraftIsBusy is set to false
    protected abstract void onCraftIsBusy(Player player, Craft craft);

    // Validation method, intended to indicate if a player is allowed to execute a sign action on a mounted craft
    // By default, this returns wether or not the player is the pilot of the craft
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (craft instanceof PilotedCraft pc) {
            return pc.getPilot() == player;
        }
        return true;
    }

    // Called when there is no craft instance for this sign
    protected abstract void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign);

    // By default we don't react to CraftDetectEvent here
    public void onCraftDetect(CraftDetectEvent event, AbstractSignListener.SignWrapper sign) {
        // Do nothing by default
    }

    public void onSignMovedByCraft(SignTranslateEvent event) {
        // Do nothing by default
    }

    // Gets called by internalProcessSign if a craft is found
    // Always override this as the validation has been made already when this is being called
    protected abstract boolean internalProcessSignWithCraft(Action clickType, AbstractSignListener.SignWrapper sign, Craft craft, Player player);

}
