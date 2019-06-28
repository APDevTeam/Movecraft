package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;

public final class RemoteSign implements Listener{
    private static final String HEADER = "Remote Sign";

    @EventHandler
    public final void onSignChange(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase(HEADER)) {
            return;
        }
        else if(event.getLine(1).equals("")) {
            event.getPlayer().sendMessage("ERROR: Remote Signs can't be blank!");
            event.setLine(0,"");
            event.setLine(2,"");
            event.setLine(3,"");
            return;
        }
        else if (event.getLine(1).equalsIgnoreCase("Name:")){
            event.getPlayer().sendMessage("ERROR: Remote Signs can't target Name: signs!");
            event.setLine(0,"");
            event.setLine(1,"");
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
        if (!(block.getState() instanceof Sign)) {
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
        LinkedList<MovecraftLocation> foundLocations = new LinkedList<MovecraftLocation>();
        boolean firstError = true;
        for (MovecraftLocation tloc : foundCraft.getHitBox()) {
            Block tb = event.getClickedBlock().getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ());
            if (!(tb.getState() instanceof Sign)) {
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
        } else if (foundLocations.isEmpty()) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Could not find target sign!"));
            return;
        }
        new BukkitRunnable()
        {
            @Override
            public void run() {

                MovecraftLocation foundLoc = foundLocations.poll();
                Block newBlock = event.getClickedBlock().getWorld().getBlockAt(foundLoc.getX(), foundLoc.getY(), foundLoc.getZ());
                Sign foundSign = (Sign) newBlock.getState();
                boolean inverted = false;//set to true if target name has an ! in front of the text
                //check Remote sign if the target strings are inverted
                for (String line : foundSign.getLines()){
                    if (line == null)
                        continue;
                    //if target line is prefixed with an exclamation point, it will invert the action of the Remote sign
                    if (!line.equals("!" + targetText))
                        continue;
                    inverted = true;
                    break;
                }
                PlayerInteractEvent newEvent = null;

                //Now invert the action to the opposite one if set to invert
                if (inverted) {
                    Action action = null;
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK){
                        action = Action.LEFT_CLICK_BLOCK;
                    } else if (event.getAction() == Action.LEFT_CLICK_BLOCK){
                        action = Action.RIGHT_CLICK_BLOCK;
                    }
                    if (action != null){
                        newEvent = new PlayerInteractEvent(event.getPlayer(), action, event.getItem(), newBlock, event.getBlockFace());
                    }
                }
                //Otherwise use the same action as on the clicked remote sign
                else {
                    newEvent = new PlayerInteractEvent(event.getPlayer(), event.getAction(), event.getItem(), newBlock, event.getBlockFace());
                }
                //TODO: DON'T DO THIS
                Bukkit.getServer().getPluginManager().callEvent(newEvent);
                if (foundLocations.isEmpty()){
                    cancel();
                }
            }


        }.runTaskTimer(Movecraft.getInstance(),0,11);
        
        event.setCancelled(true);
    }
    private boolean isEqualSign(Sign test, String target) {

        return !ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(HEADER) && (
                ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(1)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(2)).equalsIgnoreCase(target)
                || ChatColor.stripColor(test.getLine(3)).equalsIgnoreCase(target)
                ||ChatColor.stripColor(test.getLine(0)).equalsIgnoreCase("!"+target)
                || ChatColor.stripColor(test.getLine(1)).equalsIgnoreCase("!"+target)
                || ChatColor.stripColor(test.getLine(2)).equalsIgnoreCase("!"+target)
                || ChatColor.stripColor(test.getLine(3)).equalsIgnoreCase("!"+target));
    }
    private boolean isForbidden(Sign test) {
        for (int i = 0; i < 4; i++) {
            String t = test.getLine(i).toLowerCase();
            if(Settings.ForbiddenRemoteSigns.contains(t))
                return true;
        }
        return false;
    }
    private boolean invertAction(String line){
        return line.charAt(0) == '!';
    }
}
