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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class NameSign implements Listener {
    private static final String HEADER = "Name:";
    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event) {
        Craft c = event.getCraft();

        if (c instanceof PilotedCraft) {
            PilotedCraft pilotedCraft = (PilotedCraft) c;
            if (Settings.RequireNamePerm && !pilotedCraft.getPilot().hasPermission("movecraft.name.place"))
                return;
        }

        World w = c.getWorld();

        for (MovecraftLocation location : c.getHitBox()) {
            var block = location.toBukkit(w).getBlock();
            if(!Tag.SIGNS.isTagged(block.getType())){
                continue;
            }
            BlockState state = block.getState();
            if (!(state instanceof Sign)) {
                return;
            }
            Sign sign = (Sign) state;
            if (sign.getLine(0).equalsIgnoreCase(HEADER)) {
                String name = Arrays.stream(sign.getLines()).skip(1).filter(f -> f != null
                        && !f.trim().isEmpty()).collect(Collectors.joining(" "));
                c.setName(name);
                return;
            }
        }
    }

    @EventHandler
    public void onSignChange(@NotNull SignChangeEvent event) {
        if (HEADER.equalsIgnoreCase(event.getLine(0))
                && Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name.place")) {
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClickEvent(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
    }
}
