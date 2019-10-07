package net.countercraft.movecraft.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.UUID;

public class WorldguardUtils {
    public static String getRegionOwnerList(ProtectedRegion tRegion) {
        StringBuilder output = new StringBuilder();
        if (tRegion == null)
            return output.toString();
        boolean first = true;
        if (tRegion.getOwners().getUniqueIds().size() > 0) {
            for (UUID uid : tRegion.getOwners().getUniqueIds()) {
                if (!first)
                    output.append(", ");
                else
                    first = false;
                OfflinePlayer offP = Bukkit.getOfflinePlayer(uid);
                if (offP.getName() == null)
                    output.append(uid.toString());
                else
                    output.append(offP.getName());
            }
        }
        if (tRegion.getOwners().getPlayers().size() > 0) {
            for (String player : tRegion.getOwners().getPlayers()) {
                if (!first)
                    output.append(",");
                else
                    first = false;
                output.append(player);
            }
        }
        return output.toString();
    }
    public static boolean pvpAllowed(ProtectedRegion region){
        if (Settings.IsLegacy) {
            return region.getFlag(DefaultFlag.PVP)  == StateFlag.State.ALLOW;
        }
        return region.getFlag(Flags.PVP) == StateFlag.State.ALLOW;
    }


    public static boolean explosionsPermitted(Location loc){
        ApplicableRegionSet set;
        if (Settings.IsLegacy) {
            RegionManager rm = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), loc.getWorld());
            set = LegacyUtils.getApplicableRegions(rm, loc);
            if (!LegacyUtils.allows(set, DefaultFlag.OTHER_EXPLOSION)) {
                return false;
            }
        } else {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.world.World weWorld = new BukkitWorld(loc.getWorld());
            com.sk89q.worldedit.util.Location wgLoc = new com.sk89q.worldedit.util.Location(weWorld, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            set = query.getApplicableRegions(wgLoc);
            for (ProtectedRegion region : set.getRegions()){
                if (region.getFlag(Flags.OTHER_EXPLOSION) == StateFlag.State.DENY){
                    return false;
                }
            }

        }
        return true;
    }
    public static boolean allowFireSpread(Location loc){
        ApplicableRegionSet set;
        if (Settings.IsLegacy){
            set = LegacyUtils.getApplicableRegions(LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), loc.getWorld()), loc);//.getApplicableRegions();
            return LegacyUtils.allows(set, DefaultFlag.FIRE_SPREAD);

        } else {
            set = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld())).getApplicableRegions(BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            for (ProtectedRegion region : set){
                if (region.getFlag(Flags.FIRE_SPREAD) == StateFlag.State.DENY){
                    return false;
                }
            }
        }
        return true;
    }
    public static ApplicableRegionSet getRegionsAt(Location loc){
        World world = loc.getWorld();
        ApplicableRegionSet regions;
        if (Settings.IsLegacy) {
            RegionManager rm = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), world);
            regions = LegacyUtils.getApplicableRegions(rm, loc);
        } else {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
            com.sk89q.worldedit.util.Location wgLoc = new com.sk89q.worldedit.util.Location(weWorld, loc.getX(), loc.getY(), loc.getZ());
            regions = query.getApplicableRegions(wgLoc);
        }
        return regions;
    }
    public static RegionManager getRegionManager(World world){
        RegionManager manager;
        if (Settings.IsLegacy){
            try {
                manager = LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), world);
            } catch (Throwable t){
                manager = null;
            }
        } else {
            com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
            manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(weWorld);
        }
        return manager;
    }
    public Flag flag(String string){
        return null;
    }

}
