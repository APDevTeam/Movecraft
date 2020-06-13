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

import net.countercraft.movecraft.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class EntityUpdateCommand extends UpdateCommand {
    private final Entity entity;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public EntityUpdateCommand(Entity entity, double dx, double dy, double dz, float yaw, float pitch) {
        this.entity = entity;
        this.x = dx;
        this.y = dy;
        this.z = dz;
        this.yaw = yaw;
        this.pitch = pitch;
    }


    public final Entity getEntity() {
        return entity;
    }

    @Override
    public void doUpdate() {
        final Location entityLoc = entity.getVehicle() != null ? entity.getVehicle().getLocation() : entity.getLocation();
        if (!(entity instanceof Player)) {
            TeleportUtils.teleportEntity(entity, new Location(entity.getWorld(), entityLoc.getX() + x, entityLoc.getY() + y, entityLoc.getZ() + z,yaw + entityLoc.getYaw(),pitch + entityLoc.getPitch()));
            return;
        }
        //Movecraft.getInstance().getWorldHandler().addPlayerLocation((Player) entity,x,y,z,yaw,pitch);
        TeleportUtils.teleport((Player) entity, new Location(entity.getWorld(), entityLoc.getX() + x, entityLoc.getY() + y, entityLoc.getZ() + z), yaw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, x, y, z, pitch, yaw);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof EntityUpdateCommand)){
            return false;
        }
        EntityUpdateCommand other = (EntityUpdateCommand) obj;
        return this.x == other.x &&
                this.y == other.y &&
                this.z == other.z &&
                this.pitch == other.pitch &&
                this.yaw == other.yaw &&
                this.entity.equals(other.entity);
    }
}

