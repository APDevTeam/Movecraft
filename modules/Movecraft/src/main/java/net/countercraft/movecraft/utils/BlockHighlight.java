package net.countercraft.movecraft.utils;


import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.google.common.primitives.Ints;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerEntityDestroy;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerEntityMetadata;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Random;

/**
 * A set of utilities for highlighting block changes in the world
 */
public class BlockHighlight {
    private static final byte INVISIBLE = (byte) 0x20;
    private static final byte GLOWING = (byte) 0x40;
    private static final byte NOAI = (byte) 0x01;
    private static final int Mob_INDEX = 14;
    private static final int SLIME_INDEX = 15;

    public static int highlightBlockAt(Location location, Player player){
        var packet = new WrapperPlayServerSpawnEntityLiving();
        var id = new Random().nextInt();
        packet.setX(location.getX() + .5);
        packet.setY(location.getY());
        packet.setZ(location.getZ() + .5);
        packet.setType(EntityType.MAGMA_CUBE);
        packet.setEntityID(id);

        // Create metadata
        var metadata = new WrapperPlayServerEntityMetadata();
        metadata.setEntityID(id); // set id
        var watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
        watcher.setEntity(player); //Set the new data watcher's target
        watcher.setObject(0, Registry.get(Byte.class), (byte) (GLOWING ^ INVISIBLE)); //Set status to glowing and invisible
        watcher.setObject(Mob_INDEX, Registry.get(Byte.class), NOAI);
        var slimeData = new WrappedDataWatcherObject(SLIME_INDEX, Registry.get(Integer.class));
        watcher.setObject(slimeData, 2);
        metadata.setMetadata(watcher.getWatchableObjects());
//        var entity = packet.getEntity(location.getWorld());
//        entity.setGlowing(true);
//        if(!(entity instanceof LivingEntity)){
//            throw new IllegalStateException(entity + " must be magma cube, but was not.");
//        }
//        ((LivingEntity) entity).setInvisible(true);
        packet.sendPacket(player);
        metadata.sendPacket(player);
        return id;
    }

    public static void removeHighlights(Collection<Integer> ids, Player player){
        removeHighlights(Ints.toArray(ids), player);
    }

    public static void removeHighlights(int[] ids, Player player){
        var packet = new WrapperPlayServerEntityDestroy();
        packet.setEntityIds(ids);
        packet.sendPacket(player);
    }
}
