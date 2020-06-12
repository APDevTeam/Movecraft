package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static net.countercraft.movecraft.utils.ChatUtils.ERROR_PREFIX;

public final class CannonDirectorSign implements Listener {
    private static final String HEADER = "Cannon Director";

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
        if(!Settings.AllowDirectorSigns) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Sign - This Sign Not Enabled"));
            return;
        }
        Craft foundCraft = null;
        CraftManager.getInstance().getCraftsInWorld(block.getWorld());
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if ( tcraft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(block.getLocation())) &&
                    CraftManager.getInstance().getPlayerFromCraft(tcraft) != null) {
                foundCraft = tcraft;
                break;
            }
        }

        if (foundCraft == null) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("CannonDirector - Must Be Part Of Craft"));
            return;
        }

        if (!foundCraft.getType().allowCannonDirectorSign()) {
            event.getPlayer().sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("CannonDirector - Not Allowed On Craft"));
            return;
        }
        if(event.getAction()==Action.LEFT_CLICK_BLOCK && event.getPlayer()==foundCraft.getCannonDirector()){
            foundCraft.setCannonDirector(null);
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("CannonDirector - No Longer Directing"));
            return;
        }


        foundCraft.setCannonDirector(event.getPlayer());
        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("CannonDirector - Directing"));
        if (foundCraft.getAADirector() == event.getPlayer())
            foundCraft.setAADirector(null);

    }
}
