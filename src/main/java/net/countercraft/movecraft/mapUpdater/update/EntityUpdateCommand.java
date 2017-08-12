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

package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class EntityUpdateCommand extends UpdateCommand {
    private final Location newLocation;
    private final Entity entity;

    public EntityUpdateCommand(Location newLocation, Entity entity) {
        this.newLocation = newLocation;
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public Location getNewLocation() {
        return newLocation;
    }

    @Override
    public void doUpdate() {
        if (entity instanceof Player) {
            if(Settings.CompatibilityMode) {
                net.minecraft.server.v1_10_R1.EntityPlayer craftPlayer = ((CraftPlayer) entity).getHandle();
                craftPlayer.setPositionRotation(newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.getYaw(), craftPlayer.pitch);
                Location location = new Location(null, craftPlayer.locX, craftPlayer.locY, craftPlayer.locZ, craftPlayer.yaw, craftPlayer.pitch);
                craftPlayer.playerConnection.teleport(location);
            }else{
                newLocation.setPitch(entity.getLocation().getPitch());
                entity.teleport(newLocation);
            }
            // send the blocks around the player to the player, so they don't fall through the floor or get bumped by other blocks
                    /*Player p = (Player) entity;
                    for (BlockTranslateCommand muc : updatesInWorld) {
                        if (muc != null) {
                            int disty = Math.abs(muc.getNewBlockLocation().getY() - entityUpdate.getNewLocation().getBlockY());
                            int distx = Math.abs(muc.getNewBlockLocation().getX() - entityUpdate.getNewLocation().getBlockX());
                            int distz = Math.abs(muc.getNewBlockLocation().getZ() - entityUpdate.getNewLocation().getBlockZ());
                            if (disty < 2 && distx < 2 && distz < 2) {
                                Location nloc = new Location(w, muc.getNewBlockLocation().getX(), muc.getNewBlockLocation().getY(), muc.getNewBlockLocation().getZ());
                                p.sendBlockChange(nloc, muc.getTypeID(), muc.getDataID());
                            }
                        }
                    }*/
        } else {
            entity.teleport(newLocation);
        }
    }
}

