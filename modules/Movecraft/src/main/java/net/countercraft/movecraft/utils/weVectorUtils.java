package net.countercraft.movecraft.utils;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class weVectorUtils {
    private static Method getMinimumPoint;
    private static Method getMaximumPoint;
    static {
        try {
            getMinimumPoint = ProtectedRegion.class.getDeclaredMethod("getMinimumPoint");
            getMaximumPoint = ProtectedRegion.class.getDeclaredMethod("getMaximumPoint");
        } catch (NoSuchMethodException e) {
            getMaximumPoint = null;
            getMinimumPoint = null;
        }
    }
    public static org.bukkit.util.Vector getMinimumPoint(ProtectedRegion region) {
        org.bukkit.util.Vector ret = null;
        if (Settings.IsLegacy) {
            ret = LegacyWEUtils.getMinimumPoint(region);

        } else {

            try {
                BlockVector3 min = getMinimumPoint != null ? (BlockVector3) getMinimumPoint.invoke(region) : null;
                if (min != null) {
                    ret = new org.bukkit.util.Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static org.bukkit.util.Vector getMaximumPoint(ProtectedRegion region){
        org.bukkit.util.Vector ret = null;
        if (Settings.IsLegacy) {
            ret = LegacyWEUtils.getMaximumPoint(region);

        } else {
            try {
                BlockVector3 min = getMaximumPoint != null ? (BlockVector3) getMaximumPoint.invoke(region) : null;
                if (min != null) {
                    ret = new Vector(min.getBlockX(), min.getBlockY(), min.getBlockZ());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        return ret;
    }
}
