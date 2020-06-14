package net.countercraft.movecraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 */
public class TeleportUtils {
    private static Set<Object> teleportFlags;

    private static Constructor packetConstructor;
    private static Constructor vehiclePacketConstructor;
    private static Constructor vec3D;

    private static Method position;
    private static Method closeInventory;
    private static Method sendMethod;
    private static Method getBukkitEntity;
    private static Method spawnIn;

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
    private static Field activeContainer;
    private static Field defaultContainer;

    static {
        Class<?> packet = getNmsClass("Packet");
        Class<?> entity = getNmsClass("Entity");
        Class<?> entityPlayer = getNmsClass("EntityPlayer");
        Class<?> entityHuman = getNmsClass("EntityHuman");
        Class<?> connectionClass = getNmsClass("PlayerConnection");
        Class<?> packetClass = getNmsClass("PacketPlayOutPosition");
        Class<?> vehiclePacket = getNmsClass("PacketPlayOutVehicleMove");
        Class<?> vecClass = getNmsClass("Vec3D");
        Class<?> worldClass = getNmsClass("World");
        try {
            sendMethod = connectionClass.getMethod("sendPacket", packet);

            position = entity.getDeclaredMethod("setLocation", Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE);
            closeInventory = entityPlayer.getDeclaredMethod("closeInventory");
            getBukkitEntity = entity.getDeclaredMethod("getBukkitEntity");
            spawnIn = entity.getDeclaredMethod("spawnIn", worldClass);

            yaw = getField(entity, "yaw");
            pitch = getField(entity, "pitch");
            connectionField = getField(entityPlayer, "playerConnection");
            activeContainer = getField(entityHuman, "activeContainer");
            defaultContainer = getField(entityHuman, "defaultContainer");

            packetConstructor = packetClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Set.class, Integer.TYPE);
            vec3D = vecClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);

            vehiclePacketConstructor = vehiclePacket.getConstructor(entity);

            Object[] enumObjects = getNmsClass("PacketPlayOutPosition$EnumPlayerTeleportFlags").getEnumConstants();
            teleportFlags = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(enumObjects[4], enumObjects[3])));

            justTeleportedField = getField(connectionClass, "justTeleported");
            teleportPosField = getField(connectionClass, "teleportPos");
            lastPosXField = getField(connectionClass, "lastPosX");
            lastPosYField = getField(connectionClass, "lastPosY");
            lastPosZField = getField(connectionClass, "lastPosZ");
            teleportAwaitField = getField(connectionClass, "teleportAwait");
            AField = getField(connectionClass, "A");
            eField = getField(connectionClass, "e");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void teleportEntity(Entity entity, Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Entity vehicle = entity.getVehicle();
        Object handle = getHandle(vehicle == null ? entity : vehicle);
        try {
            if (location.getWorld() != entity.getWorld()) {
                Method wHandle = location.getWorld().getClass().getDeclaredMethod("getHandle");
                spawnIn.invoke(handle, wHandle.invoke(location.getWorld()));
            }
            position.invoke(handle, x,y,z, location.getYaw(), location.getPitch());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void teleport(Player player, Location location, float yawChange) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        Entity vehicle = player.getVehicle();
        Object handle = getHandle(vehicle == null ? player : vehicle);
        Object pHandle = getHandle(player);

        try {
            if (location.getWorld() != player.getWorld()) {
                Method wHandle = location.getWorld().getClass().getDeclaredMethod("getHandle");
                spawnIn.invoke(handle, wHandle.invoke(location.getWorld()));
            }
            position.invoke(handle, x,y,z, yaw.get(handle), pitch.get(handle));
            yaw.set(handle, yaw.getFloat(handle) + yawChange);
            Object connection = connectionField.get(pHandle);
            justTeleportedField.set(connection, true);
            teleportPosField.set(connection, vec3D.newInstance(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);
            int teleportAwait = teleportAwaitField.getInt(connection) + 1;
            if(teleportAwait == 2147483647) teleportAwait = 0;
            teleportAwaitField.set(connection, teleportAwait);
            AField.set(connection, eField.get(connection));

            Object packet = packetConstructor.newInstance(x, y, z, yawChange, 0, teleportFlags, teleportAwait);
            sendPacket(packet, player);
            if (vehicle == null)
                return;
            Object vehiclePacket = vehiclePacketConstructor.newInstance(handle);
            sendPacket(vehiclePacket, player);
            List<Entity> passengers;
            try {
                passengers = (List<Entity>) Entity.class.getDeclaredMethod("getPassengers").invoke(vehicle);
            } catch (Exception e) {
                return;
            }
            for (Entity pass : passengers) {
                if (pass.getType() != EntityType.PLAYER || pass.equals(player)) {
                    continue;
                }
                Player p = (Player) pass;
                sendPacket(packet, p);
                sendPacket(vehiclePacket, p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendPacket(Object packet, Player p) {
        try {
            Object handle = getHandle(p);
            Object pConnection = connectionField.get(handle);
            sendMethod.invoke(pConnection, packet);
        } catch (Exception var9) {
            var9.printStackTrace();
        }
    }

    private static Object getHandle(Entity entity) {
        try {
            Method entity_getHandle = entity.getClass().getMethod("getHandle");
            return entity_getHandle.invoke(entity);
        } catch (Exception var2) {
            var2.printStackTrace();
            return null;
        }
    }
    private static Class<?> getNmsClass(String name) {
        Class clazz = null;

        try {
            clazz = Class.forName("net.minecraft.server." + getVersion() + "." + name);
        } catch (ClassNotFoundException var3) {
            var3.printStackTrace();
        }

        return clazz;
    }

    private static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }
}
