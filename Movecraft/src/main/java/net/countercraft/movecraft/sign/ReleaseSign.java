package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

public class ReleaseSign extends AbstractMovecraftSign {

    public ReleaseSign() {
        super(null);
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking, EventType eventType) {
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
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @Nullable Craft craft) {
        if (craft == null) {
            craft = CraftManager.getInstance().getCraftByPlayer(player);
        }
        if (craft != null) {
            CraftManager.getInstance().release(craft, CraftReleaseEvent.Reason.PLAYER, false);
        }
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        return false;
    }

}
