package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MathUtils;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.craft.CraftType;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class SubcraftRotateSign implements Listener {
    private static final String HEADER = "Subcraft Rotate";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        Rotation rotation;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            rotation = Rotation.CLOCKWISE;
        }else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
            rotation = Rotation.ANTICLOCKWISE;
        }else{
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }

        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Subcraft Rotate")) {
            // rotate subcraft
            String craftTypeStr = ChatColor.stripColor(sign.getLine(1));
            CraftType type = CraftManager.getInstance().getCraftTypeFromString(craftTypeStr);
            if (type == null) {
                return;
            }
            if (ChatColor.stripColor(sign.getLine(2)).equals("")
                    && ChatColor.stripColor(sign.getLine(3)).equals("")) {
                sign.setLine(2, "_\\ /_");
                sign.setLine(3, "/ \\");
                sign.update(false, false);
            }

            if (!event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".pilot") || !event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".rotate")) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return;
            }
            /*Long time = timeMap.get(event.getPlayer());
            if (time != null && Math.abs((System.currentTimeMillis() - time) / 50) < type.getTickCooldown()) {
                event.setCancelled(true);
                return;
            }*/
            final Location loc = event.getClickedBlock().getLocation();
            final Craft c = new ICraft(type, loc.getWorld());
            MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            // find the craft this is a subcraft of, and set it to processing so it doesn't move
            Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld(event.getClickedBlock().getWorld());
            if (craftsInWorld != null) {
                Outer:
                for (Craft craft : craftsInWorld) {
                    for (MovecraftLocation mLoc : craft.getBlockList()) {
                        if (mLoc.equals(startPoint)) {
                            // found a parent craft
                            if (!craft.isNotProcessing()) {
                                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Parent Craft is busy"));
                                return;
                            }
                            craft.setProcessing(true); // prevent the parent craft from moving or updating until the subcraft is done
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    craft.setProcessing(false);
                                }
                            }.runTaskLater(Movecraft.getInstance(), (10));
                            break Outer;
                        }
                    }
                }
            }
            c.detect(null, event.getPlayer(), startPoint);
            new BukkitRunnable() {
                @Override
                public void run() {
                    CraftManager.getInstance().removeCraft(c);
                }
            }.runTaskLater(Movecraft.getInstance(), (10));

            new BukkitRunnable() {
                @Override
                public void run() {
                    c.rotate(rotation, MathUtils.bukkit2MovecraftLoc(loc), true);
                }
            }.runTaskLater(Movecraft.getInstance(), (5));
            //timeMap.put(event.getPlayer(), System.currentTimeMillis());
            event.setCancelled(true);
        }
    }
}
