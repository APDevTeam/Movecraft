package net.countercraft.movecraft.sync;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.update.EntityMoveUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.Set;

public class EntityProcessor {

    private EntityProcessor(){

    }
    public static Set<UpdateCommand> translateEntities(Craft craft, int dx, int dy, int dz){
        HashHitBox oldHitBox = craft.getHitBox();
        final HashSet<UpdateCommand> updateCmds = new HashSet<>();
        if (oldHitBox.isEmpty())
            return updateCmds;

        Location midpoint = new Location(
                craft.getW(),
                (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
        for (Entity entity : craft.getW().getNearbyEntities(midpoint, oldHitBox.getXLength() / 2.0 + 1, oldHitBox.getYLength() / 2.0 + 2, oldHitBox.getZLength() / 2.0 + 1)) {
            if (entity.getType() == EntityType.PLAYER) {
                if(craft.getSinking()){
                    continue;
                }
                EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, dx, dy, dz, 0, 0);
                updateCmds.add(eUp);
            } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, dx, dy, dz,  0, 0);
                updateCmds.add(eUp);
            }
        }
        return updateCmds;
    }

    public static Set<UpdateCommand> rotateEntities(Craft craft, MovecraftLocation originPoint, Rotation rotation){
        HashHitBox oldHitBox = craft.getHitBox();
        final HashSet<UpdateCommand> updates = new HashSet<>();
        if (oldHitBox.isEmpty())
            return updates;
        Location tOP = new Location(craft.getW(), originPoint.getX(), originPoint.getY(), originPoint.getZ());
        tOP.setX(tOP.getBlockX() + 0.5);
        tOP.setZ(tOP.getBlockZ() + 0.5);

        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            Location midpoint = new Location(
                    craft.getW(),
                    (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                    (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                    (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
            for(Entity entity : craft.getW().getNearbyEntities(midpoint, oldHitBox.getXLength()/2.0 + 1, oldHitBox.getYLength()/2.0 + 2, oldHitBox.getZLength()/2.0 + 1)){
                if ((entity.getType() == EntityType.PLAYER && !craft.getSinking()) || !craft.getType().getOnlyMovePlayers()) {
                    // Player is onboard this craft

                    Location adjustedPLoc = entity.getLocation().subtract(tOP);

                    double[] rotatedCoords = MathUtils.rotateVecNoRound(rotation, adjustedPLoc.getX(), adjustedPLoc.getZ());
                    float newYaw = rotation == Rotation.CLOCKWISE ? 90F : -90F;
                    EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, rotatedCoords[0] + tOP.getX() - entity.getLocation().getX(), 0, rotatedCoords[1] + tOP.getZ() - entity.getLocation().getZ(), newYaw, 0);
                    updates.add(eUp);
                }
            }
        }
        return updates;
    }
}
