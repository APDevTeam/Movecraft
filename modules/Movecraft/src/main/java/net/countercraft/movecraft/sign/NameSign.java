package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.util.ChatUtils;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
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
                String name = Arrays.stream(sign.getLines()).skip(1).filter(f -> f != null && !f.trim().isEmpty()).collect(Collectors.joining(" "));
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
