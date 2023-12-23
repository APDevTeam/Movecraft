package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class SpeedSign implements Listener{
    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            var block = location.toBukkit(world).getBlock();
            if(!Tag.SIGNS.isTagged(block.getType()))
                continue;

            BlockState state = block.getState();
            if (!(state instanceof Sign))
                continue;

            Sign sign = (Sign) state;
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Speed:")) {
                sign.setLine(1, "0 m/s");
                sign.setLine(2, "0ms");
                sign.setLine(3, "0T");
                sign.update();
            }
        }
    }

    @EventHandler
    public void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("Speed:")) {
            return;
        }
        event.setLine(1,String.format("%.2f",craft.getSpeed()) + "m/s");
        event.setLine(2,String.format("%.2f",craft.getMeanCruiseTime() * 1000) + "ms");
        event.setLine(3,craft.getTickCooldown() + "T");
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        BlockState state = block.getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Speed:"))
            return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null)
            return;

        final int gearShifts = craft.getType().getIntProperty(CraftType.GEAR_SHIFTS);
        int currentGear = craft.getCurrentGear();
        if (gearShifts == 1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(I18nSupport.getInternationalisedString("Gearshift - Disabled for craft type")));
            return;
        }
        currentGear++;
        if (currentGear > gearShifts)
            currentGear = 1;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(I18nSupport.getInternationalisedString("Gearshift - Gear changed")
                        + " " + currentGear + " / " + gearShifts));
        craft.setCurrentGear(currentGear);
    }
}
