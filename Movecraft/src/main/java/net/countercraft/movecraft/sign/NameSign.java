package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.util.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
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
    protected boolean canPlayerUseSign(Action clickType, Sign sign, Player player) {
        return !Settings.RequireNamePerm || super.canPlayerUseSign(clickType, sign, player);
    }

    @Override
    protected boolean isSignValid(Action clickType, Sign sign, Player player) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, Sign sign, Player player, Craft craft) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, Sign sign, Player player, Optional<Craft> craft) {
        return true;
    }

    @Override
    protected void onParentCraftBusy(Player player, Craft craft) {
    }

    @Override
    protected void onCraftNotFound(Player player, Sign sign) {
    }

    @Override
    public boolean processSignChange(SignChangeEvent event) {
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
    public void onCraftDetect(CraftDetectEvent event, Sign sign) {
        Craft craft = event.getCraft();
        if (craft != null && craft instanceof PilotedCraft pc) {
            if (Settings.RequireNamePerm && !pc.getPilot().hasPermission(NAME_SIGN_PERMISSION))
                return;
        }

        craft.setName(Arrays.stream(sign.getLines()).skip(1).filter(f -> f != null
                && !f.trim().isEmpty()).collect(Collectors.joining(" ")));
    }
}
