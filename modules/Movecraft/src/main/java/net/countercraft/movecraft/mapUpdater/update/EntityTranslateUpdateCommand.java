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
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.TeleportUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class EntityTranslateUpdateCommand extends UpdateCommand {
    private final Craft craft;
    private final HitBox oldHitBox;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public EntityTranslateUpdateCommand(Craft craft, HitBox oldHitBox, double x, double y, double z) {
        this.craft = craft;
        this.oldHitBox = oldHitBox;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0;
        this.pitch = 0;
    }


    public Craft getCraft() {
        return craft;
    }

    @Override
    public void doUpdate() {
        if (oldHitBox.isEmpty()){
            return;
        }
        for (Entity entity : getEntitiesOnCraft()){
            Location playerLoc = entity.getLocation();
            if (!(entity instanceof Player) || yaw > .01 || pitch > .01 || yaw < -.01 || pitch < -.01) {
                entity.teleport(new Location(entity.getWorld(), x + playerLoc.getX(),y + playerLoc.getY(),z + playerLoc.getZ(),yaw + playerLoc.getYaw(),pitch + playerLoc.getPitch()));
                return;
            }
            //Movecraft.getInstance().getWorldHandler().addPlayerLocation((Player) entity,x,y,z,yaw,pitch);
            TeleportUtils.teleport((Player) entity, new Location(entity.getWorld(), playerLoc.getX() + x, playerLoc.getY() + y, playerLoc.getZ() + z));
        }


    }

    @Override
    public int hashCode() {
        return Objects.hash(craft, oldHitBox, x, y, z, pitch, yaw);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof EntityTranslateUpdateCommand)){
            return false;
        }
        EntityTranslateUpdateCommand other = (EntityTranslateUpdateCommand) obj;
        return this.x == other.x &&
                this.y == other.y &&
                this.z == other.z &&
                this.pitch == other.pitch &&
                this.yaw == other.yaw &&
                this.craft.equals(other.craft) &&
                this.oldHitBox.equals(other.oldHitBox);
    }

    private Collection<Entity> getEntitiesOnCraft(){
        final Location midpoint = oldHitBox.getMidPoint().toBukkit(craft.getW());
        final Collection<Entity> entities = craft.getW().getNearbyEntities(midpoint, oldHitBox.getXLength() / 2.0 + 1, oldHitBox.getYLength() / 2.0 + 2, oldHitBox.getZLength() / 2.0 + 1);
        final Set<Entity> toMove = new HashSet<>();
        for (Entity entity : entities){
            if (entity.getType() == EntityType.PLAYER) {
                if(craft.getSinking()){
                    continue;
                }
                toMove.add(entity);
            } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                toMove.add(entity);
            }
        }
        return toMove;
    }
}

