package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import static net.countercraft.movecraft.util.ChatUtils.ERROR_PREFIX;

public final class RemoteSign implements Listener{
    private static final String HEADER = "Remote Sign";

    @EventHandler
    public final void onSignChange(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase(HEADER)) {
            return;
        }
        else if(event.getLine(1).equals("")) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Sign - Cannot be blank"));
            event.setLine(0,"");
            event.setLine(2,"");
            event.setLine(3,"");
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
        Craft foundCraft = null;
        for (PlayerCraft tcraft : CraftManager.getInstance().getPlayerCraftsInWorld(event.getClickedBlock().getWorld())) {
            if (MathUtils.locationInHitBox(tcraft.getHitBox(), event.getClickedBlock().getLocation())) {
                // don't use a craft with a null player. This is
                // mostly to avoid trying to use subcrafts
                foundCraft = tcraft;
                break;
            }
        }

        if (foundCraft == null) {
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign - Must be a part of a piloted craft"));
            return;
        }

        if (!foundCraft.getType().getBoolProperty(CraftType.ALLOW_REMOTE_SIGN)) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Sign - Not allowed on this craft"));
            return;
        }

        String targetText = ChatColor.stripColor(sign.getLine(1));
        if(targetText.equalsIgnoreCase(HEADER)) {
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign - Cannot remote another Remote Sign"));
            return;
        }

        if(targetText.equalsIgnoreCase("")) {
            event.getPlayer().sendMessage("Remote Sign - Cannot be blank");
            return;
        }

        LinkedList<MovecraftLocation> foundLocations = new LinkedList<MovecraftLocation>();
        boolean firstError = true;
        for (MovecraftLocation tloc : foundCraft.getHitBox()) {
            BlockState tstate = event.getClickedBlock().getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ()).getState();
            if (!(tstate instanceof Sign)) {
                continue;
            }
            Sign ts = (Sign) tstate;

            if (isEqualSign(ts, targetText)) {
                if (isForbidden(ts)) {
                    if (firstError) {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Forbidden string found"));
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
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Could not find target sign"));
            return;
        }

        if (Settings.MaxRemoteSigns > -1) {
            int foundLocCount = foundLocations.size();
            if(foundLocCount > Settings.MaxRemoteSigns) {
                event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Remote Sign - Exceeding maximum allowed"), foundLocCount, Settings.MaxRemoteSigns));
                return;
            }
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
