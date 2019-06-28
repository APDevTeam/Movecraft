package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
            Block block = location.toBukkit(world).getBlock();
            if(block.getState() instanceof Sign){
                Sign sign = (Sign) block.getState();
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
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }

            if (!c.getType().getCanCruise()) {
                return;
            }
            //c.resetSigns(false, true, true);

            sign.setLine(0, "Cruise: ON");
            sign.update(true);
            final BlockFace direction;
            if (Settings.IsLegacy){
                if (sign.getData() instanceof org.bukkit.material.Sign){
                    org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign.getData();
                    if (signData.isWallSign()) {
                        direction = signData.getAttachedFace();
                    } else {
                        direction = signData.getFacing().getOppositeFace();
                    }
                }else {
                    direction = null;
                }
            } else {
                if (sign.getBlockData() instanceof org.bukkit.block.data.type.Sign){
                    org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign)sign.getBlockData();
                    direction = signData.getRotation().getOppositeFace();
                }else if (sign.getBlockData() instanceof org.bukkit.block.data.type.WallSign){
                    WallSign signData = (WallSign)sign.getBlockData();
                    direction = signData.getFacing().getOppositeFace();
                } else {
                    direction = null;
                }
            }
            c.setCruiseDirection(direction);
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")
                && c != null
                && c.getType().getCanCruise()) {
            sign.setLine(0, "Cruise: OFF");
            sign.update(true);
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).setCruising(false);
        }
        if (c == null)
            return;
        Movecraft.getInstance().getLogger().info("Cruising: " + c.getCruising());
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
