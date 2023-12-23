package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class CruiseSign implements Listener {

    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event) {
        World world = event.getCraft().getWorld();
        for (MovecraftLocation location : event.getCraft().getHitBox()) {
            var block = location.toBukkit(world).getBlock();
            if (!Tag.SIGNS.isTagged(block.getType()))
                continue;

            BlockState state = block.getState();
            if (!(state instanceof Sign))
                continue;
            Sign sign = (Sign) state;
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")) {
                sign.setLine(0, "Cruise: OFF");
                sign.update();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) state;
        String line = ChatColor.stripColor(sign.getLine(0));
        if (line.equalsIgnoreCase("Cruise: OFF")) {
            event.setCancelled(true);
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (c == null || !c.getType().getBoolProperty(CraftType.CAN_CRUISE))
                return;
            if (!(sign.getBlockData() instanceof Directional))
                return;

            sign.setLine(0, "Cruise: ON");
            sign.update(true);

            c.setCruiseDirection(CruiseDirection.fromBlockFace(((Directional) sign.getBlockData()).getFacing()));
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);
            if (!c.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
                CraftManager.getInstance().addReleaseTask(c);
            }
        }
        else if (line.equalsIgnoreCase("Cruise: ON")) {
            event.setCancelled(true);
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (c == null || !c.getType().getBoolProperty(CraftType.CAN_CRUISE))
                return;

            sign.setLine(0, "Cruise: OFF");
            sign.update(true);
            c.setCruising(false);
            c.resetSigns(sign);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(@NotNull SignChangeEvent event) {
        Player player = event.getPlayer();
        String line = ChatColor.stripColor(event.getLine(0));
        if (line == null)
            return;
        if (!line.equalsIgnoreCase("Cruise: OFF") && !line.equalsIgnoreCase("Cruise: ON"))
            return;
        if (player.hasPermission("movecraft.cruisesign") || !Settings.RequireCreatePerm)
            return;

        player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
        event.setCancelled(true);
    }
}
