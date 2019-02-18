package net.countercraft.movecraft;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public abstract class MovecraftRepair {
    public abstract boolean saveCraftRepairState(Craft craft, Sign sign, Plugin plugin, String repairStateName);
    public abstract boolean saveRegionRepairState(Plugin plugin, World world, org.bukkit.util.Vector minPos, org.bukkit.util.Vector maxPos, String regionName);
    public abstract boolean repairRegion(World world, String regionName);
    public abstract Clipboard loadCraftRepairStateClipboard(Plugin plugin, Sign sign, String repairStateName, World bukkitWorld);
    public abstract Clipboard loadRegionRepairStateClipboard(Plugin plugin, String repairStateName, World bukkitWorld);
    public abstract HashMap<Material, Double> getMissingBlocks(String repairName);
    public abstract LinkedList<Vector> getMissingBlockLocations(String repairName);
    public abstract long getNumDiffBlocks(String repairName);
    public abstract org.bukkit.util.Vector getDistanceFromSignToLowestPoint(Clipboard clipboard, String repairName);
    public abstract org.bukkit.util.Vector getDistanceFromClipboardToWorldOffset(org.bukkit.util.Vector offset, Clipboard clipboard);
    public abstract void setFawePlugin(Plugin fawePlugin);


}
