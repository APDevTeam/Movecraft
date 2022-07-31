package net.countercraft.movecraft.util.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.17.1
 */
public class MojangMappedTeleport extends AbstractTeleport {
    private static Set<Object> teleportFlags;

    private static Method position;
    private static Method sendMethod;

    private static Constructor<?> vec3;
    private static Constructor<?> packet;

    private static Field connectionField;
    private static Field justTeleportedField;
    private static Field teleportPosField;
    private static Field lastPosXField;
    private static Field lastPosYField;
    private static Field lastPosZField;
    private static Field teleportAwaitField;
    private static Field awaitingTeleportTimeField;
    private static Field tickCountField;
    private static Field yaw;
    private static Field pitch;

    private static @NotNull Class<?> getNmClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft." + name);
    }

    private static void sendPacket(Object packet, Player p) {
        try {
            Object handle = TeleportUtils.getHandle(p);
            Object pConnection = connectionField.get(handle);
            sendMethod.invoke(pConnection, packet);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean initialize() {
        boolean sucess = false;
        try {
            Class<?> packetClass = getNmClass("network.protocol.Packet");
            Class<?> positionPacketClass = getNmClass("network.protocol.game.ClientboundPlayerPositionPacket");
            Class<?> entityClass = getNmClass("world.entity.Entity");
            Class<?> playerClass = getNmClass("server.level.ServerPlayer");
            Class<?> connectionClass = getNmClass("server.network.ServerGamePacketListenerImpl");
            Class<?> vectorClass = getNmClass("world.phys.Vec3");

            Object[] flags = getNmClass("network.protocol.game.ClientboundPlayerPositionPacket$RelativeArgument").getEnumConstants();
            teleportFlags = Set.of(flags[4], flags[3]); // X_ROT, Y_ROT

            position = entityClass.getDeclaredMethod("absMoveTo", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
            sendMethod = connectionClass.getMethod("send", packetClass);

            vec3 = vectorClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
            packet = positionPacketClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE, Boolean.TYPE);

            connectionField = TeleportUtils.getField(playerClass, "connection");
            justTeleportedField = TeleportUtils.getField(connectionClass, "justTeleported");
            teleportPosField = TeleportUtils.getField(connectionClass, "awaitingPositionFromClient");
            lastPosXField = TeleportUtils.getField(connectionClass, "lastPosX");
            lastPosYField = TeleportUtils.getField(connectionClass, "lastPosY");
            lastPosZField = TeleportUtils.getField(connectionClass, "lastPosZ");
            teleportAwaitField = TeleportUtils.getField(connectionClass, "awaitingTeleport");
            awaitingTeleportTimeField = TeleportUtils.getField(connectionClass, "awaitingTeleportTime");
            tickCountField = TeleportUtils.getField(connectionClass, "tickCount");
            yaw = TeleportUtils.getField(entityClass, "xRot");
            pitch = TeleportUtils.getField(entityClass, "yRot");
            sucess = true;
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return sucess;
    }

    public static void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Object handle = TeleportUtils.getHandle(player);
        try {
            position.invoke(handle, x, y, z, yaw.get(handle), pitch.get(handle));
            Object connection = connectionField.get(handle);
            justTeleportedField.set(connection, true);
            teleportPosField.set(connection, vec3.newInstance(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);
            int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            if (teleportAwait == Integer.MAX_VALUE)
                teleportAwait = 0;
            teleportAwaitField.setInt(connection, teleportAwait);
            awaitingTeleportTimeField.set(connection, tickCountField.get(connection));

            Object packetObject = packet.newInstance(x, y, z, yawChange, pitchChange, teleportFlags, teleportAwait, false);
            sendPacket(packetObject, player);
        }
        catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
