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

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
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
        BlockData data = event.getClickedBlock().getBlockData();
        if (!(data instanceof Switch)) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        } // if they left click a button which is pressed, unpress it
        Switch state = (Switch) data;
        if (state.isPowered()) {
            state.setPowered(false);
            event.getClickedBlock().setBlockData(state);
        }
    }

    @EventHandler
    public void onPlayerInteractStick(PlayerInteractEvent event) {

        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        // if not in command of craft, don't process pilot tool clicks
        if (c == null)
            return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Player player = event.getPlayer();
            PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(player);

            if (event.getItem() == null || event.getItem().getType() != Settings.PilotTool) {
                return;
            }
            event.setCancelled(true);
            if (craft == null) {
                return;
            }
            final CraftType type = craft.getType();
            int currentGear = craft.getCurrentGear();
            if (player.isSneaking() && !craft.getPilotLocked()) {
                final int gearShifts = type.getIntProperty(CraftType.GEAR_SHIFTS);
                if (gearShifts == 1) {
                    player.sendMessage(I18nSupport.getInternationalisedString("Gearshift - Disabled for craft type"));
                    return;
                }
                currentGear++;
                if (currentGear > gearShifts)
                    currentGear = 1;
                player.sendMessage(I18nSupport.getInternationalisedString("Gearshift - Gear changed") + " " + currentGear + " / " + gearShifts);
                craft.setCurrentGear(currentGear);
                return;
            }
            Long time = timeMap.get(player);
            int tickCooldown = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_TICK_COOLDOWN, craft.getWorld());
            if (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT) && type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN)) {
                tickCooldown *= currentGear;
            }
            if (time != null) {
                long ticksElapsed = (System.currentTimeMillis() - time) / 50;

                // if the craft should go slower underwater, make time
                // pass more slowly there
                if (craft.getType().getBoolProperty(CraftType.HALF_SPEED_UNDERWATER) && craft.getHitBox().getMinY() < craft.getWorld().getSeaLevel())
                    ticksElapsed /= 2;


                if (Math.abs(ticksElapsed) < tickCooldown) {
                    return;
                }
            }

            if (!MathUtils.locationNearHitBox(craft.getHitBox(),event.getPlayer().getLocation(),2)) {
                return;
            }

            if (!event.getPlayer().hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")) {
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
                if (craft.getType().getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT))
                    DY *= currentGear;
                craft.translate(0, DY, 0);
                timeMap.put(event.getPlayer(), System.currentTimeMillis());
                craft.setLastCruiseUpdate(System.currentTimeMillis());
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
            craft.setLastCruiseUpdate(System.currentTimeMillis());
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getItem() == null || event.getItem().getType() != Settings.PilotTool) {
                return;
            }
            PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (craft == null) {
                return;
            }
            if (craft.getPilotLocked()) {
                craft.setPilotLocked(false);
                event.getPlayer().sendMessage(
                        I18nSupport.getInternationalisedString("Direct Control - Leaving"));
                event.setCancelled(true);
                return;
            }
            if (!event.getPlayer().hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")
                    || !craft.getType().getBoolProperty(CraftType.CAN_DIRECT_CONTROL)) {
                        event.getPlayer().sendMessage(
                                I18nSupport.getInternationalisedString("Insufficient Permissions"));
                        return;
            }
            craft.setPilotLocked(true);
            craft.setPilotLockedX(event.getPlayer().getLocation().getBlockX() + 0.5);
            craft.setPilotLockedY(event.getPlayer().getLocation().getY());
            craft.setPilotLockedZ(event.getPlayer().getLocation().getBlockZ() + 0.5);
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Direct Control - Entering"));
            event.setCancelled(true);
        }

    }

}
