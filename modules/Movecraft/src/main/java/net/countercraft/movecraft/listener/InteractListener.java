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

import net.countercraft.movecraft.api.utils.MathUtils;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public final class InteractListener implements Listener {
    private static final Map<Player, Long> timeMap = new HashMap<>();

    @EventHandler
    public final void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Material m = event.getClickedBlock().getType();
        if (!m.equals(Material.WOOD_BUTTON) && !m.equals(Material.STONE_BUTTON)) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        } // if they left click a button which is pressed, unpress it
        if (event.getClickedBlock().getData() >= 8) {
            event.getClickedBlock().setData((byte) (event.getClickedBlock().getData() - 8));
        }
    }

    @EventHandler
    public void onPlayerInteractStick(PlayerInteractEvent event) {

        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        // if not in command of craft, don't process pilot tool clicks
        if (c == null)
            return;

/*		if( c.getCannonDirector()==event.getPlayer() ) // if the player is the cannon director, don't let them move the ship
			return;

		if( c.getAADirector()==event.getPlayer() ) // if the player is the cannon director, don't let them move the ship
			return;
*/
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());

            if (event.getItem() == null || event.getItem().getTypeId() != Settings.PilotTool) {
                return;
            }
            event.setCancelled(true);
            if (craft == null) {
                return;
            }
            Long time = timeMap.get(event.getPlayer());
            if (time != null) {
                long ticksElapsed = (System.currentTimeMillis() - time) / 50;

                // if the craft should go slower underwater, make time
                // pass more slowly there
                if (craft.getType().getHalfSpeedUnderwater() && craft.getHitBox().getMinY() < craft.getW().getSeaLevel())
                    ticksElapsed = ticksElapsed >> 1;

                if (Math.abs(ticksElapsed) < craft.getType().getTickCooldown()) {
                    return;
                }
            }

            if (!MathUtils.locationInHitbox(craft.getHitBox(),event.getPlayer().getLocation())) {
                return;
            }

            if (!event.getPlayer().hasPermission("movecraft." + craft.getType().getCraftName() + ".move")) {
                event.getPlayer().sendMessage(
                        I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return;
            }
            if (craft.getPilotLocked()) {
                // right click moves up or down if using direct
                // control
                int DY = 1;
                if (event.getPlayer().isSneaking())
                    DY = -1;

                craft.translate(0, DY, 0);
                timeMap.put(event.getPlayer(), System.currentTimeMillis());
                craft.setLastCruisUpdate(System.currentTimeMillis());
                return;
            }
            // Player is onboard craft and right clicking
            float rotation = (float) Math.PI * event.getPlayer().getLocation().getYaw() / 180f;

            float nx = -(float) Math.sin(rotation);
            float nz = (float) Math.cos(rotation);

            int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
            int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);
            int dy;

            float p = event.getPlayer().getLocation().getPitch();

            dy = -(Math.abs(p) >= 25 ? 1 : 0) * (int) Math.signum(p);

            if (Math.abs(event.getPlayer().getLocation().getPitch()) >= 75) {
                dx = 0;
                dz = 0;
            }

            craft.translate(dx, dy, dz);
            timeMap.put(event.getPlayer(), System.currentTimeMillis());
            craft.setLastCruisUpdate(System.currentTimeMillis());
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getItem() == null || event.getItem().getTypeId() != Settings.PilotTool) {
                return;
            }
            Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (craft == null) {
                return;
            }
            if (craft.getPilotLocked()) {
                craft.setPilotLocked(false);
                event.getPlayer().sendMessage(
                        I18nSupport.getInternationalisedString("Leaving Direct Control Mode"));
                event.setCancelled(true);
                return;
            }
            if (!event.getPlayer().hasPermission("movecraft." + craft.getType().getCraftName() + ".move")
                    || !craft.getType().getCanDirectControl()) {
                        event.getPlayer().sendMessage(
                                I18nSupport.getInternationalisedString("Insufficient Permissions"));
                        return;
            }
            craft.setPilotLocked(true);
            craft.setPilotLockedX(event.getPlayer().getLocation().getBlockX() + 0.5);
            craft.setPilotLockedY(event.getPlayer().getLocation().getY());
            craft.setPilotLockedZ(event.getPlayer().getLocation().getBlockZ() + 0.5);
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Entering Direct Control Mode"));
            event.setCancelled(true);
        }

    }

}
