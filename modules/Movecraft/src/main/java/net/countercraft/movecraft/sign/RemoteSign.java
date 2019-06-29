package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.LinkedList;

import static net.countercraft.movecraft.utils.ChatUtils.ERROR_PREFIX;

public final class RemoteSign implements Listener{
    private static final String HEADER = "Remote Sign";

    @EventHandler
    public final void onSignChange(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase(HEADER)) {
            return;
        }
        else if(event.getLine(1).equals("")) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Signs can't be blank!"));
            event.setLine(0,"");
            event.setLine(2,"");
            event.setLine(3,"");
            return;
        }
    }

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
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign must be a part of a piloted craft!"));
            return;
        }

        if (!foundCraft.getType().allowRemoteSign()) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Signs not allowed on this craft!"));
            return;
        }

        String targetText = ChatColor.stripColor(sign.getLine(1));
        if(targetText.equalsIgnoreCase(HEADER)) {
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign cannot remote another Remote Sign!"));
            return;
        }

        if(targetText.equalsIgnoreCase("")) {
            event.getPlayer().sendMessage("ERROR: Remote Signs can't be blank!");
            return;
        }

        LinkedList<MovecraftLocation> foundLocations = new LinkedList<MovecraftLocation>();
        boolean firstError = true;
        for (MovecraftLocation tloc : foundCraft.getHitBox()) {
            Block tb = event.getClickedBlock().getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ());
            if (!tb.getType().equals(Material.SIGN_POST) && !tb.getType().equals(Material.WALL_SIGN)) {
                continue;
            }
            Sign ts = (Sign) tb.getState();

            if (isEqualSign(ts, targetText)) {
                if (isForbidden(ts)) {
                    if (firstError) {
                        event.getPlayer().sendMessage("Warning: Forbidden remote sign(s) found at the following locations with the following text:");
                        firstError = false;
                    }
                    event.getPlayer().sendMessage(" - ".concat(tloc.toString()).concat(" : ").concat(ts.getLine(0)));
                } else {
                    foundLocations.add(tloc);
                }
            }
        }
        if (!firstError) {
            return;
        }
        else if (foundLocations.isEmpty()) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Could not find target sign!"));
            return;
        }

        for (MovecraftLocation foundLoc : foundLocations) {
            Block newBlock = event.getClickedBlock().getWorld().getBlockAt(foundLoc.getX(), foundLoc.getY(), foundLoc.getZ());

            PlayerInteractEvent newEvent = new PlayerInteractEvent(event.getPlayer(), event.getAction(), event.getItem(), newBlock, event.getBlockFace());

            //TODO: DON'T DO THIS
            Bukkit.getServer().getPluginManager().callEvent(newEvent);
        }
        
        event.setCancelled(true);
    }
    private boolean isEqualSign(Sign test, String target) {
        return !ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(HEADER) && ( ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(1)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(2)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(3)).equalsIgnoreCase(target) );
    }
    private boolean isForbidden(Sign test) {
        for (int i = 0; i < 4; i++) {
            String t = test.getLine(i).toLowerCase();
            if(Settings.ForbiddenRemoteSigns.contains(t))
                return true;
        }
        return false;
    }
}
