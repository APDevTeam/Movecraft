package net.countercraft.movecraft.repair;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.utils.LegacyUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepairUtils {


    public static boolean repairRegion(String regionName, World world){
        MovecraftRepair movecraftRepair = Movecraft.getInstance().getMovecraftRepair();
        Clipboard clipboard = movecraftRepair.loadRegionRepairStateClipboard(Movecraft.getInstance(), regionName, world);
        if (clipboard == null){
            return false;
        }
        int minX = clipboard.getMinimumPoint().getBlockX();
        int minY = clipboard.getMinimumPoint().getBlockY();
        int minZ = clipboard.getMinimumPoint().getBlockZ();
        int maxX = clipboard.getMaximumPoint().getBlockX();
        int maxY = clipboard.getMaximumPoint().getBlockY();
        int maxZ = clipboard.getMaximumPoint().getBlockZ();

        for (int x = minX; x <= maxX; x++){
            for (int y = minY; y <= maxY; y++){
                for (int z = minZ; z <= maxZ; z++){
                    if (Settings.IsLegacy){
                        Vector pos = new Vector(x,y,z);
                        BaseBlock bb = clipboard.getBlock(pos);
                        if (bb.isAir()){
                            continue;
                        }
                        if (Settings.AssaultDestroyableBlocks.contains(LegacyUtils.getMaterial(bb.getId()))){
                            if (!world.getChunkAt(x >> 4, z >> 4).isLoaded())
                                world.loadChunk(x >> 4, z >> 4);
                            if (world.getBlockAt(x, y, z).isEmpty() || world.getBlockAt(x, y, z).isLiquid()) {
                                MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                                WorldEditUpdateCommand updateCommand = new WorldEditUpdateCommand(bb, world, moveloc, LegacyUtils.getMaterial(bb.getType()), (byte) bb.getData());
                                MapUpdateManager.getInstance().scheduleUpdate(updateCommand);
                            }
                        }
                    } else {
                        BlockVector3 pos = BlockVector3.at(x,y,z);
                        BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
                    }
                }
            }
        }
        return true;
    }


}
