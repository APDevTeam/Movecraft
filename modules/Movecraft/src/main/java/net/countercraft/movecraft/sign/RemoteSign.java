package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class RemoteSign implements Listener{
    private static final String HEADER = "Remote Sign";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
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
        Craft foundCraft = null;
        CraftManager.getInstance().getCraftsInWorld(event.getClickedBlock().getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(event.getClickedBlock().getWorld())) {
            if (MathUtils.locationInHitbox(tcraft.getHitBox(), event.getClickedBlock().getLocation())) {
                // don't use a craft with a null player. This is
                // mostly to avoid trying to use subcrafts
                if (CraftManager.getInstance().getPlayerFromCraft(tcraft) != null) {
                    foundCraft = tcraft;
                    break;
                }
            }
        }

        if (foundCraft == null) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Remote Sign must be a part of a piloted craft!"));
            return;
        }

        if (!foundCraft.getType().allowRemoteSign()) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Remote Signs not allowed on this craft!"));
            return;
        }

        String targetText = ChatColor.stripColor(sign.getLine(1));
        if(targetText.equalsIgnoreCase(HEADER)) {
            event.getPlayer().sendMessage("ERROR: Remote Sign can't remote another Remote Sign!");
            return;
        }
        MovecraftLocation foundLoc = null;
        for (MovecraftLocation tloc : foundCraft.getHitBox()) {
            Block tb = event.getClickedBlock().getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ());
            if (!tb.getType().equals(Material.SIGN_POST) && !tb.getType().equals(Material.WALL_SIGN)) {
                continue;
            }
            Sign ts = (Sign) tb.getState();
            if (ChatColor.stripColor(ts.getLine(0)).equalsIgnoreCase(targetText))
                foundLoc = tloc;
            if (ChatColor.stripColor(ts.getLine(1)).equalsIgnoreCase(targetText)) {
                if (ChatColor.stripColor(ts.getLine(0)).equalsIgnoreCase(HEADER))
                    continue;
                foundLoc = tloc;
            }
            if (ChatColor.stripColor(ts.getLine(2)) != null)
                if (ChatColor.stripColor(ts.getLine(2)).equalsIgnoreCase(targetText))
                    foundLoc = tloc;
            if (ChatColor.stripColor(ts.getLine(3)) != null)
                if (ChatColor.stripColor(ts.getLine(3)).equalsIgnoreCase(targetText))
                    foundLoc = tloc;
        }
        if (foundLoc == null) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Could not find target sign!"));
            return;
        }

        Block newBlock = event.getClickedBlock().getWorld().getBlockAt(foundLoc.getX(), foundLoc.getY(),
                foundLoc.getZ());
        PlayerInteractEvent newEvent = new PlayerInteractEvent(event.getPlayer(), event.getAction(),
                event.getItem(), newBlock, event.getBlockFace());
        //TODO: DON'T DO THIS
        Bukkit.getServer().getPluginManager().callEvent(newEvent);
        event.setCancelled(true);
    }
}
