package net.countercraft.movecraft.utils;


import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.google.common.primitives.Ints;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerEntityDestroy;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerEntityMetadata;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerScoreboardTeam;
import net.countercraft.movecraft.utils.packets.WrapperPlayServerSpawnEntityLiving;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A set of utilities for highlighting block changes in the world
 */
public class BlockHighlight implements Listener {
    private static final byte INVISIBLE = (byte) 0x20;
    private static final byte GLOWING = (byte) 0x40;
    private static final byte NOAI = (byte) 0x01;
    private static final int MOB_INDEX = 14;
    private static final int SLIME_INDEX = 15;

    public static int highlightBlockAt(Location location, Player player){
        var packet = new WrapperPlayServerSpawnEntityLiving();
        var id = new Random().nextInt();
        var uuid = UUID.randomUUID();
        packet.setX(location.getX() + .5);
        packet.setY(location.getY());
        packet.setZ(location.getZ() + .5);
        packet.setType(EntityType.SHULKER);
        packet.setEntityID(id);
        packet.setUniqueId(uuid);

        // Create metadata
        var metadata = new WrapperPlayServerEntityMetadata();
        metadata.setEntityID(id); // set id
        var watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
        watcher.setEntity(player); //Set the new data watcher's target
        watcher.setObject(0, Registry.get(Byte.class), (byte) (GLOWING ^ INVISIBLE)); //Set status to glowing and invisible
        watcher.setObject(MOB_INDEX, Registry.get(Byte.class), NOAI);
//        var slimeData = new WrappedDataWatcherObject(SLIME_INDEX, Registry.get(Integer.class));
//        watcher.setObject(slimeData, 2);
        metadata.setMetadata(watcher.getWatchableObjects());
//        var entity = packet.getEntity(location.getWorld());
//        entity.setGlowing(true);
//        if(!(entity instanceof LivingEntity)){
//            throw new IllegalStateException(entity + " must be magma cube, but was not.");
//        }
//        ((LivingEntity) entity).setInvisible(true);
        packet.sendPacket(player);
        metadata.sendPacket(player);
        addToTeam(uuid).sendPacket(player);
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

    private static WrapperPlayServerScoreboardTeam createTeam(){
        var packet = new WrapperPlayServerScoreboardTeam();
        packet.setName("mvcrft_highlight");
        packet.setMode(0);
        packet.setDisplayName(WrappedChatComponent.fromText(""));
        packet.setNameTagVisibility("never");
        packet.setCollisionRule("never");
        packet.setPrefix(WrappedChatComponent.fromText("§"+ 13));
        packet.setSuffix(WrappedChatComponent.fromText(""));
        packet.setColor(ChatColor.RED);
        return packet;
    }

    private static WrapperPlayServerScoreboardTeam addToTeam(UUID id){
        var packet = new WrapperPlayServerScoreboardTeam();
        packet.setName("mvcrft_highlight");
        packet.setMode(3);
        packet.setPlayers(List.of(id.toString()));
        return packet;
    }

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent event){
        createTeam().sendPacket(event.getPlayer());
    }
}
