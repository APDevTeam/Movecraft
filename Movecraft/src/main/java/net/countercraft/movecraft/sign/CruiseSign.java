package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.Nullable;

public class CruiseSign extends AbstractCruiseSign {

    public CruiseSign() {
        super("movecraft.cruisesign", true, "ON", "OFF");
    }

    @Override
    protected void setCraftCruising(Player player, CruiseDirection direction) {

    }

    @Override
    protected CruiseDirection getCruiseDirection(AbstractSignListener.SignWrapper sign) {
        BlockFace face = sign.facing();
        face = face.getOppositeFace();
        return CruiseDirection.fromBlockFace(face);
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (super.isSignValid(clickType, sign, player)) {
            switch(sign.facing()) {
                case NORTH:
                case EAST:
                case SOUTH:
                case WEST:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    @Override
    protected void onAfterStoppingCruise(Craft craft, AbstractSignListener.SignWrapper signWrapper, Player player) {
        super.onAfterStoppingCruise(craft, signWrapper, player);
        if (!craft.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
            CraftManager.getInstance().addReleaseTask(craft);
        }
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        // Ignore
    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {

    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            return craft.getType().getBoolProperty(CraftType.CAN_CRUISE);
        }
        return false;
    }
}
