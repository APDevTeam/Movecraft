package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
<<<<<<< HEAD
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.craft.CraftType;
import net.countercraft.movecraft.api.config.Settings;
=======
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.config.Settings;
>>>>>>> upstream/master
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.ICraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class CraftSign implements Listener{

    @EventHandler
    public void onSignChange(SignChangeEvent event){

        if (CraftManager.getInstance().getCraftTypeFromString(event.getLine(0)) == null) {
            return;
        }
        if (!Settings.RequireCreatePerm) {
            return;
        }
        if (!event.getPlayer().hasPermission("movecraft." + ChatColor.stripColor(event.getLine(0)) + ".create")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        CraftType type = CraftManager.getInstance().getCraftTypeFromString(ChatColor.stripColor(sign.getLine(0)));
        if (type == null) {
            return;
        }
        // Valid sign prompt for ship command.
        if (!event.getPlayer().hasPermission("movecraft." + ChatColor.stripColor(sign.getLine(0)) + ".pilot")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        // Attempt to run detection
        Location loc = event.getClickedBlock().getLocation();
        MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        final Craft c = new ICraft(type, loc.getWorld());

        if (c.getType().getCruiseOnPilot()) {
            c.detect(null, event.getPlayer(), startPoint);
            c.setCruiseDirection(sign.getRawData());
            c.setLastCruisUpdate(System.currentTimeMillis());
            c.setCruising(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    CraftManager.getInstance().removeCraft(c);
                }
            }.runTaskLater(Movecraft.getInstance(), (20 * 15));
        } else {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                c.detect(event.getPlayer(), event.getPlayer(), startPoint);
            } else {
                Craft oldCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
                if (oldCraft.isNotProcessing()) {
                    CraftManager.getInstance().removeCraft(oldCraft);
                    c.detect(event.getPlayer(), event.getPlayer(), startPoint);
                }
            }
        }
        event.setCancelled(true);

    }
}
