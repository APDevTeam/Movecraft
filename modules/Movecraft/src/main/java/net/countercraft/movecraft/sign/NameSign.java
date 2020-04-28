package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.ChatUtils;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

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
            Block b = location.toBukkit(w).getBlock();
            if (SignUtils.isSign(b)) {
                Sign s = (Sign) b.getState();
                String name = "";
                if (s.getLine(0).equalsIgnoreCase(HEADER)) {
                    boolean firstName = true;
                    for (int i = 1; i <= 3; i++) {
                        if (s.getLine(i) != "") {
                            if (firstName) {
                                firstName = true;
                            } else {
                                name += " ";
                                //Add a space between lines for all after the first.
                            }
                            name += s.getLine(i);
                        }
                    }
                    c.setName(name);
                    return;
                }
            }
        }
    }
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase(HEADER)) {
            if (Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name.place")) {
                event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
                event.setCancelled(true);
                return;
            }
        }
    }
}
