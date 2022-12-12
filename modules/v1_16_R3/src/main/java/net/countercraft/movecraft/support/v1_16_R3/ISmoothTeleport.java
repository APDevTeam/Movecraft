package net.countercraft.movecraft.support.v1_16_R3;

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
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 * Used for 1.14.4 to 1.16.5
 */
public class ISmoothTeleport extends SmoothTeleport {
    private Set<Object> teleportFlags;

    private Constructor packetConstructor;
    private Constructor vec3D;

    private Method position;
    private Method sendMethod;

    private Field connectionField;
    private Field justTeleportedField;
    private Field teleportPosField;
    private Field lastPosXField;
    private Field lastPosYField;
    private Field lastPosZField;
    private Field teleportAwaitField;
    private Field AField;
    private Field eField;
    private Field yaw;
    private Field pitch;

    private static @NotNull Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + ReflectUtils.getVersion() + "." + name);
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
            Class<?> packet = getNmsClass("Packet");
            Class<?> entity = getNmsClass("Entity");
            Class<?> entityPlayer = getNmsClass("EntityPlayer");
            Class<?> connectionClass = getNmsClass("PlayerConnection");
            Class<?> packetClass = getNmsClass("PacketPlayOutPosition");
            Class<?> vecClass = getNmsClass("Vec3D");
            sendMethod = connectionClass.getMethod("sendPacket", packet);

            position = entity.getDeclaredMethod("setLocation", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);

            yaw = ReflectUtils.getField(entity, "yaw");
            pitch = ReflectUtils.getField(entity, "pitch");
            connectionField = ReflectUtils.getField(entityPlayer, "playerConnection");

            packetConstructor = packetClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE);
            vec3D = vecClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);

            Object[] enumObjects = getNmsClass("PacketPlayOutPosition$EnumPlayerTeleportFlags").getEnumConstants();
            teleportFlags = Set.of(enumObjects[4], enumObjects[3]);

            justTeleportedField = ReflectUtils.getField(connectionClass, "justTeleported");
            teleportPosField = ReflectUtils.getField(connectionClass, "teleportPos");
            lastPosXField = ReflectUtils.getField(connectionClass, "lastPosX");
            lastPosYField = ReflectUtils.getField(connectionClass, "lastPosY");
            lastPosZField = ReflectUtils.getField(connectionClass, "lastPosZ");
            teleportAwaitField = ReflectUtils.getField(connectionClass, "teleportAwait");
            AField = ReflectUtils.getField(connectionClass, "A");
            eField = ReflectUtils.getField(connectionClass, "e");
            success = true;
        }
        catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | SecurityException e) {
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
