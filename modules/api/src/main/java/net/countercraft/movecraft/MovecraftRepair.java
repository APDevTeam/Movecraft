package net.countercraft.movecraft;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Material;
import org.bukkit.World;
import net.countercraft.movecraft.utils.*;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.HashMap;



public abstract class MovecraftRepair {
    public abstract boolean saveCraftRepairState(Craft craft, Sign sign);
    public abstract Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign);

    public abstract Clipboard loadRegionRepairStateClipboard(String repairStateName, World bukkitWorld);
    public abstract HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName);

    public abstract ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String repairName);

    public abstract long getNumDiffBlocks(String repairName);
}
