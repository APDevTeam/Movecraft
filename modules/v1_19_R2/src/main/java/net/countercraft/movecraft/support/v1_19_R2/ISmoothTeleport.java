package net.countercraft.movecraft.support.v1_19_R2;

import net.countercraft.movecraft.SmoothTeleport;
import net.countercraft.movecraft.util.ReflectUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Code derived from code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.19.3
 */
public class ISmoothTeleport extends SmoothTeleport {
    private Set<Object> teleportFlags;

    private Method positionMethod;
    private Method sendMethod;

    private Constructor<?> vec3Constructor;
    private Constructor<?> packetConstructor;

    private Field connectionField;
    private Field teleportPosField;
    private Field teleportAwaitField;
    private Field awaitingTeleportTimeField;
    private Field tickCountField;
    private Field yawField;
    private Field pitchField;

    private static @NotNull Class<?> getNmClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft." + name);
    }

    private void sendPacket(Object packet, Player p) {
        try {
            Object handle = ReflectUtils.getHandle(p);
            Object pConnection = connectionField.get(handle);
            sendMethod.invoke(pConnection, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean initialize() {
        boolean success = false;
        try {
            Class<?> packetClass = getNmClass("network.protocol.Packet");
            Class<?> positionPacketClass = getNmClass("network.protocol.game.PacketPlayOutPosition"); // ClientboundPlayerPositionPacket
            Class<?> entityClass = getNmClass("world.entity.Entity");
            Class<?> playerClass = getNmClass("server.level.EntityPlayer"); // ServerPlayer
            Class<?> connectionClass = getNmClass("server.network.PlayerConnection"); // ServerGamePacketListenerImpl
            Class<?> vectorClass = getNmClass("world.phys.Vec3D"); // Vec3

            Object[] flags = getNmClass("network.protocol.game.PacketPlayOutPosition$EnumPlayerTeleportFlags").getEnumConstants(); // $RelativeArgument
            teleportFlags = Set.of(flags[4], flags[3]); // X_ROT, Y_ROT

            positionMethod = entityClass.getDeclaredMethod("a", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE); // absMoveTo
            sendMethod = connectionClass.getMethod("a", packetClass); // send

            vec3Constructor = vectorClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
            packetConstructor = positionPacketClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE, Boolean.TYPE);

            connectionField = ReflectUtils.getField(playerClass, "b"); // connection
            teleportPosField = ReflectUtils.getField(connectionClass, "D"); // awaitingPositionFromClient
            teleportAwaitField = ReflectUtils.getField(connectionClass, "E"); // awaitingTeleport
            awaitingTeleportTimeField = ReflectUtils.getField(connectionClass, "F"); // awaitingTeleportTime
            tickCountField = ReflectUtils.getField(connectionClass, "j"); // tickCount
            yawField = ReflectUtils.getField(entityClass, "aB"); // xRot
            pitchField = ReflectUtils.getField(entityClass, "aA"); // yRot
            success = true;
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return success;
    }

    public void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Object handle = ReflectUtils.getHandle(player);
        try {
            positionMethod.invoke(handle, x, y, z, yawField.get(handle), pitchField.get(handle));
            Object connection = connectionField.get(handle);
            teleportPosField.set(connection, vec3Constructor.newInstance(x, y, z));
            int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            if (teleportAwait == Integer.MAX_VALUE)
                teleportAwait = 0;
            teleportAwaitField.setInt(connection, teleportAwait);
            awaitingTeleportTimeField.set(connection, tickCountField.get(connection));

            Object packet = packetConstructor.newInstance(x, y, z, yawChange, pitchChange, teleportFlags, teleportAwait, false);
            sendPacket(packet, player);
        }
        catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
