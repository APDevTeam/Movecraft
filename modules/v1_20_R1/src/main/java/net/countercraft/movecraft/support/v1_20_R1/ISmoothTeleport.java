package net.countercraft.movecraft.support.v1_20_R1;

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
 * Used for 1.19.4
 */
public class ISmoothTeleport extends SmoothTeleport {
    private final Set<Object> teleportFlags;

    private final Method positionMethod;
    private final Method sendMethod;

    private final Constructor<?> vec3Constructor;
    private final Constructor<?> packetConstructor;

    private final Field connectionField;
    private final Field teleportPosField;
    private final Field teleportAwaitField;
    private final Field awaitingTeleportTimeField;
    private final Field tickCountField;
    private final Field yawField;
    private final Field pitchField;

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

    public ISmoothTeleport() throws NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
        Class<?> packetClass = getNmClass("network.protocol.Packet");
        Class<?> positionPacketClass = getNmClass("network.protocol.game.PacketPlayOutPosition"); // ClientboundPlayerPositionPacket
        Class<?> entityClass = getNmClass("world.entity.Entity");
        Class<?> playerClass = getNmClass("server.level.EntityPlayer"); // ServerPlayer
        Class<?> connectionClass = getNmClass("server.network.PlayerConnection"); // ServerGamePacketListenerImpl
        Class<?> vectorClass = getNmClass("world.phys.Vec3D"); // Vec3

        Object[] flags = getNmClass("world.entity.RelativeMovement").getEnumConstants();
        teleportFlags = Set.of(flags[4], flags[3]); // X_ROT, Y_ROT

        positionMethod = entityClass.getDeclaredMethod("a", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE); // absMoveTo
        sendMethod = connectionClass.getMethod("a", packetClass); // send

        vec3Constructor = vectorClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
        packetConstructor = positionPacketClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE);

        connectionField = ReflectUtils.getField(playerClass, "b"); // connection
        teleportPosField = ReflectUtils.getField(connectionClass, "D"); // awaitingPositionFromClient
        teleportAwaitField = ReflectUtils.getField(connectionClass, "E"); // awaitingTeleport
        awaitingTeleportTimeField = ReflectUtils.getField(connectionClass, "F"); // awaitingTeleportTime
        tickCountField = ReflectUtils.getField(connectionClass, "j"); // tickCount
        yawField = ReflectUtils.getField(entityClass, "aF"); // xRot
        pitchField = ReflectUtils.getField(entityClass, "aE"); // yRot
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

            Object packet = packetConstructor.newInstance(x, y, z, yawChange, pitchChange, teleportFlags, teleportAwait);
            sendPacket(packet, player);
        }
        catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
