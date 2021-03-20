package net.countercraft.movecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Entities {

    private static Method getID = null;
    private static Method getType = null;
    private static Constructor<?> minecraftKey = null;
    private static Object registryInstance = null;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        try {
            Field registry = Class.forName("net.minecraft.server." + version + ".IRegistry").getField("ENTITY_TYPE");
            registryInstance = registry.get(null);
            getID = registry.getDeclaringClass().getMethod("a", Object.class);
            var minecraftKeyClass = Class.forName("net.minecraft.server." + version + ".MinecraftKey");
            getType = registry.getDeclaringClass().getMethod("get", minecraftKeyClass);

            minecraftKey = minecraftKeyClass.getConstructor(String.class);
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Spigot has stopped updating their internal entity ID's, thus breaking any context in which their use is required
     * This will get the actual entity id of an EntityType, from the servers EntityType registry
     * @param type entity type to get an id from
     * @return correct entity id
     */
    @SuppressWarnings("deprecation")
    public static int getTypeID(EntityType type){
        try {
            var key = minecraftKey.newInstance(type.getName());
            var baseType = getType.invoke(registryInstance, key);
            return (int) getID.invoke(registryInstance, baseType);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
