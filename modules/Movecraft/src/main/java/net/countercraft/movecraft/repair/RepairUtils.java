package net.countercraft.movecraft.repair;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.utils.LegacyUtils;
import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepairUtils {
    private static Method GET_BLOCK;
    static {
        try {
            GET_BLOCK = BlockArrayClipboard.class.getDeclaredMethod("getBlock", Vector.class);
        } catch (NoSuchMethodException e) {
            GET_BLOCK = null;
        }
    }


    public static void legacyRepairRegion(Clipboard clipboard, World world, int x, int y, int z){
        Vector pos = new Vector(x,y,z);
        BaseBlock bb = getBlock(clipboard, pos);
        if (bb.isAir()){
            return;
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
    }
    public static BaseBlock getBlock(Clipboard clipboard, Vector pos){
        BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
        try {
            return GET_BLOCK != null ? (BaseBlock) GET_BLOCK.invoke(bac, pos) : null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }


}
