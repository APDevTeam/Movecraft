package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
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
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
                    sign.setLine(0, "Ascend: OFF");
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

        Sign sign = (Sign) block.getState();
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")) {
            event.setCancelled(true);
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
                return;
            }
            //c.resetSigns(true, false, true);
            sign.setLine(0, "Ascend: ON");
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
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
            return;
        }
        event.setCancelled(true);
        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (c == null || !c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
            return;
        }
        sign.setLine(0, "Ascend: OFF");
        sign.update(true);

        c.setCruising(false);
        c.resetSigns(sign);

    }
}
