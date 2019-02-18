package net.countercraft.movecraft.utils;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.registry.WorldData;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FAWEUtils {
    public static boolean faweSaveCraftRepairState(Craft craft, Sign sign, Plugin plugin, String s) {
        HashHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        World world = craft.getW();
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        Vector minPos = new Vector(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        Vector maxPos = new Vector(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        Extent destination = clipboard;
        ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), destination, minPos);
        ExistingBlockMask mask = new ExistingBlockMask(source);
        copy.setSourceMask(mask);
        WorldData data = weWorld.getWorldData();
        try {
            Operations.complete(copy);
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
        File schematicFile = new File(saveDirectory, s + ".schematic");
        try {
            OutputStream output = new FileOutputStream(schematicFile);
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(output);
            writer.write(clipboard, data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
