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
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public final class InteractListener implements Listener {
    private final Map<Player, Long> timeMap = new WeakHashMap<>();

    @EventHandler(priority = EventPriority.LOWEST) // LOWEST so that it runs before the other events
    public void onPlayerInteract(@NotNull PlayerInteractEvent e) {
        if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) {
            if (e.getItem() != null && e.getItem().getType() == Settings.PilotTool) {
                // Handle pilot tool left clicks
                e.setCancelled(true);

                Player p = e.getPlayer();
                PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(p);
                if (craft == null)
                    return;

                if (craft.getPilotLocked()) {
                    // Allow all players to leave direct control mode
                    craft.setPilotLocked(false);
                    p.sendMessage(I18nSupport.getInternationalisedString("Direct Control - Leaving"));
                }
                else if (!p.hasPermission(
                        "movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")
                        || !craft.getType().getBoolProperty(CraftType.CAN_DIRECT_CONTROL)) {
                    // Deny players from entering direct control mode
                    p.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                }
                else {
                    // Enter direct control mode
                    craft.setPilotLocked(true);
                    craft.setPilotLockedX(p.getLocation().getBlockX() + 0.5);
                    craft.setPilotLockedY(p.getLocation().getY());
                    craft.setPilotLockedZ(p.getLocation().getBlockZ() + 0.5);
                    p.sendMessage(I18nSupport.getInternationalisedString("Direct Control - Entering"));
                }
            }
            else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Handle button left clicks
                BlockState state = e.getClickedBlock().getState();
                if (!(state instanceof Switch))
                    return;

                Switch data = (Switch) state.getBlockData();
                if (data.isPowered()) {
                    // Depower the button
                    data.setPowered(false);
                    e.getClickedBlock().setBlockData(data);
                    e.setCancelled(true);
                }
            }
        }
        else if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getItem() == null || e.getItem().getType() != Settings.PilotTool)
                return;

            // Handle pilot tool right clicks
            e.setCancelled(true);

            Player p = e.getPlayer();
            PlayerCraft craft = CraftManager.getInstance().getCraftByPlayer(p);
            if (craft == null)
                return;

            CraftType type = craft.getType();
            int currentGear = craft.getCurrentGear();
            int tickCooldown = (int) craft.getType().getPerWorldProperty(
                    CraftType.PER_WORLD_TICK_COOLDOWN, craft.getWorld());
            if (type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT)
                    && type.getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_TICK_COOLDOWN))
                tickCooldown *= currentGear; // Account for gear shifts
            Long lastTime = timeMap.get(p);
            if (lastTime != null) {
                long ticksElapsed = (System.currentTimeMillis() - lastTime) / 50;

                // if the craft should go slower underwater, make time pass more slowly there
                if (craft.getType().getBoolProperty(CraftType.HALF_SPEED_UNDERWATER)
                        && craft.getHitBox().getMinY() < craft.getWorld().getSeaLevel())
                    ticksElapsed /= 2;

                if (ticksElapsed < tickCooldown)
                    return; // Not enough time has passed, so don't do anything
            }

            if (!p.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME) + ".move")) {
                p.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return; // Player doesn't have permission to move this craft, so don't do anything
            }

            if (!MathUtils.locationNearHitBox(craft.getHitBox(), p.getLocation(), 2))
                return; // Player is not near the craft, so don't do anything

            if (craft.getPilotLocked()) {
                // Direct control mode allows vertical movements when right-clicking
                int dy = 1; // Default to up
                if (p.isSneaking())
                    dy = -1; // Down if sneaking
                if (craft.getType().getBoolProperty(CraftType.GEAR_SHIFTS_AFFECT_DIRECT_MOVEMENT))
                    dy *= currentGear; // account for gear shifts

                craft.translate(craft.getWorld(), 0, dy, 0);
                timeMap.put(p, System.currentTimeMillis());
                craft.setLastCruiseUpdate(System.currentTimeMillis());
                return;
            }

            double rotation = p.getLocation().getYaw() * Math.PI / 180.0;
            float nx = -(float) Math.sin(rotation);
            float nz = (float) Math.cos(rotation);
            int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
            int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);

            float pitch = p.getLocation().getPitch();
            int dy = -(Math.abs(pitch) >= 25 ? 1 : 0) * (int) Math.signum(pitch);
            if (Math.abs(pitch) >= 75) {
                dx = 0;
                dz = 0;
            }

            craft.translate(craft.getWorld(), dx, dy, dz);
            timeMap.put(p, System.currentTimeMillis());
            craft.setLastCruiseUpdate(System.currentTimeMillis());
        }
    }
}
