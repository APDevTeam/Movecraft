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
 * Code derived from code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.19.1
 */
public class V1_19Teleport extends AbstractTeleport {
    private static Set<Object> teleportFlags;

    private static Method positionMethod;
    private static Method sendMethod;

    private static Constructor<?> vec3Constructor;
    private static Constructor<?> packetConstructor;

    private static Field connectionField;
    //private static Field justTeleportedField;
    private static Field teleportPosField;
    //private static Field lastPosXField;
    //private static Field lastPosYField;
    //private static Field lastPosZField;
    private static Field teleportAwaitField;
    private static Field awaitingTeleportTimeField;
    private static Field tickCountField;
    private static Field yawField;
    private static Field pitchField;

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

            connectionField = TeleportUtils.getField(playerClass, "b"); // connection
            //justTeleportedField = TeleportUtils.getField(connectionClass, "justTeleported"); // justTeleported
            teleportPosField = TeleportUtils.getField(connectionClass, "C"); // awaitingPositionFromClient
            //lastPosXField = TeleportUtils.getField(connectionClass, "lastPosX"); // lastPosX
            //lastPosYField = TeleportUtils.getField(connectionClass, "lastPosY"); // lastPosY
            //lastPosZField = TeleportUtils.getField(connectionClass, "lastPosZ"); // lastPosZ
            teleportAwaitField = TeleportUtils.getField(connectionClass, "D"); // awaitingTeleport
            awaitingTeleportTimeField = TeleportUtils.getField(connectionClass, "E"); // awaitingTeleportTime
            tickCountField = TeleportUtils.getField(connectionClass, "i"); // tickCount
            yawField = TeleportUtils.getField(entityClass, "aB"); // xRot
            pitchField = TeleportUtils.getField(entityClass, "aA"); // yRot
            success = true;
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Object handle = TeleportUtils.getHandle(player);
        try {
            positionMethod.invoke(handle, x, y, z, yawField.get(handle), pitchField.get(handle));
            Object connection = connectionField.get(handle);
            //justTeleportedField.set(connection, true);
            teleportPosField.set(connection, vec3Constructor.newInstance(x, y, z));
            //lastPosXField.set(connection, x);
            //lastPosYField.set(connection, y);
            //lastPosZField.set(connection, z);
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
