package net.countercraft.movecraft.warfare.assault;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.WorldEdit7UpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.LegacyWEUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

public class AssaultUtils {

    public static boolean areDefendersOnline(ProtectedRegion tRegion) {
        int numOnline = 0;

        if(Settings.AssaultRequiredOwnersOnline > 0) {
            for (UUID playerID : tRegion.getOwners().getUniqueIds()) {
                if (Bukkit.getPlayer(playerID) != null) {
                    numOnline++;

                    if(numOnline > Settings.AssaultRequiredOwnersOnline) {
                        break;
                    }
                }
            }

            if (numOnline < Settings.AssaultRequiredOwnersOnline) {
                return false;
            }
        }

        numOnline = 0;
        if(Settings.AssaultRequiredOwnersOnline > 0) {
            for (UUID playerID : tRegion.getMembers().getUniqueIds()) {
                if (Bukkit.getPlayer(playerID) != null) {
                    numOnline++;

                    if(numOnline > Settings.AssaultRequiredDefendersOnline) {
                        return true;
                    }
                }
            }

            if (numOnline < Settings.AssaultRequiredDefendersOnline) {
                return false;
            }
        }
        return numOnline >= Settings.AssaultRequiredDefendersOnline;
    }

    public static double getCostToAssault(ProtectedRegion tRegion) {
        return getAssaultBalance(tRegion) * Settings.AssaultCostPercent;
    }

    public static double getMaxDamages(ProtectedRegion tRegion) {

        return getAssaultBalance(tRegion) * Settings.AssaultDamagesCapPercent;
    }

    private static double getAssaultBalance(ProtectedRegion tRegion) {
        return getOwnerBalance(tRegion) + getMemberBalance(tRegion);
    }

    private static double getOwnerBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerID : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerID);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultOwnerWeightPercent / 100.0);
    }

    private static double getMemberBalance(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        double total = 0.0;
        for (UUID playerID : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerID);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > Settings.AssaultMaxBalance)
                    total += Settings.AssaultMaxBalance;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * (Settings.AssaultMemberWeightPercent / 100.0);
    }

    public static boolean repairRegion(World world, String regionName) {
        Clipboard clipboard = Movecraft.getInstance().getMovecraftRepair().loadRegionRepairStateClipboard(Movecraft.getInstance(),regionName,world);
        if (clipboard == null){
            return false;
        }
        int minX = clipboard.getMinimumPoint().getX();
        int minY = clipboard.getMinimumPoint().getY();
        int minZ = clipboard.getMinimumPoint().getZ();
        int maxX = clipboard.getMaximumPoint().getX();
        int maxY = clipboard.getMaximumPoint().getY();
        int maxZ = clipboard.getMaximumPoint().getZ();
        LinkedList<UpdateCommand> updateCommands = new LinkedList<>();
        for (int x = minX; x <= maxX ; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ ; z <= maxZ ; z++){
                    Material type;
                    if (Settings.IsLegacy){
                        Vector position = new Vector(x,y,z);
                        com.sk89q.worldedit.blocks.BaseBlock bb = LegacyWEUtils.getBlock(clipboard,position);
                        type = LegacyUtils.getMaterial(bb.getType());
                        byte data = (byte) bb.getData();
                        if (type.name().endsWith("AIR")) {
                            continue;
                        }
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != type){
                            updateCommands.add(new WorldEditUpdateCommand(bb,world,new MovecraftLocation(x,y,z),type,data));
                        }
                    } else {
                        BlockVector3 position = BlockVector3.at(x, y, z);
                        BaseBlock bb = clipboard.getFullBlock(position);
                        type = BukkitAdapter.adapt(bb.getBlockType());
                        BlockData bdata = BukkitAdapter.adapt(bb);
                        if (type.name().endsWith("AIR")) {
                            continue;
                        }
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != type){
                            updateCommands.add(new WorldEdit7UpdateCommand(bb,world,new MovecraftLocation(x,y,z),type));
                        }
                    }
                }
            }
        }
        if (!updateCommands.isEmpty()){
            MapUpdateManager.getInstance().scheduleUpdates(updateCommands);
        }
        return true;
    }
}
