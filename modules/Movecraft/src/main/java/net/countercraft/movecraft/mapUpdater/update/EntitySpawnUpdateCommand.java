package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Set;

public class EntitySpawnUpdateCommand extends UpdateCommand {
    @NotNull private final int spawnRange;
    @NotNull private final int maxEntities;
    @NotNull private final Location loc;
    @NotNull private final Set<EntityType> entitiesToSpawn;

    public EntitySpawnUpdateCommand(@NotNull int spawnRange, @NotNull int maxEntities, @NotNull Location loc, @NotNull Set<EntityType> entitiesToSpawn){
        this.spawnRange = spawnRange;
        this.maxEntities = maxEntities;
        this.loc = loc;
        this.entitiesToSpawn = entitiesToSpawn;
    }
    @Override
    public void doUpdate() {
        //Get locations where entities can be spawned
        @NotNull ArrayDeque<Location> spawnLocs = new ArrayDeque<>();
        int minX = loc.getBlockX() - spawnRange;
        int minZ = loc.getBlockZ() - spawnRange;
        int maxX = loc.getBlockX() + spawnRange;
        int maxZ = loc.getBlockZ() + spawnRange;
        for (int i = 1 ; i <= maxEntities; i++){
            final int randX = MathUtils.randomInteger(minX,maxX);
            final int randZ = MathUtils.randomInteger(minZ,maxZ);
            final int y =loc.getBlockY();
            @NotNull final Location spawnLoc = new Location(loc.getWorld(),randX,y,randZ);
            for (int dy = 0 ; dy <= 255 - y; dy++){
                if (loc.getWorld().getBlockAt(randX,y + dy, randZ).getType().name().endsWith("AIR")){
                    spawnLoc.add(0,dy,0);
                    break;
                }

            }
            spawnLocs.addLast(spawnLoc);
        }
        //Spawn entities at the determined locations
        while (!spawnLocs.isEmpty()){
            for (EntityType entity : entitiesToSpawn){
                @NotNull Location poll = spawnLocs.pollFirst();
                if (poll == null)
                    continue;
                loc.getWorld().spawnEntity(poll,entity);
            }
        }
    }
}
