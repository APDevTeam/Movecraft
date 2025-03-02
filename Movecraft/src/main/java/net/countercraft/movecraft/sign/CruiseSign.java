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

    public CruiseSign(final String ident) {
        super("movecraft.cruisesign", true, ident,"ON", "OFF");
    }

    @Override
    protected void setCraftCruising(Player player, CruiseDirection direction, Craft craft) {
        craft.setCruiseDirection(direction);
        craft.setLastCruiseUpdate(System.currentTimeMillis());
        craft.setCruising(true);
    }

    @Override
    protected CruiseDirection getCruiseDirection(SignListener.SignWrapper sign) {
        BlockFace face = sign.facing();
        // NOt necessary, CruiseDirection#fromBlockFace already handles this!
        //face = face.getOppositeFace();
        return CruiseDirection.fromBlockFace(face);
    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
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
    protected void onAfterStoppingCruise(Craft craft, SignListener.SignWrapper signWrapper, Player player) {
        super.onAfterStoppingCruise(craft, signWrapper, player);
        // TODO: WTF?!
        if (!craft.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
            CraftManager.getInstance().addReleaseTask(craft);
        }
    }

    @Override
    protected boolean shouldShareSameToggleState(SignListener.SignWrapper sign, SignListener.SignWrapper other) {
        return super.shouldShareSameToggleState(sign, other) && sign.facing() == other.facing();
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        // Ignore
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {

    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            return craft.getType().getBoolProperty(CraftType.CAN_CRUISE);
        }
        return false;
    }
}
