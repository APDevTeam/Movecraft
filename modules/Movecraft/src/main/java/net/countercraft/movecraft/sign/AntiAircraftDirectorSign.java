package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.utils.MathUtils;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AntiAircraftDirectorSign implements Listener {
    private static final String HEADER = "AA Director";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction()!=Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN){
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        Craft foundCraft = null;
        if (CraftManager.getInstance().getCraftsInWorld(block.getWorld()) == null) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Sign must be a part of a piloted craft!"));
            return;
        }
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if (MathUtils.locationInHitbox(tcraft.getHitBox(), event.getClickedBlock().getLocation()) &&
                    CraftManager.getInstance().getPlayerFromCraft(tcraft) != null) {
                foundCraft = tcraft;
                break;
            }
        }

        if (foundCraft == null) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: Sign must be a part of a piloted craft!"));
            return;
        }

        if (!foundCraft.getType().allowCannonDirectorSign()) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("ERROR: AA Director Signs not allowed on this craft!"));
            return;
        }
        if(event.getAction()==Action.LEFT_CLICK_BLOCK && event.getPlayer()==foundCraft.getCannonDirector()){
            foundCraft.setCannonDirector(null);
            event.getPlayer().sendMessage("You are no longer directing the AA of this craft");
            return;
        }


        foundCraft.setAADirector(event.getPlayer());
        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You are now directing the AA of this craft"));
        if (foundCraft.getCannonDirector() == event.getPlayer()) {
            foundCraft.setCannonDirector(null);
        }

    }
}
