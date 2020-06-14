package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class TeleportSign implements Listener {
    private static final String HEADER = "Teleport:";
    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!SignUtils.isSign(block)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
            return;
        }
        
        int tX = sign.getX(); int tY = sign.getY(); int tZ = sign.getZ();
        String[] numbers = ChatColor.stripColor(sign.getLine(1)).replaceAll(" ", "").split(",");
        if (numbers.length >= 3) {
            try {
                
                tX = Integer.parseInt(numbers[0]);
                tY = Integer.parseInt(numbers[1]);
                tZ = Integer.parseInt(numbers[2]);
                
            } catch (NumberFormatException e) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Invalid Coordinates"));
                return;
            }
        }
        
        String w = ChatColor.stripColor(sign.getLine(2));
        World world = Bukkit.getWorld(w);
        if (world == null) world = sign.getWorld();

        if (!event.getPlayer().hasPermission("movecraft." + CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCraftName() + ".move")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanTeleport()) {
            int dx = tX - sign.getX();
            int dy = tY - sign.getY();
            int dz = tZ - sign.getZ();
            CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).translate(world, dx, dy, dz);
        }
    }
}
