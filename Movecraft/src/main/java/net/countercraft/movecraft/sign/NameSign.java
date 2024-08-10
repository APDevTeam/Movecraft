package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.util.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class NameSign extends AbstractCraftSign {

    public static final String NAME_SIGN_PERMISSION = "movecraft.name.place";

    public NameSign() {
        super(NAME_SIGN_PERMISSION, true);
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        if (type == null) {
            return !processingSuccessful;
        }
        return !sneaking;
    }

    @Override
    protected boolean canPlayerUseSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return !Settings.RequireNamePerm || super.canPlayerUseSign(clickType, sign, player);
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Craft craft) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Optional<Craft> craft) {
        return true;
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        if (this.canPlayerUseSign(Action.RIGHT_CLICK_BLOCK, null, event.getPlayer())) {
            // Nothing to do
            return true;
        } else {
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
            return false;
        }
    }

    @Override
    public void onCraftDetect(CraftDetectEvent event, AbstractSignListener.SignWrapper sign) {
        Craft craft = event.getCraft();
        if (craft != null && craft instanceof PilotedCraft pc) {
            if (Settings.RequireNamePerm && !pc.getPilot().hasPermission(NAME_SIGN_PERMISSION))
                return;
        }

        craft.setName(Arrays.stream(sign.rawLines()).skip(1).filter(f -> f != null
                && !f.trim().isEmpty()).collect(Collectors.joining(" ")));
    }
}
