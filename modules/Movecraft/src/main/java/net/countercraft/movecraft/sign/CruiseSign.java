package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CruiseSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getW();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            BlockState state = location.toBukkit(world).getBlock().getState();
            if(state instanceof Sign){
                Sign sign = (Sign) state;
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")) {
                    sign.setLine(0, "Cruise: OFF");
                    sign.update();
                }
            }
        }
    }

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getCanCruise()) {
                return;
            }
            //c.resetSigns(false, true, true);
            sign.setLine(0, "Cruise: ON");
            sign.update(true);


            if(sign.getData() instanceof WallSign) {
                c.setCruiseDirection(CruiseDirection.fromBlockFace(((WallSign) sign.getData()).getFacing()));
            } else {
                c.setCruiseDirection(CruiseDirection.NONE);
            }
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);
            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")
                && CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) != null
                && CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            sign.setLine(0, "Cruise: OFF");
            sign.update(true);
            c.setCruising(false);
            c.resetSigns(sign);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!event.getLine(0).equalsIgnoreCase("Cruise: OFF") && !event.getLine(0).equalsIgnoreCase("Cruise: ON")) {
            return;
        }
        if (player.hasPermission("movecraft.cruisesign") || !Settings.RequireCreatePerm) {
            return;
        }
        player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
        event.setCancelled(true);
    }
}
