package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.ISubCraft;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public final class SubcraftRotateSign implements Listener {
    private static final String HEADER = "Subcraft Rotate";
    private final Set<MovecraftLocation> rotatingCrafts = new HashSet<>();

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        MovecraftRotation rotation;
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
            rotation = MovecraftRotation.CLOCKWISE;
        else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
            rotation = MovecraftRotation.ANTICLOCKWISE;
        else
            return;

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign))
            return;
        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        final Location loc = event.getClickedBlock().getLocation();
        final MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if(rotatingCrafts.contains(startPoint)) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Already Rotating"));
            event.setCancelled(true);
            return;
        }

        // rotate subcraft
        String craftTypeStr = ChatColor.stripColor(sign.getLine(1));
        CraftType type = CraftManager.getInstance().getCraftTypeFromString(craftTypeStr);
        if(type == null)
            return;
        if(ChatColor.stripColor(sign.getLine(2)).equals("")
                && ChatColor.stripColor(sign.getLine(3)).equals("")) {
            sign.setLine(2, "_\\ /_");
            sign.setLine(3, "/ \\");
            sign.update(false, false);
        }

        if(!event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".pilot") || !event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".rotate")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        final Craft playerCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if(playerCraft != null) {
            if (!playerCraft.isNotProcessing()) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
                return;
            }
            playerCraft.setProcessing(true); // prevent the parent craft from moving or updating until the subcraft is done
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerCraft.setProcessing(false);
                }
            }.runTaskLater(Movecraft.getInstance(), (10));
        }

        rotatingCrafts.add(startPoint);

        if(!type.getBoolProperty(CraftType.MUST_BE_SUBCRAFT)) {
            event.getPlayer().sendMessage("This feature is not implemented yet.");
            return;
        }

        final ISubCraft craft = new ISubCraft(type, loc.getWorld());
        craft.detect(null, event.getPlayer(), startPoint);

        Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(craft, CraftPilotEvent.Reason.SUB_CRAFT));

        new BukkitRunnable() {
            @Override
            public void run() {
                craft.rotate(rotation, startPoint, true);
                var newHitbox = craft.getParent().getHitBox().union(craft.getHitBox());
                craft.getParent().setHitBox(newHitbox);
                rotatingCrafts.remove(startPoint);
                CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.SUB_CRAFT);
            }
        }.runTaskLater(Movecraft.getInstance(), 3);

        event.setCancelled(true);
    }
}
