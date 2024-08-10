package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

// TODO: Unify with AscendSign to use a common "VerticalCruiseSign" class
public class DescendSign extends AbstractCruiseSign {

    public DescendSign() {
        super(true, "ON", "OFF");
    }

    @Override
    protected void setCraftCruising(Player player, CruiseDirection direction, Craft craft) {
        craft.setCruiseDirection(direction);
        craft.setLastCruiseUpdate(System.currentTimeMillis());
        craft.setCruising(true);
    }

    @Override
    protected CruiseDirection getCruiseDirection(AbstractSignListener.SignWrapper sign) {
        return CruiseDirection.DOWN;
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        // Ignore
    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {

    }

    @Override
    protected void onAfterStoppingCruise(Craft craft, AbstractSignListener.SignWrapper signWrapper, Player player) {
        if (!craft.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
            CraftManager.getInstance().addReleaseTask(craft);
        }
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            return craft.getType().getBoolProperty(CraftType.CAN_CRUISE);
        }
        return false;
    }
}
