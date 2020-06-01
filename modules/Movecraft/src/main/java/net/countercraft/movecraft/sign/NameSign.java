package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class NameSign implements Listener {
    private static final String HEADER = "Name:";
    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event) {
        Craft c = event.getCraft();

        if(c.getNotificationPlayer() == null || (Settings.RequireNamePerm && !c.getNotificationPlayer().hasPermission("movecraft.name.use"))) {
            //Player is null or does not have permission (when required)
            return;
        }

        World w = c.getW();

        for (MovecraftLocation location : c.getHitBox()) {
            Block b = location.toBukkit(w).getBlock();
            if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
                continue;
            }
            Sign s = (Sign) b.getState();
            if (s.getLine(0).equalsIgnoreCase(HEADER)) {
                String name = Arrays.stream(s.getLines()).skip(1).filter(f -> f != null && !f.trim().isEmpty()).collect(Collectors.joining(" "));
                c.setName(name);
                return;
            }
        }
    }
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase(HEADER) && Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name.place")) {
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
        }
    }
}
