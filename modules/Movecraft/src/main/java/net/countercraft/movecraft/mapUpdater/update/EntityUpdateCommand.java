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

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.TeleportUtils;
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

    public EntityUpdateCommand(Entity entity, double dx, double dy, double dz, float yaw, float pitch) {
        this.entity = entity;
        this.x = dx;
        this.y = dy;
        this.z = dz;
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


    public final Entity getEntity() {
        return entity;
    }

    @Override
    public void doUpdate() {
        final Location entityLoc = entity.getVehicle() != null ? entity.getVehicle().getLocation() : entity.getLocation();
        final Location destLoc = new Location(world, entityLoc.getX() + x, entityLoc.getY() + y, entityLoc.getZ() + z,yaw + entityLoc.getYaw(),pitch + entityLoc.getPitch());
        if (!entity.getWorld().equals(world)) {
            entity.teleport(destLoc);
            if (sound != null && (entity instanceof Player)) {
                ((Player) entity).playSound(entityLoc, sound, volume, 1.0f);
            }
            return;
        } else if (!(entity instanceof Player)) {
            TeleportUtils.teleportEntity(entity, destLoc);
            return;
        }
        Location playerLoc = entity.getLocation();

        Player player = (Player) entity;
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        Location location;
        if (craft != null && craft.getNotificationPlayer() == player && craft.getPilotLocked()) {
            craft.setPilotLockedX(craft.getPilotLockedX() + x);
            craft.setPilotLockedY(craft.getPilotLockedY() + y);
            craft.setPilotLockedZ(craft.getPilotLockedZ() + z);
            location = new Location(world, craft.getPilotLockedX(), craft.getPilotLockedY(), craft.getPilotLockedZ());
        } else {
            location = new Location(world, playerLoc.getX() + x, playerLoc.getY() + y, playerLoc.getZ() + z);
        }
        //Movecraft.getInstance().getWorldHandler().addPlayerLocation((Player) entity,x,y,z,yaw,pitch);
        TeleportUtils.teleport((Player) entity, location, yaw);
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
