package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.craft.ICraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
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

import java.util.HashMap;
import java.util.Map;

public final class SubcraftRotateSign implements Listener {
    private static final String HEADER = "Subcraft Rotate";
    private final Map<Craft,Rotation> craftRotations = new HashMap<>();
    private final Map<Craft,MovecraftLocation> craftRotationOrigins = new HashMap<>();

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

        final Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if(craft!=null) {
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
        }
        final Location loc = event.getClickedBlock().getLocation();
        final Craft subCraft = new ICraft(type, loc.getWorld());
        MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        subCraft.detect(null, event.getPlayer(), startPoint);
        craftRotations.put(subCraft,rotation);
        craftRotationOrigins.put(subCraft, MathUtils.bukkit2MovecraftLoc(loc));
        //timeMap.put(event.getPlayer(), System.currentTimeMillis());
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraftRelease(CraftReleaseEvent event){

    }

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        Craft craft = event.getCraft();
        if(!craftRotations.containsKey(craft)){
            return;
        }
        Rotation rotation = craftRotations.remove(craft);
        MovecraftLocation origin = craftRotationOrigins.remove(craft);
        new BukkitRunnable() {
            @Override
            public void run() {
                CraftManager.getInstance().removeCraft(craft);
            }
        }.runTaskLater(Movecraft.getInstance(), (3));

        new BukkitRunnable() {
            @Override
            public void run() {
                craft.rotate(rotation, origin, true);
            }
        }.runTaskLater(Movecraft.getInstance(), (1));
    }
}
