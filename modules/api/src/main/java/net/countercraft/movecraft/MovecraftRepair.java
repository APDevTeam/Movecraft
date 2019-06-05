package net.countercraft.movecraft;


import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.Pair;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.HashMap;


public abstract class MovecraftRepair {
    public abstract boolean saveCraftRepairState(Craft craft, Sign sign, Plugin plugin, String repairStateName);
    public abstract boolean saveRegionRepairState(Plugin plugin, World world, ProtectedRegion region);
    public abstract Clipboard loadCraftRepairStateClipboard(Plugin plugin, Craft craft, Sign sign, String repairStateName, World bukkitWorld);
    public abstract Clipboard loadRegionRepairStateClipboard(Plugin plugin, String repairStateName, World bukkitWorld);
    public abstract HashMap<Material, Double> getMissingBlocks(String repairName);
    public abstract ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String repairName);
    public abstract long getNumDiffBlocks(String repairName);
    public abstract org.bukkit.util.Vector getDistanceFromSignToLowestPoint(Clipboard clipboard);
    public abstract org.bukkit.util.Vector getDistance(String repairName);



}
