package net.countercraft.movecraft.utils;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.util.Vector;

public class LegacyWEUtils {
    public static Vector getMinimumPoint(ProtectedRegion region){
        com.sk89q.worldedit.Vector weMin = region.getMaximumPoint();
        return WorldEditUtils.toBukkitVector(weMin);
    }
    public static Vector getMaximumPoint(ProtectedRegion region){
        com.sk89q.worldedit.Vector weMax = region.getMaximumPoint();
        return WorldEditUtils.toBukkitVector(weMax);
    }
}
