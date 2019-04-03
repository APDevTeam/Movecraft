package net.countercraft.movecraft;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.util.Vector;

public abstract class RegionUtils {
    public abstract Vector getRegionPoint(ProtectedRegion region, boolean minPoint);
}
