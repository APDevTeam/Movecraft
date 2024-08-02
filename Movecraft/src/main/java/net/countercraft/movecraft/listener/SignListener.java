package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.sign.AbstractCraftSign;
import net.countercraft.movecraft.sign.AbstractMovecraftSign;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onCraftDetect(CraftDetectEvent event) {
        final World world = event.getCraft().getWorld();;
        event.getCraft().getHitBox().forEach(
                (mloc) -> {
                    Block block = mloc.toBukkit(world).getBlock();
                    BlockState state = block.getState();
                    if (state instanceof Sign sign) {
                        String ident = sign.getLines()[0];
                        AbstractCraftSign.tryGetCraftSign(ident).ifPresent(acs -> {
                            acs.onCraftDetect(event, sign);
                        });
                    }
                }
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignTranslate(SignTranslateEvent event) {
        String ident = event.getLine(0);
        AbstractCraftSign.tryGetCraftSign(ident).ifPresent(acs -> {
            acs.onSignMovedByCraft(event);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            final String signHeader = ChatColor.stripColor(event.getLines()[0]);
            AbstractMovecraftSign.tryGet(signHeader).ifPresent(ams -> {

                boolean success = ams.processSignChange(event);
                if (ams.shouldCancelEvent(success, null, event.getPlayer().isSneaking())) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            final String signHeader = ChatColor.stripColor(sign.getLines()[0]);
            AbstractMovecraftSign.tryGet(signHeader).ifPresent(ams -> {
                boolean success = ams.processSignClick(event.getAction(), sign, event.getPlayer());
                if (ams.shouldCancelEvent(success, event.getAction(), event.getPlayer().isSneaking())) {
                    event.setCancelled(true);
                }
            });
        }
    }

}
