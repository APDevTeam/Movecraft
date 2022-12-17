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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.util.ReflectUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
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
    private final World world;
    private final Sound sound;
    private final float volume;

    public EntityUpdateCommand(Entity entity, double x, double y, double z, float yaw, float pitch) {
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = entity.getWorld();
        this.sound = null;
        this.volume = 0.0f;
    }

    public EntityUpdateCommand(Entity entity, double x, double y, double z, float yaw, float pitch, World world) {
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
        this.sound = null;
        this.volume = 0.0f;
    }

    public EntityUpdateCommand(Entity entity, double x, double y, double z, float yaw, float pitch, World world, Sound sound, float volume) {
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
        this.sound = sound;
        this.volume = volume;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void doUpdate() {
        Location playerLoc = entity.getLocation();
        // Use bukkit teleporting API for changing worlds because it won't be smooth anyway
        if (!(entity instanceof Player) || !playerLoc.getWorld().equals(world)) { 
            entity.teleport(new Location(world, x + playerLoc.getX(),y + playerLoc.getY(),z + playerLoc.getZ(),yaw + playerLoc.getYaw(),pitch + playerLoc.getPitch()));
            return;
        }
        Location location = new Location(world, playerLoc.getX() + x, playerLoc.getY() + y, playerLoc.getZ() + z);
        Movecraft.getInstance().getSmoothTeleport().teleport((Player) entity, location, yaw, pitch);
        if (sound != null) {
            ((Player) entity).playSound(location, sound, volume, 1.0f);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity.getUniqueId(), x, y, z, pitch, yaw);
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
