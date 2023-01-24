package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class AscendSign implements Listener {
    private static final String MAIN_SIGN_TEXT = I18nSupport.getInternationalisedString("Sign - Ascend");
    private static final String ON_SIGN_TEXT = I18nSupport.getInternationalisedString("Sign - ON");
    private static final String OFF_SIGN_TEXT = I18nSupport.getInternationalisedString("Sign - OFF");

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            var block = location.toBukkit(world).getBlock();
            if(!Tag.SIGNS.isTagged(block.getType())){
                continue;
            }
            BlockState state = block.getState();
            if(block.getState() instanceof Sign){
                Sign sign = (Sign) block.getState();
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(MAIN_SIGN_TEXT + ON_SIGN_TEXT)) {
                    sign.setLine(0, MAIN_SIGN_TEXT + OFF_SIGN_TEXT);
                    sign.update();
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClickEvent(@NotNull PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)){
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(MAIN_SIGN_TEXT + OFF_SIGN_TEXT)) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
                return;
            }
            //c.resetSigns(true, false, true);
            sign.setLine(0, MAIN_SIGN_TEXT + ON_SIGN_TEXT);
            sign.update(true);

            c.setCruiseDirection(CruiseDirection.UP);
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);

            if (!c.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(MAIN_SIGN_TEXT + ON_SIGN_TEXT)) {
            return;
        }
        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (c == null || !c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
            return;
        }
        sign.setLine(0, MAIN_SIGN_TEXT + OFF_SIGN_TEXT);
        sign.update(true);

        c.setCruising(false);
        c.resetSigns(sign);

    }
}
