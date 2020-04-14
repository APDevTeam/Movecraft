package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.ICraft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
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
        if (!SignUtils.isSign(block)) {
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
            final BlockFace direction;
            if (Settings.IsLegacy){
                if (sign.getData() instanceof org.bukkit.material.Sign){
                    org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign.getData();
                    if (signData.isWallSign()) {
                        direction = signData.getAttachedFace();
                    } else {
                        direction = signData.getFacing().getOppositeFace();
                    }
                }else {
                    direction = null;
                }
            } else {
                if (sign.getBlockData() instanceof org.bukkit.block.data.type.Sign){
                    org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign)sign.getBlockData();
                    direction = signData.getRotation().getOppositeFace();
                }else if (sign.getBlockData() instanceof org.bukkit.block.data.type.WallSign){
                    WallSign signData = (WallSign)sign.getBlockData();
                    direction = signData.getFacing().getOppositeFace();
                } else {
                    direction = null;
                }
            }
            c.setCruiseDirection(direction);
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    c.sink();
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
        Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(c, CraftPilotEvent.Reason.PLAYER));
        event.setCancelled(true);

    }
}
