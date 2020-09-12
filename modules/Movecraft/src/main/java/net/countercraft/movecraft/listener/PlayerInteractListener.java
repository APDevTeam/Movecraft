/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public final class PlayerInteractListener implements Listener {
    private static final Map<Player, Long> timeMap = new HashMap<>();

    @EventHandler
    public final void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            processButtonSmack(event.getClickedBlock());
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        // if not in command of craft, don't process pilot tool clicks
        if (craft == null)
            return;

        Player player = event.getPlayer();
        if (event.getItem() == null || event.getItem().getTypeId() != Settings.PilotTool) {
            return;
        }
        if (!player.hasPermission("movecraft." + craft.getType().getCraftName() + ".move")) {
            player.sendMessage(
                    I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }
        event.setCancelled(true);

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            processRightStick(event.getPlayer(), craft);
        }
        if(event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            processLeftStick(event.getPlayer(), craft);
        }
    }

    // if they left click a button which is pressed, unpress it
    public void processButtonSmack(Block b) {
        Material m = b.getType();
        if (!m.equals(Material.WOOD_BUTTON) && !m.equals(Material.STONE_BUTTON)) {
            return;
        }
        if (b.getData() >= 8) {
            b.setData((byte) (b.getData() - 8));
        }
    }

    public void processRightStick(Player player, Craft craft) {
        Long time = timeMap.get(player);
        if (time != null) {
            long ticksElapsed = (System.currentTimeMillis() - time) / 50;

            // if the craft should go slower underwater, make time
            // pass more slowly there
            if (craft.getType().getHalfSpeedUnderwater() && craft.getHitBox().getMinY() < craft.getW().getSeaLevel())
                ticksElapsed = ticksElapsed >> 1;

            if (Math.abs(ticksElapsed) < craft.getType().getTickCooldown(craft.getW())) {
                return;
            }
        }

        if (!MathUtils.locationNearHitBox(craft.getHitBox(), player.getLocation(),2)) {
            return;
        }

        if (craft.getPilotLocked()) {
            // right click moves up or down if using direct
            // control
            int DY = 1;
            if (player.isSneaking())
                DY = -1;

            craft.translate(0, DY, 0);
            timeMap.put(player, System.currentTimeMillis());
            craft.setLastCruiseUpdate(System.currentTimeMillis());
            return;
        }
        // Player is onboard craft and right clicking
        float rotation = (float) Math.PI * player.getLocation().getYaw() / 180f;

        float nx = -(float) Math.sin(rotation);
        float nz = (float) Math.cos(rotation);

        int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
        int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);
        int dy;

        float p = player.getLocation().getPitch();

        dy = -(Math.abs(p) >= 25 ? 1 : 0) * (int) Math.signum(p);

        if (Math.abs(player.getLocation().getPitch()) >= 75) {
            dx = 0;
            dz = 0;
        }

        craft.translate(dx, dy, dz);
        timeMap.put(player, System.currentTimeMillis());
        craft.setLastCruiseUpdate(System.currentTimeMillis());
    }

    public void processLeftStick(Player player, Craft craft) {
        if (craft.getPilotLocked()) {
            craft.setPilotLocked(false);
            player.sendMessage(I18nSupport.getInternationalisedString("Direct Control - Leaving"));
        }
        else {
            craft.setPilotLocked(true);
            craft.setPilotLockedX(player.getLocation().getBlockX() + 0.5);
            craft.setPilotLockedY(player.getLocation().getY());
            craft.setPilotLockedZ(player.getLocation().getBlockZ() + 0.5);
            player.sendMessage(I18nSupport.getInternationalisedString("Direct Control - Entering"));
        }
    }
}
