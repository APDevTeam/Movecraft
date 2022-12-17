package net.countercraft.movecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 */
public class ReflectUtils {
    public static @Nullable Object getHandle(Entity entity) {
        try {
            Method entity_getHandle = entity.getClass().getMethod("getHandle");
            return entity_getHandle.invoke(entity);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static @NotNull Field getField(@NotNull Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static @NotNull String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }
}
