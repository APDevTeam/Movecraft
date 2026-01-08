package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

public class HelmSign extends AbstractCraftSign {

    public static final String PRETTY_HEADER = "\\  ||  /";
    public static final Component[] PRETTY_LINES = new Component[] {
            Component.text(PRETTY_HEADER),
            Component.text("==      =="),
            Component.text("/  ||  \\")
    };   

    public HelmSign() {
        super(false);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {

    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        // Nothing to check here honestly...
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("[helm]")) {
            return true;
        }
        for (int i = 0; i < PRETTY_LINES.length && i < event.getLines().length; i++) {
            event.line(i, PRETTY_LINES[i]);
        }
        return true;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        MovecraftRotation rotation;
        if (clickType == Action.RIGHT_CLICK_BLOCK) {
            rotation = MovecraftRotation.CLOCKWISE;
        }else if(clickType == Action.LEFT_CLICK_BLOCK){
            rotation = MovecraftRotation.ANTICLOCKWISE;
        }else{
            return false;
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

        if(!MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(player.getLocation())))
            return false;

        // TODO: Why was this used before?  CraftManager.getInstance().getCraftByPlayer(event.getPlayer())...  The craft variable did exist, so why don't use it?
        if (craft.getCraftProperties().get(PropertyKeys.ROTATE_AT_MIDPOINT)) {
            craft.rotate(rotation, craft.getHitBox().getMidPoint());
        } else {
           craft.rotate(rotation, MathUtils.bukkit2MovecraftLoc(sign.block().getLocation()));
        }

        //timeMap.put(event.getPlayer(), System.currentTimeMillis());
        //TODO: Lower speed while turning
            /*int curTickCooldown = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getCurTickCooldown();
            int baseTickCooldown = CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCruiseTickCooldown();
            if (curTickCooldown * 2 > baseTickCooldown)
                curTickCooldown = baseTickCooldown;
            else
                curTickCooldown = curTickCooldown * 2;*/
        //CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCurTickCooldown(curTickCooldown); // lose half your speed when turning

        return true;
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            if (!player.hasPermission("movecraft." + craft.getCraftProperties().getName().toLowerCase() + ".rotate")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return false;
            }
            return true;
        }
        return false;
    }
}
