package net.countercraft.movecraft.compat.v1_13_R2;

import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldedit.world.registry.WorldData;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class IMovecraftRepair extends MovecraftRepair {
    @Override
    public boolean saveCraftRepairState(Craft craft, Sign sign, Plugin plugin, String s) {
        HashHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        World world = craft.getW();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        BlockVector3 minPos = BlockVector3.at(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        Extent destination = clipboard;
        ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), destination, minPos);
        ExistingBlockMask mask = new ExistingBlockMask(source);
        copy.setSourceMask(mask);
        try {
            Operations.completeLegacy(copy);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
            return false;
        }
        File schematicFile = new File(saveDirectory, s + ".schematic");
        try {
            OutputStream output = new FileOutputStream(schematicFile);
            NBTOutputStream outputStream = new NBTOutputStream(output);
            SpongeSchematicWriter writer = new SpongeSchematicWriter(outputStream);
            writer.write(clipboard);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean saveRegionRepairState(Plugin plugin, World world, org.bukkit.util.Vector vector, org.bukkit.util.Vector vector1, String s) {
        return false;
    }

    @Override
    public boolean repairRegion(World world, String s) {
        return false;
    }

    @Override
    public Clipboard loadCraftRepairStateClipboard(Plugin plugin, Sign sign, String s, World world) {
        File dataDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        File file = new File(dataDirectory, s + ".schematic"); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        Clipboard clipboard;
        if (file == null){
            return null;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(file);

        try {
            InputStream input = new FileInputStream(file);
            ClipboardReader reader = format.getReader(input);
            clipboard = reader.read();
            reader.close();
            return clipboard;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Clipboard loadRegionRepairStateClipboard(Plugin plugin, String s, World world) {
        return null;
    }

    @Override
    public HashMap<Material, Double> getMissingBlocks(String s) {
        return null;
    }

    @Override
    public HashSet<Vector> getMissingBlockLocations(String s) {
        return null;
    }

    @Override
    public long getNumDiffBlocks(String s) {
        return 0;
    }

    @Override
    public Vector getDistanceFromSignToLowestPoint(Clipboard clipboard, String s) {
        return null;
    }
}
