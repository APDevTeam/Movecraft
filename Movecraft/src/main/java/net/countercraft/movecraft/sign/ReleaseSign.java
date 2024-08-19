package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

public class ReleaseSign extends AbstractCraftSign {

    public ReleaseSign() {
        super(true);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {

    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        if (processingSuccessful) {
            return true;
        }
        return !sneaking;
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        return false;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, AbstractSignListener.SignWrapper sign, Craft craft, Player player) {
        CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.PLAYER, false);
        return true;
    }
}
