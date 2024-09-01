package net.countercraft.movecraft.sign;

import jakarta.inject.Inject;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class ReleaseSign implements Listener{
    private static final String HEADER = "Release";
    private final @NotNull CraftManager craftManager;

    @Inject
    public ReleaseSign(@NotNull CraftManager craftManager){
        this.craftManager = craftManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
        Craft craft = craftManager.getCraftByPlayer(event.getPlayer());
        if (craft == null) {
            return;
        }
        craftManager.release(craft, CraftReleaseEvent.Reason.PLAYER, false);
    }
}
