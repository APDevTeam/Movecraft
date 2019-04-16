package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.LegacyUtils;
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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class HelmSign implements Listener {

    @EventHandler
    public final void onSignChange(SignChangeEvent event){
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("[helm]")) {
            return;
        }
        event.setLine(0, "\\  ||  /");
        event.setLine(1, "==      ==");
        event.setLine(2, "/  ||  \\");
    }

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        Rotation rotation;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            rotation = Rotation.CLOCKWISE;
        }else if(event.getAction() == Action.LEFT_CLICK_BLOCK){
            rotation = Rotation.ANTICLOCKWISE;
        }else{
            return;
        }
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!(ChatColor.stripColor(sign.getLine(0)).equals("\\  ||  /") &&
                ChatColor.stripColor(sign.getLine(1)).equals("==      ==") &&
                ChatColor.stripColor(sign.getLine(2)).equals("/  ||  \\"))) {
            return;
        }
        Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (craft == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("movecraft." + craft.getType().getCraftName() + ".rotate")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        /*Long time = timeMap.get(event.getPlayer());
        if (time != null) {
            long ticksElapsed = (System.currentTimeMillis() - time) / 50;

            // if the craft should go slower underwater,
            // make time pass more slowly there
            if (craft.getType().getHalfSpeedUnderwater()
                    && craft.getMinY() < craft.getW().getSeaLevel())
                ticksElapsed = ticksElapsed >> 1;

            if (Math.abs(ticksElapsed) < craft.getTickCooldown()) {
                event.setCancelled(true);
                return;
            }
        }*/

        if (!MathUtils.locationInHitbox(craft.getHitBox(), event.getPlayer().getLocation())) {
            return;
        }

        if (craft.getType().rotateAtMidpoint()) {
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).rotate(rotation, craft.getHitBox().getMidPoint());
        } else {
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).rotate(rotation, MathUtils.bukkit2MovecraftLoc(sign.getLocation()));
        }

        //timeMap.put(event.getPlayer(), System.currentTimeMillis());
        event.setCancelled(true);
        //TODO: Lower speed while turning
            /*int curTickCooldown = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getCurTickCooldown();
            int baseTickCooldown = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCruiseTickCooldown();
            if (curTickCooldown * 2 > baseTickCooldown)
                curTickCooldown = baseTickCooldown;
            else
                curTickCooldown = curTickCooldown * 2;*/
        //CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCurTickCooldown(curTickCooldown); // lose half your speed when turning

    }
}
