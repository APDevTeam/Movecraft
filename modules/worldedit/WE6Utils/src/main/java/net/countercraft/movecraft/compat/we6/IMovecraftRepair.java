package net.countercraft.movecraft.compat.we6;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class IMovecraftRepair extends MovecraftRepair {
    private HashMap<String, ArrayDeque<Pair<Vector, Vector>>> locMissingBlocksMap = new HashMap<>();
    private HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    private HashMap<String, HashMap<Material, Double>> missingBlocksMap = new HashMap<>();
    private final HashMap<String, Vector> distanceMap = new HashMap<>();

    @Override
    public boolean saveCraftRepairState (Craft craft, Sign sign, Plugin plugin, String s) {
        HashHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        World world = craft.getW();
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        com.sk89q.worldedit.Vector origin = new com.sk89q.worldedit.Vector(sign.getX(),sign.getY(),sign.getZ());
        com.sk89q.worldedit.Vector minPos = new com.sk89q.worldedit.Vector(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        com.sk89q.worldedit.Vector maxPos = new com.sk89q.worldedit.Vector(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion cRegion = new CuboidRegion(minPos, maxPos);
        File repairStateFile = new File(saveDirectory, s + ".schematic");
        Set<BaseBlock> blockSet = baseBlocksFromCraft(craft);
        try {

            BlockArrayClipboard clipboard = new BlockArrayClipboard(cRegion);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
            Extent destination = clipboard;
            ForwardExtentCopy copy = new ForwardExtentCopy(source, cRegion, clipboard.getOrigin(), destination, clipboard.getOrigin());
            BlockMask mask = new BlockMask(source, blockSet);
            copy.setSourceMask(mask);
            Operations.completeLegacy(copy);
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(repairStateFile, false));
            writer.write(clipboard, worldData);
            writer.close();
            return true;

        } catch (MaxChangedBlocksException | IOException e) {
            e.printStackTrace();
            return false;
        }


    }

    @Override
    public boolean saveRegionRepairState(Plugin plugin, World world, ProtectedRegion region) {

        File saveDirectory = new File(plugin.getDataFolder(), "RegionRepairStates");
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        com.sk89q.worldedit.Vector weMinPos = region.getMinimumPoint();
        com.sk89q.worldedit.Vector weMaxPos = region.getMaximumPoint();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        Set<BaseBlock> baseBlockSet = new HashSet<>();
        Region weRegion = null;
        if (region instanceof ProtectedCuboidRegion){
            weRegion = new CuboidRegion(weMinPos, weMaxPos);
        } else if (region instanceof ProtectedPolygonalRegion){
            ProtectedPolygonalRegion polyReg = (ProtectedPolygonalRegion) region;
            weRegion = new Polygonal2DRegion(weWorld,polyReg.getPoints(),polyReg.getMinimumPoint().getBlockY(),polyReg.getMaximumPoint().getBlockY());
        }



        File repairStateFile = new File(saveDirectory, region.getId().replaceAll("´\\s+", "_") + ".schematic");
        for (int x = weMinPos.getBlockX(); x <= weMaxPos.getBlockX(); x++){
            for (int y = weMinPos.getBlockY(); y <= weMaxPos.getBlockY(); y++){
                for (int z = weMinPos.getBlockZ(); z <= weMaxPos.getBlockZ(); z++){
                    Block block = world.getBlockAt(x,y,z);
                    if (block.getType().equals(Material.AIR)){
                        continue;
                    }
                    if (Settings.AssaultDestroyableBlocks.contains(block.getType())){
                        baseBlockSet.add(new BaseBlock(block.getTypeId(), block.getData()));
                    }
                }
            }
        }
        try {

            BlockArrayClipboard clipboard = new BlockArrayClipboard(weRegion);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
            Extent destination = clipboard;
            ForwardExtentCopy copy = new ForwardExtentCopy(source, weRegion, clipboard.getOrigin(), destination, weMinPos);
            BlockMask mask = new BlockMask(source, baseBlockSet);
            copy.setSourceMask(mask);
            Operations.completeLegacy(copy);
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(repairStateFile, false));
            writer.write(clipboard, worldData);
            writer.close();
            return true;

        } catch (MaxChangedBlocksException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    @Override
    public Clipboard loadCraftRepairStateClipboard(Plugin plugin,Craft craft, Sign sign, String repairStateFile, World world) {
        File dataDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        File file = new File(dataDirectory, repairStateFile + ".schematic"); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        Clipboard clipboard;
        HashHitBox hitBox = craft.getHitBox();
        try {
            clipboard = ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (clipboard != null){
            long numDiffBlocks = 0;
            HashMap<Material, Double> missingBlocks = new HashMap<>();
            ArrayDeque<Pair<Vector, Vector>> locMissingBlocks = new ArrayDeque<>();
            Vector distance = new Vector(sign.getX() - hitBox.getMinX(), sign.getY() - hitBox.getMinY(),sign.getZ() - hitBox.getMinZ());
            if (distanceMap.containsKey(repairStateFile)) {
                distanceMap.replace(repairStateFile,distance);
            } else {
                distanceMap.put(repairStateFile,distance);
            }
            for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++){
                for (int y = clipboard.getMinimumPoint().getBlockY(); y <= clipboard.getMaximumPoint().getBlockY(); y++){
                    for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint().getBlockZ(); z++){
                        com.sk89q.worldedit.Vector position = new com.sk89q.worldedit.Vector(x,y,z);
                        Location bukkitLoc = new Location(sign.getWorld(), x + distance.getBlockX(), y + distance.getBlockY(), z + distance.getBlockZ());
                        BaseBlock block = clipboard.getBlock(position);
                        Block bukkitBlock = sign.getWorld().getBlockAt(bukkitLoc);
                        boolean isImportant = true;
                        if (block.getType() == 0){
                            isImportant = false;
                        }

                        if (isImportant && bukkitBlock.getTypeId() != block.getType()) {
                            int itemToConsume = block.getType();
                            double qtyToConsume = 1.0;
                            numDiffBlocks++;
                            //some blocks aren't represented by items with the same number as the block
                            if (itemToConsume == 63 || itemToConsume == 68) // signs
                                itemToConsume = 323;
                            if (itemToConsume == 93 || itemToConsume == 94) // repeaters
                                itemToConsume = 356;
                            if (itemToConsume == 149 || itemToConsume == 150) // comparators
                                itemToConsume = 404;
                            if (itemToConsume == 55) // redstone
                                itemToConsume = 331;
                            if (itemToConsume == 118) // cauldron
                                itemToConsume = 380;
                            if (itemToConsume == 124) // lit redstone lamp
                                itemToConsume = 123;
                            if (itemToConsume == 75) // lit redstone torch
                                itemToConsume = 76;
                            if (itemToConsume == 8 || itemToConsume == 9) { // don't require water to be in the chest
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                            }
                            if (itemToConsume == 10 || itemToConsume == 11) { // don't require lava either, yeah you could exploit this for free lava, so make sure you set a price per block
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                            }
                            if (itemToConsume == 26) { //beds
                                itemToConsume = 355;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 64) { //doors
                                itemToConsume = 324;   //since doors and beds encompass two blocks, require only 0.5 block for each of the two blocks
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 71){
                                itemToConsume = 330;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 193){
                                itemToConsume = 427;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 194){
                                itemToConsume = 428;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 195){
                                itemToConsume = 429;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 196){
                                itemToConsume = 430;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 197) {
                                itemToConsume = 431;
                                qtyToConsume = 0.5;
                            }
                            if (itemToConsume == 23){
                                Tag t = block.getNbtData().getValue().get("Items");
                                ListTag lt = null;
                                if (t instanceof ListTag){
                                    lt = (ListTag) t;
                                }
                                int numTNT = 0;
                                int numFireCharges = 0;
                                int numWaterBuckets = 0;
                                if (lt != null) {
                                    for (Tag entryTag : lt.getValue()) {
                                        if (entryTag instanceof CompoundTag){
                                            CompoundTag cTag = (CompoundTag) entryTag;
                                            if (cTag.toString().contains("minecraft:tnt")){
                                                numTNT += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:fire_charge")){
                                                numFireCharges += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:water_bucket")){
                                                numWaterBuckets += cTag.getByte("Count");
                                            }
                                        }
                                    }
                                }


                                if (numTNT > 0){
                                    if (!missingBlocks.containsKey(Material.TNT)){
                                        missingBlocks.put(Material.TNT, (double) numTNT);
                                    } else {
                                        Double num = missingBlocks.get(Material.TNT);
                                        num += numTNT;
                                        missingBlocks.put(Material.TNT, num);
                                    }
                                }
                                if (numFireCharges > 0){
                                    if (!missingBlocks.containsKey(Material.FIREBALL)){
                                        missingBlocks.put(Material.FIREBALL, (double) numFireCharges);
                                    } else {
                                        Double num = missingBlocks.get(Material.FIREBALL);
                                        num += numFireCharges;
                                        missingBlocks.put(Material.FIREBALL, num);

                                    }
                                }
                                if (numWaterBuckets > 0){
                                    if (!missingBlocks.containsKey(Material.WATER_BUCKET)){
                                        missingBlocks.put(Material.WATER_BUCKET, (double) numWaterBuckets);
                                    } else {
                                        Double num = missingBlocks.get(Material.WATER_BUCKET);
                                        num += numWaterBuckets;
                                        missingBlocks.put(Material.WATER_BUCKET, num);
                                    }
                                }
                            }
                            if (itemToConsume == 43) { // for double slabs, require 2 slabs
                                itemToConsume = 44;
                                qtyToConsume = 2;
                            }
                            if (itemToConsume == 125) { // for double wood slabs, require 2 wood slabs
                                itemToConsume = 126;
                                qtyToConsume = 2;
                            }
                            if (itemToConsume == 181) { // for double red sandstone slabs, require 2 red sandstone slabs
                                itemToConsume = 182;
                                qtyToConsume = 2;
                            }
                            if (itemToConsume != 0){
                                if (!missingBlocks.containsKey(Material.getMaterial(itemToConsume))){
                                    missingBlocks.put(Material.getMaterial(itemToConsume), qtyToConsume);
                                } else {
                                    Double num = missingBlocks.get(Material.getMaterial(itemToConsume));
                                    num += qtyToConsume;
                                    missingBlocks.put(Material.getMaterial(itemToConsume), num);
                                }
                                locMissingBlocks.add(new Pair<>(new org.bukkit.util.Vector(x,y,z),null));
                            }
                        }
                        if (bukkitBlock.getType() == Material.DISPENSER && block.getType() == 23){
                            boolean needReplace = false;
                            Tag t = block.getNbtData().getValue().get("Items");
                            ListTag lt = null;
                            if (t instanceof ListTag){
                                lt = (ListTag) t;
                            }
                            int numTNT = 0;
                            int numFireCharges = 0;
                            int numWaterBuckets = 0;
                            if (lt != null) {
                                for (Tag entryTag : lt.getValue()) {
                                    if (entryTag instanceof CompoundTag){
                                        CompoundTag cTag = (CompoundTag) entryTag;
                                        if (cTag.toString().contains("minecraft:tnt")){
                                            numTNT += cTag.getByte("Count");
                                        }
                                        if (cTag.toString().contains("minecraft:fire_charge")){
                                            numFireCharges += cTag.getByte("Count");
                                        }
                                        if (cTag.toString().contains("minecraft:water_bucket")){
                                            numWaterBuckets += cTag.getByte("Count");
                                        }
                                    }
                                }
                            }
                            Dispenser bukkitDispenser = (Dispenser) bukkitBlock.getState();
                            //Bukkit.getLogger().info(String.format("TNT: %d, Fireballs: %d, Water buckets: %d", numTNT, numFireCharges, numWaterBuckets));
                            for (ItemStack iStack : bukkitDispenser.getInventory().getContents()){
                                if (iStack != null) {
                                    if (iStack.getType() == Material.TNT) {
                                        numTNT -= iStack.getAmount();
                                    }
                                    if (iStack.getType() == Material.FIREBALL) {
                                        numFireCharges -= iStack.getAmount();
                                    }
                                    if (iStack.getType() == Material.WATER_BUCKET) {
                                        numWaterBuckets -= iStack.getAmount();
                                    }
                                }
                            }
                            //Bukkit.getLogger().info(String.format("TNT: %d, Fireballs: %d, Water buckets: %d", numTNT, numFireCharges, numWaterBuckets));
                            if (numTNT > 0){
                                if (!missingBlocks.containsKey(Material.TNT)){
                                    missingBlocks.put(Material.TNT, (double) numTNT);
                                } else {
                                    Double num = missingBlocks.get(Material.TNT);
                                    num += numTNT;
                                    missingBlocks.put(Material.TNT, num);
                                }
                                needReplace = true;
                            }
                            if (numFireCharges > 0){
                                if (!missingBlocks.containsKey(Material.FIREBALL)){
                                    missingBlocks.put(Material.FIREBALL, (double) numFireCharges);
                                } else {
                                    Double num = missingBlocks.get(Material.FIREBALL);
                                    num += numFireCharges;
                                    missingBlocks.put(Material.FIREBALL, num);

                                }
                                needReplace = true;
                            }
                            if (numWaterBuckets > 0){
                                if (!missingBlocks.containsKey(Material.WATER_BUCKET)){
                                    missingBlocks.put(Material.WATER_BUCKET, (double) numWaterBuckets);
                                } else {
                                    Double num = missingBlocks.get(Material.WATER_BUCKET);
                                    num += numWaterBuckets;
                                    missingBlocks.put(Material.WATER_BUCKET, num);
                                }
                                needReplace = true;
                            }
                            if (needReplace){
                                numDiffBlocks++;
                                locMissingBlocks.addLast(new Pair<>(new org.bukkit.util.Vector(x,y,z),null));
                            }
                        }
                    }
                }
            }
            locMissingBlocksMap.put(repairStateFile, locMissingBlocks);
            missingBlocksMap.put(repairStateFile, missingBlocks);
            numDiffBlocksMap.put(repairStateFile, numDiffBlocks);
        }
        return clipboard;
    }

    @Override
    public Clipboard loadRegionRepairStateClipboard(Plugin plugin, String s, World world) {
        File dataDirectory = new File(plugin.getDataFolder(), "RegionRepairStates");
        File file = new File(dataDirectory, s + ".schematic"); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        try {
            return ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public HashMap<Material, Double> getMissingBlocks(String repairName) {
        return missingBlocksMap.get(repairName);
    }

    @Override
    public ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String repairName) {
        return locMissingBlocksMap.get(repairName);
    }

    @Override
    public long getNumDiffBlocks(String s) {
        return numDiffBlocksMap.get(s);
    }


    @Override
    public Vector getDistance(String repairName) {
        return distanceMap.get(repairName);
    }

    @Override
    public Vector getOffset(String repairName) {
        return null;
    }


    private Set<BaseBlock> baseBlocksFromCraft(Craft craft){
        HashSet<BaseBlock> returnSet = new HashSet<>();
        HashHitBox hitBox = craft.getHitBox();
        World w = craft.getW();
        for (MovecraftLocation location : hitBox){
            Integer id = w.getBlockTypeIdAt(location.getX(), location.getY(), location.getZ());
            Byte data = w.getBlockAt(location.getX(), location.getY(), location.getZ()).getData();
            returnSet.add(new BaseBlock(id, data));
        }
        Bukkit.getLogger().info(returnSet.toString());
        return returnSet;
    }

    private Set<BaseBlock> baseBlocksFromAssaultRegion(com.sk89q.worldedit.Vector minPos, com.sk89q.worldedit.Vector maxPos, World world){
        HashSet<BaseBlock> returnSet = new HashSet<>();
        for (int x = minPos.getBlockX(); x <= maxPos.getBlockX(); x++){
            for (int y = minPos.getBlockY(); y <= maxPos.getBlockY(); y++){
                for (int z = minPos.getBlockZ(); z <= maxPos.getBlockZ(); z++){
                    int id = world.getBlockTypeIdAt(x,y,z);
                    byte data = world.getBlockAt(x,y,z).getData();
                    //exclude air and blocks that are not destroyable during an assault
                    if (!Settings.AssaultDestroyableBlocks.contains(id) || id == 0){
                        continue;
                    }
                    returnSet.add(new BaseBlock(id, data));
                }
            }
        }

        return returnSet;
    }
}
