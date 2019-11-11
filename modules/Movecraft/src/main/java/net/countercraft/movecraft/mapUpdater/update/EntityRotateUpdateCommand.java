package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityRotateUpdateCommand extends UpdateCommand {

    private final Craft craft;
    private final HitBox oldHitBox;
    private final Rotation rotation;
    private final MovecraftLocation originPoint;

    public EntityRotateUpdateCommand(Craft craft, HitBox oldHitBox, Rotation rotation, MovecraftLocation originPoint) {
        this.craft = craft;
        this.oldHitBox = oldHitBox;
        this.rotation = rotation;
        this.originPoint = originPoint;
    }


    @Override
    public void doUpdate() {
        final Map<Entity, Location> entities = getEntitiesOnCraft();
        for (Entity entity : entities.keySet()){
            final Location tp = entities.get(entity);
            assert tp != null;
            entity.teleport(tp);
        }


    }
    private Map<Entity, Location> getEntitiesOnCraft() {
        HashMap<Entity, Location> ret = new HashMap<>();
        if (oldHitBox.isEmpty())
            return Collections.emptyMap();
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
                    Location tpLoc = new Location(craft.getW(), rotatedCoords[0] + tOP.getX(), entity.getLocation().getY(), rotatedCoords[1] + tOP.getZ(), newYaw + entity.getLocation().getYaw() , entity.getLocation().getPitch());
                    ret.put(entity, tpLoc);

                }
            }
        }
        return ret;
    }
}
