package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.ChatUtils;
import net.countercraft.movecraft.utils.LegacyUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

public final class NameSign implements Listener {
    String HEADER = "Name:";
    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event){
        Craft c = event.getCraft();
        World w = c.getW();
        for (MovecraftLocation location : event.getCraft().getHitBox()){
            Block b = location.toBukkit(w).getBlock();
            if (b.getType() == (Settings.IsLegacy ? LegacyUtils.SIGN_POST : Material.SIGN) || b.getType() == Material.WALL_SIGN){
                Sign s = (Sign) b.getState();
                String name = "";
                if (s.getLine(0).equalsIgnoreCase(HEADER)){
                    if (s.getLine(1) != null){
                        name += s.getLine(1) + " "; //reserved for prefix (HMA, SS, MS, USS, HMS, etc...)
                    }
                    if (s.getLine(2) != null){ // lines 2 and 3 are for the name
                        name += s.getLine(2);
                    }
                    if (s.getLine(3) != null){
                        name += s.getLine(3);
                    }
                    c.setUniqueName(name);

                }
            }
        }
    }
    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if (!event.getLine(0).equalsIgnoreCase(HEADER)){
            return;
        }
        if (Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name")){
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
            return;
        }
    }
}
