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
 * Used for 1.14.4 to 1.16.5
 */
public class SpigotMappedTeleport extends AbstractTeleport {
    private static Set<Object> teleportFlags;

    private static Constructor packetConstructor;
    private static Constructor vec3D;

    private static Method position;
    private static Method sendMethod;

    private static Field connectionField;
    private static Field justTeleportedField;
    private static Field teleportPosField;
    private static Field lastPosXField;
    private static Field lastPosYField;
    private static Field lastPosZField;
    private static Field teleportAwaitField;
    private static Field AField;
    private static Field eField;
    private static Field yaw;
    private static Field pitch;

    private static @NotNull Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + TeleportUtils.getVersion() + "." + name);
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
            Class<?> packet = getNmsClass("Packet");
            Class<?> entity = getNmsClass("Entity");
            Class<?> entityPlayer = getNmsClass("EntityPlayer");
            Class<?> connectionClass = getNmsClass("PlayerConnection");
            Class<?> packetClass = getNmsClass("PacketPlayOutPosition");
            Class<?> vecClass = getNmsClass("Vec3D");
            sendMethod = connectionClass.getMethod("sendPacket", packet);

            position = entity.getDeclaredMethod("setLocation", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);

            yaw = TeleportUtils.getField(entity, "yaw");
            pitch = TeleportUtils.getField(entity, "pitch");
            connectionField = TeleportUtils.getField(entityPlayer, "playerConnection");

            packetConstructor = packetClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE);
            vec3D = vecClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);

            Object[] enumObjects = getNmsClass("PacketPlayOutPosition$EnumPlayerTeleportFlags").getEnumConstants();
            teleportFlags = Set.of(enumObjects[4], enumObjects[3]);

            justTeleportedField = TeleportUtils.getField(connectionClass, "justTeleported");
            teleportPosField = TeleportUtils.getField(connectionClass, "teleportPos");
            lastPosXField = TeleportUtils.getField(connectionClass, "lastPosX");
            lastPosYField = TeleportUtils.getField(connectionClass, "lastPosY");
            lastPosZField = TeleportUtils.getField(connectionClass, "lastPosZ");
            teleportAwaitField = TeleportUtils.getField(connectionClass, "teleportAwait");
            AField = TeleportUtils.getField(connectionClass, "A");
            eField = TeleportUtils.getField(connectionClass, "e");
            sucess = true;
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | SecurityException e) {
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
            teleportPosField.set(connection, vec3D.newInstance(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);
            int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            if (teleportAwait == Integer.MAX_VALUE)
                teleportAwait = 0;
            teleportAwaitField.set(connection, teleportAwait);
            AField.set(connection, eField.get(connection));

            Object packet = packetConstructor.newInstance(x, y, z, yawChange, pitchChange, teleportFlags, teleportAwait);
            sendPacket(packet, player);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
