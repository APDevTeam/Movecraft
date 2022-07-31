package net.countercraft.movecraft.util.teleport;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Code taken with permission from MicleBrick
 * https://www.spigotmc.org/threads/teleport-player-smoothly.317416/
 */
public class TeleportUtils {
    protected static @Nullable Object getHandle(Entity entity) {
        try {
            Method entity_getHandle = entity.getClass().getMethod("getHandle");
            return entity_getHandle.invoke(entity);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected static @NotNull Field getField(@NotNull Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    protected static @NotNull String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().substring(23);
    }

    private enum Mode {
        UNINITIALIZED,
        FALLBACK,
        SPIGOT_MAPPED,
        MOJANG_MAPPED
    }
    private static Mode mode = Mode.UNINITIALIZED;

    public static void teleport(Player player, @NotNull Location location, float yawChange, float pitchChange) {
        if (mode == Mode.UNINITIALIZED) {
            // initialize
        }
        switch (mode) {
            case SPIGOT_MAPPED:
                teleportSpigotMapped(player, location, yawChange, pitchChange);
                break;
            case MOJANG_MAPPED:
                teleportMojangMapped(player, location, yawChange, pitchChange);
                break;
            case FALLBACK:
                Movecraft.getInstance().getWorldHandler().addPlayerLocation(player,
                        location.getX() - player.getLocation().getX(),
                        location.getY() - player.getLocation().getY(),
                        location.getZ() - player.getLocation().getZ(),
                        yawChange, pitchChange
                );
                break;
            default:
                throw new IllegalStateException("Unknown mode: " + mode);
        }
    }
}
