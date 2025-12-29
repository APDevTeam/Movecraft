package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

// TODO: Unify with AscendSign to use a common "VerticalCruiseSign" class
public class DescendSign extends AbstractCruiseSign {

    public DescendSign(final String ident) {
        super(true, ident, "ON", "OFF");
    }

    @Override
    protected void setCraftCruising(Player player, CruiseDirection direction, Craft craft) {
        craft.setCruiseDirection(direction);
        craft.setLastCruiseUpdate(System.currentTimeMillis());
        craft.setCruising(true);
    }

    @Override
    protected CruiseDirection getCruiseDirection(SignListener.SignWrapper sign) {
        return CruiseDirection.DOWN;
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        // Ignore
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {

    }

    @Override
    protected void onAfterStoppingCruise(Craft craft, SignListener.SignWrapper signWrapper, Player player) {
        if (!craft.getCraftProperties().get(PropertyKeys.CAN_MOVE_ENTITIES)) {
            CraftManager.getInstance().addReleaseTask(craft);
        }
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            return craft.getCraftProperties().get(PropertyKeys.CAN_CRUISE);
        }
        return false;
    }
}
