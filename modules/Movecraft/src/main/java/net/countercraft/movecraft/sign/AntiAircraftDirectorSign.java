package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MathUtils;
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

import static net.countercraft.movecraft.utils.ChatUtils.ERROR_PREFIX;

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
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if (MathUtils.locationInHitBox(tcraft.getHitBox(), event.getClickedBlock().getLocation()) &&
                    CraftManager.getInstance().getPlayerFromCraft(tcraft) != null) {
                foundCraft = tcraft;
                break;
            }
        }

        if (foundCraft == null) {
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("AADirector - Must Be Part Of Craft"));
            return;
        }

        if (!foundCraft.getType().allowCannonDirectorSign()) {
            event.getPlayer().sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("AADirector - Not Allowed On Craft"));
            return;
        }
        if(event.getAction()==Action.LEFT_CLICK_BLOCK && event.getPlayer()==foundCraft.getCannonDirector()){
            foundCraft.setCannonDirector(null);
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("AADirector - No Longer Directing"));
            return;
        }


        foundCraft.setAADirector(event.getPlayer());
        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("AADirector - Directing"));
        if (foundCraft.getCannonDirector() == event.getPlayer()) {
            foundCraft.setCannonDirector(null);
        }

    }
}
