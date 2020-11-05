package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.craft.ICraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class SubcraftRotateSign implements Listener {
    private static final String HEADER = "Subcraft Rotate";
    private final Set<MovecraftLocation> rotatingCrafts = new HashSet<>();
    private final Map<Craft, Rotation> rotationMap = new WeakHashMap<>();
    private final Map<Craft, MovecraftLocation> startPointMap = new WeakHashMap<>();

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
        if (!SignUtils.isSign(block)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        final Location loc = event.getClickedBlock().getLocation();
        final MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(loc);
        if(rotatingCrafts.contains(startPoint)){
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Already Rotating"));
            event.setCancelled(true);
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
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
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
        final Craft subCraft = new ICraft(type, loc.getWorld());
        rotationMap.put(subCraft, rotation);
        subCraft.detect(null, event.getPlayer(), startPoint);
        rotatingCrafts.add(startPoint);
        startPointMap.put(subCraft, startPoint);
        Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(subCraft, CraftPilotEvent.Reason.SUB_CRAFT));
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraftRelease(CraftReleaseEvent event){
        rotatingCrafts.removeAll(event.getCraft().getHitBox().asSet());
    }

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        if (!rotationMap.containsKey(event.getCraft()))
            return;
        final Craft subCraft = event.getCraft();
        final Rotation rotation = rotationMap.get(subCraft);
        final MovecraftLocation startPoint = startPointMap.get(subCraft);
        new BukkitRunnable() {
            @Override
            public void run() {
                subCraft.rotate(rotation, startPoint, true);
            }
        }.runTaskLater(Movecraft.getInstance(), 1);
    }

    @EventHandler
    public void onCraftRotate(CraftRotateEvent event) {
        if (!rotationMap.containsKey(event.getCraft()))
            return;
        final Craft subCraft = event.getCraft();
        rotationMap.remove(subCraft);
        final MovecraftLocation startPoint = startPointMap.remove(subCraft);
        new BukkitRunnable() {
            @Override
            public void run() {
                rotatingCrafts.remove(startPoint);
                CraftManager.getInstance().removeCraft(subCraft, CraftReleaseEvent.Reason.SUB_CRAFT);
            }
        }.runTaskLater(Movecraft.getInstance(), 1);
    }
}
