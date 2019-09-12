package net.countercraft.movecraft.sync;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.EntityMoveUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;

public class EntityProcessor extends BukkitRunnable {
    private final Craft craft;
    private final MovecraftLocation displacement;

    public EntityProcessor(Craft craft, MovecraftLocation displacement) {
        this.craft = craft;
        this.displacement = displacement;
    }

    @Override
    public void run() {
        LinkedList<UpdateCommand> updates = new LinkedList<>();
        HashHitBox oldHitBox = craft.getHitBox();
        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
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
                    EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, displacement.getX(), displacement.getY(), displacement.getZ(), 0, 0);
                    updates.add(eUp);
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, displacement.getX(), displacement.getY(), displacement.getZ(), 0, 0);
                    updates.add(eUp);
                }
            }
        } else {
            //add releaseTask without playermove to manager
            if (!craft.getType().getCruiseOnPilot() && !craft.getSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
        }
        MapUpdateManager.getInstance().scheduleUpdates(updates);
    }
}
