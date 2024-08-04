package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HelmSign extends AbstractCraftSign {

    public static final String[] PRETTY_LINES = new String[] {
            "\\  ||  /",
            "\\  ||  /",
            "/  ||  \\"
    };
    public static final String PRETTY_HEADER = PRETTY_LINES[0];

    public HelmSign() {
        super(false);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("[helm]")) {
            return;
        }
        for (int i = 0; i < PRETTY_LINES.length && i < event.getLines().length; i++) {
            event.setLine(i, PRETTY_LINES[i]);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {


    }

    @Override
    protected void onParentCraftBusy(Player player, Craft craft) {

    }

    @Override
    protected void onCraftNotFound(Player player, Sign sign) {

    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        return !sneaking;
    }

    @Override
    protected boolean isSignValid(Action clickType, Sign sign, Player player) {
        // Nothing to check here honestly...
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event) {
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("[helm]")) {
            return true;
        }
        event.setLine(0, "\\  ||  /");
        event.setLine(1, "==      ==");
        event.setLine(2, "/  ||  \\");
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, Sign sign, Player player, Craft craft) {
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
        if (craft.getType().getBoolProperty(CraftType.ROTATE_AT_MIDPOINT)) {
            craft.rotate(rotation, craft.getHitBox().getMidPoint());
        } else {
           craft.rotate(rotation, MathUtils.bukkit2MovecraftLoc(sign.getLocation()));
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

        return false;
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            if (!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".rotate")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return false;
            }
            return true;
        }
        return false;
    }
}
