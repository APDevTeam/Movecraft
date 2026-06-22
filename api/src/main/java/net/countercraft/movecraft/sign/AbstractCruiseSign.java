package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.entity.Player;

/*
 * Base class for all cruise signs
 *
 * Has the relevant logic for the "state" suffix (on / off) as well as calling the relevant methods and setting the craft to cruising
 *
 */
public abstract class AbstractCruiseSign extends AbstractToggleSign {

    public AbstractCruiseSign(boolean ignoreCraftIsBusy, String ident, String suffixOn, String suffixOff) {
        super(ignoreCraftIsBusy, ident, suffixOn, suffixOff);
    }

    public AbstractCruiseSign(final String permission, boolean ignoreCraftIsBusy, String ident, String suffixOn, String suffixOff) {
        super(permission, ignoreCraftIsBusy, ident, suffixOn, suffixOff);
    }

    // Hook to do stuff that run after stopping to cruise
    protected void onAfterStoppingCruise(Craft craft, SignListener.SignWrapper signWrapper, Player player) {

    }

    // Hook to do stuff that run after starting to cruise
    protected void onAfterStartingCruise(Craft craft, SignListener.SignWrapper signWrapper, Player player) {

    }

    @Override
    protected void onAfterToggle(Craft craft, SignListener.SignWrapper signWrapper, Player player, boolean toggledToOn) {
        if (toggledToOn) {
            this.onAfterStartingCruise(craft, signWrapper, player);
        } else {
            this.onAfterStoppingCruise(craft, signWrapper, player);
        }
    }

    @Override
    protected void onBeforeToggle(Craft craft, SignListener.SignWrapper signWrapper, Player player, boolean willBeOn) {
        if (willBeOn) {
            CruiseDirection cruiseDirection = this.getCruiseDirection(signWrapper);
            this.setCraftCruising(player, cruiseDirection, craft);
        } else {
            craft.setCruising(false);
        }
    }

    // Should call the craft's relevant methods to start cruising
    protected abstract void setCraftCruising(Player player, CruiseDirection direction, Craft craft);

    // TODO: Rework cruise direction to vectors => Vector defines the skip distance and the direction
    // Returns the direction in which the craft should cruise
    protected abstract CruiseDirection getCruiseDirection(SignListener.SignWrapper sign);
}
