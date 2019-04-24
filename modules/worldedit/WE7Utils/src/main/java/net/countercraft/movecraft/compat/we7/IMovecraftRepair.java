package net.countercraft.movecraft.compat.we7;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.type.Slab;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class IMovecraftRepair extends MovecraftRepair {
    private final HashMap<String, ArrayDeque<Vector>> locMissingBlocksMap = new HashMap<>();
    private final HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    private final HashMap<String, HashMap<Material, Double>> missingBlocksMap = new HashMap<>();
    private final HashMap<String, Vector> distanceMap = new HashMap<>();
    @Override
    public boolean saveCraftRepairState(Craft craft, Sign sign, Plugin plugin, String s) {
        HashHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        World world = craft.getW();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        BlockVector3 origin = BlockVector3.at(sign.getX(),sign.getY(),sign.getZ());
        BlockVector3 minPos = BlockVector3.at(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);
        Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        Extent destination = clipboard;
        ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), destination, clipboard.getOrigin());
        ExistingBlockMask mask = new ExistingBlockMask(source);
        copy.setSourceMask(mask);
        try {
            Operations.complete(copy);
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
        File schematicFile = new File(saveDirectory, s + ".schematic");
        try {
            OutputStream output = new FileOutputStream(schematicFile);
            ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(output);
            writer.write(clipboard);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean saveRegionRepairState(Plugin plugin, World world, ProtectedRegion region) {
        File saveDirectory = new File(plugin.getDataFolder(), "RegionRepairStates");
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockVector3 weMinPos = region.getMinimumPoint();
        BlockVector3 weMaxPos = region.getMaximumPoint();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        Set<BlockType> blockTypeSet = new HashSet<>();
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
                        blockTypeSet.add(new BlockType("minecraft:" + block.getType().name().toLowerCase()));
                    }
                }
            }
        }
        try {

            BlockArrayClipboard clipboard = new BlockArrayClipboard(weRegion);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
            Extent destination = clipboard;
            ForwardExtentCopy copy = new ForwardExtentCopy(source, weRegion, destination, weMinPos);
            BlockTypeMask mask = new BlockTypeMask(source, blockTypeSet);
            copy.setSourceMask(mask);
            Operations.completeLegacy(copy);
            ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(repairStateFile, false));
            writer.write(clipboard);
            writer.close();
            return true;

        } catch (MaxChangedBlocksException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean repairRegion(World world, String s) {
        return false;
    }

    @Override
    public Clipboard loadCraftRepairStateClipboard(Plugin plugin,Craft craft, Sign sign, String s, World world) {
        File dataDirectory = new File(plugin.getDataFolder(), "CraftRepairStates");
        File file = new File(dataDirectory, s + ".schematic"); // The schematic file
        Clipboard clipboard = null;
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        HashHitBox hitBox = craft.getHitBox();
        try {
            InputStream input = new FileInputStream(file);
            ClipboardReader reader = format.getReader(input);
            clipboard = reader.read();
            reader.close();


        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        if (clipboard != null){
            long numDiffBlocks = 0;
            HashMap<Material, Double> missingBlocks = new HashMap<>();
            ArrayDeque<Vector> locMissingBlocks = new ArrayDeque<>();
            Vector distance = new Vector(sign.getX() - hitBox.getMinX(), sign.getY() - hitBox.getMinY(),sign.getZ() - hitBox.getMinZ());
            if (distanceMap.containsKey(s)) {
                distanceMap.replace(s,distance);
            } else {
                distanceMap.put(s,distance);
            }
            Bukkit.broadcastMessage(distance.toString());


            for (int y = clipboard.getMinimumPoint().getBlockY(); y <= clipboard.getMaximumPoint().getBlockY(); y++) {
                for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint().getBlockZ(); z++) {
                    for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++) {
                        BlockVector3 position = BlockVector3.at(x,y,z);
                        int cx = x - distance.getBlockX();
                        int cy = y - distance.getBlockY();
                        int cz = z - distance.getBlockZ();
                        BaseBlock block = clipboard.getFullBlock(position);
                        Material type = BukkitAdapter.adapt(block.getBlockType());
                        Location loc = new Location(sign.getWorld(),x,y,z);
                        Block bukkitBlock = sign.getWorld().getBlockAt(loc);
                        boolean isImportant = true;

                        if (type.equals(Material.AIR) || type.equals(Material.CAVE_AIR) || type.equals(Material.VOID_AIR)){
                            isImportant = false;
                        }
                        boolean blockMissing = isImportant && type != bukkitBlock.getType();
                        //Check if single slabs are at locations where double slabs should be located and vice versa
                        if (type.name().endsWith("_SLAB")){
                            plugin.getLogger().info(type.name());
                            for (Property property : block.getStates().keySet()){
                                if (!(property instanceof EnumProperty)){
                                    continue;
                                }
                                @SuppressWarnings("unchecked")
                                String prop = (String) block.getState(property);
                                if (bukkitBlock.getBlockData() instanceof Slab){
                                    Slab slab = (Slab) bukkitBlock.getBlockData();
                                    blockMissing = slab.getType() != Slab.Type.valueOf(prop.toUpperCase());
                                }
                            }
                        }

                        if (blockMissing){
                            Material typeToConsume = type;
                            double qtyToConsume = 1.0;
                            if (type.equals(Material.WATER)||type.equals(Material.LAVA)){
                                qtyToConsume = 0;
                            }
                            if (type.name().endsWith("_SLAB")){
                                plugin.getLogger().info(type.name());
                                for (Property property : block.getStates().keySet()){
                                    if (!(property instanceof EnumProperty)){
                                        continue;
                                    }
                                    @SuppressWarnings("unchecked")
                                    String prop = (String) block.getState(property);
                                    if (prop.equals("double")){
                                        qtyToConsume = 2.0;
                                    }
                                }
                            }
                            if (type.equals(Material.WALL_SIGN)){
                                typeToConsume = Material.SIGN;
                            }
                            if (type.equals(Material.REDSTONE_WALL_TORCH)){
                                typeToConsume = Material.REDSTONE_TORCH;
                            }
                            if (type.equals(Material.WALL_TORCH))
                                typeToConsume = Material.TORCH;
                            if (type.name().endsWith("_DOOR") || type.name().endsWith("_BED"))
                                qtyToConsume = 0.5;
                            if (type.equals(Material.REDSTONE_WIRE))
                                typeToConsume = Material.REDSTONE;
                            if (type.equals(Material.DISPENSER)){
                                int numTNT = 0;
                                int numFirecharge = 0;
                                int numWaterBucket = 0;
                                ListTag listTag = block.getNbtData().getListTag("Items");
                                if (listTag != null) {
                                    for (Tag entry : listTag.getValue()) {
                                        //To avoid ClassCastExceptions, continue if tag is not a CompoundTag
                                        if (!(entry instanceof CompoundTag)) {
                                            continue;
                                        }
                                        CompoundTag cTag = (CompoundTag) entry;
                                        if (cTag.getString("id").equals("minecraft:tnt")) {
                                            numTNT += cTag.getByte("Count");
                                        }
                                        if (cTag.getString("id").equals("minecraft:fire_charge")) {
                                            numFirecharge += cTag.getByte("Count");
                                        }
                                        if (cTag.getString("id").equals("minecraft:water_bucket")) {
                                            numWaterBucket += cTag.getByte("Count");
                                        }
                                    }
                                }
                                if (numTNT > 0){
                                    if (missingBlocks.containsKey(Material.TNT)){
                                        double count = missingBlocks.get(Material.TNT);
                                        count += numTNT;
                                        missingBlocks.put(Material.TNT,count);
                                    } else {
                                        missingBlocks.put(Material.TNT, (double) numTNT);
                                    }
                                }
                                if (numFirecharge > 0){
                                    if (missingBlocks.containsKey(Material.FIRE_CHARGE)){
                                        double count = missingBlocks.get(Material.FIRE_CHARGE);
                                        count += numFirecharge;
                                        missingBlocks.put(Material.FIRE_CHARGE,count);
                                    } else {
                                        missingBlocks.put(Material.FIRE_CHARGE, (double) numFirecharge);
                                    }
                                }
                                if (numWaterBucket > 0){
                                    if (missingBlocks.containsKey(Material.WATER_BUCKET)){
                                        double count = missingBlocks.get(Material.WATER_BUCKET);
                                        count += numWaterBucket;
                                        missingBlocks.put(Material.WATER_BUCKET,count);
                                    } else {
                                        missingBlocks.put(Material.WATER_BUCKET, (double) numWaterBucket);
                                    }
                                }
                            }
                            locMissingBlocks.addLast(new Vector(cx,cy,cz));
                            numDiffBlocks++;
                            if (missingBlocks.containsKey(typeToConsume)){
                                double count = missingBlocks.get(typeToConsume);
                                count += qtyToConsume;
                                missingBlocks.put(typeToConsume,count);
                            } else {
                                missingBlocks.put(typeToConsume,qtyToConsume);
                            }
                        }
                        if (bukkitBlock.getState() instanceof Dispenser){
                            if (!type.equals(Material.DISPENSER))
                                continue;
                            Dispenser dispenser = (Dispenser) bukkitBlock.getState();
                            int numTNT = 0;
                            int numFireCharge = 0;
                            int numWaterBucket = 0;
                            ListTag listTag = block.getNbtData().getListTag("Items");
                            if (listTag != null) {
                                for (Tag entry : listTag.getValue()) {
                                    //To avoid ClassCastExceptions, continue if tag is not a CompoundTag
                                    if (!(entry instanceof CompoundTag)) {
                                        continue;
                                    }
                                    CompoundTag cTag = (CompoundTag) entry;
                                    if (cTag.getString("id").equals("minecraft:tnt")) {
                                        numTNT += (int) cTag.getByte("Count");
                                    }
                                    if (cTag.getString("id").equals("minecraft:fire_charge")) {
                                        numFireCharge += (int) cTag.getByte("Count");
                                    }
                                    if (cTag.getString("id").equals("minecraft:water_bucket")) {
                                        numWaterBucket += (int) cTag.getByte("Count");
                                    }
                                }
                            }
                            ItemStack[] contents = dispenser.getInventory().getContents();
                            for (ItemStack iStack : contents){
                                if (iStack == null){
                                    continue;
                                }
                                if (iStack.getType().equals(Material.TNT)){
                                    numTNT -= iStack.getAmount();
                                }
                                if (iStack.getType().equals(Material.WATER_BUCKET)){
                                    numWaterBucket -= iStack.getAmount();
                                }
                                if (iStack.getType().equals(Material.FIRE_CHARGE)){
                                    numFireCharge -= iStack.getAmount();
                                }
                            }
                            boolean needsReplace = false;
                            if (numTNT > 0){
                                if (missingBlocks.containsKey(Material.TNT)){
                                    double count = missingBlocks.get(Material.TNT);
                                    count += numTNT;
                                    missingBlocks.put(Material.TNT,count);
                                } else {
                                    missingBlocks.put(Material.TNT, (double) numTNT);
                                }
                                needsReplace = true;
                            }
                            if (numFireCharge > 0){
                                if (missingBlocks.containsKey(Material.FIRE_CHARGE)){
                                    double count = missingBlocks.get(Material.FIRE_CHARGE);
                                    count += numFireCharge;
                                    missingBlocks.put(Material.FIRE_CHARGE,count);
                                } else {
                                    missingBlocks.put(Material.FIRE_CHARGE, (double) numFireCharge);
                                }
                                needsReplace = true;
                            }
                            if (numWaterBucket > 0){
                                if (missingBlocks.containsKey(Material.WATER_BUCKET)){
                                    double count = missingBlocks.get(Material.WATER_BUCKET);
                                    count += numWaterBucket;
                                    missingBlocks.put(Material.WATER_BUCKET,count);
                                } else {
                                    missingBlocks.put(Material.WATER_BUCKET, (double) numWaterBucket);
                                }
                                needsReplace = true;
                            }
                            if (needsReplace){
                                locMissingBlocks.addLast(new Vector(cx,cy,cz));
                                numDiffBlocks++;
                            }
                        }

                    }
                }
            }
            locMissingBlocksMap.put(s, locMissingBlocks);
            missingBlocksMap.put(s, missingBlocks);
            numDiffBlocksMap.put(s, numDiffBlocks);
        }
        return clipboard;
    }

    @Override
    public Clipboard loadRegionRepairStateClipboard(Plugin plugin, String s, World world) {
        Clipboard clipboard;
        File dataDirectory = new File(plugin.getDataFolder(), "RegionRepairStates");
        File file = new File(dataDirectory, s + ".schematic"); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        try {
            InputStream input = new FileInputStream(file);
            ClipboardReader reader = format.getReader(input);
            clipboard = reader.read();
            reader.close();
        } catch (IOException e) {
            clipboard = null;
            e.printStackTrace();
        }
        return clipboard;
    }

    @Override
    public HashMap<Material, Double> getMissingBlocks(String s) {
        return missingBlocksMap.get(s);
    }

    @Override
    public ArrayDeque<Vector> getMissingBlockLocations(String s) {

        return locMissingBlocksMap.get(s);
    }

    @Override
    public long getNumDiffBlocks(String s) {
        return numDiffBlocksMap.get(s);
    }

    @Override
    public Vector getDistanceFromSignToLowestPoint(Clipboard clipboard) {
        return new Vector(clipboard.getOrigin().getBlockX() - clipboard.getMinimumPoint().getBlockX(),clipboard.getOrigin().getBlockY() - clipboard.getMinimumPoint().getBlockY(),clipboard.getOrigin().getBlockZ() - clipboard.getMinimumPoint().getBlockZ());
        /*for (int x = clipboard.getMinimumPoint().getBlockX(); x <= clipboard.getMaximumPoint().getBlockX(); x++) {
            for (int y = clipboard.getMinimumPoint().getBlockY(); y <= clipboard.getMaximumPoint().getBlockY(); y++) {
                for (int z = clipboard.getMinimumPoint().getBlockZ(); z <= clipboard.getMaximumPoint().getBlockZ(); z++) {
                    BlockVector3 pos = BlockVector3.at(x,y,z);
                    BaseBlock block = clipboard.getFullBlock(pos);
                    if (block.getBlockType().getId().equals("minecraft:sign")||block.getBlockType().getId().equals("minecraft:wall_sign")) {
                        Logger log = Bukkit.getLogger();
                        String firstLine = block.getNbtData().getString("Text1");
                        firstLine = firstLine.substring(2);
                        if (firstLine.startsWith("extra")) {
                            firstLine = firstLine.substring(17);
                            firstLine = firstLine.replace("\"}],\"text\":\"\"}", "");
                        }
                        String secondLine = block.getNbtData().getString("Text2");
                        secondLine = secondLine.substring(2);
                        if (secondLine.startsWith("extra")) {
                            secondLine = secondLine.substring(17);
                            secondLine = secondLine.replace("\"}],\"text\":\"\"}", "");
                        }
                        if (firstLine.equalsIgnoreCase("Repair:") && s.endsWith(secondLine)) {
                            returnDistance = new Vector(x - clipboard.getMinimumPoint().getBlockX(), y - clipboard.getMinimumPoint().getBlockY(), z - clipboard.getMinimumPoint().getBlockZ());
                            break;
                        }
                    }
                }
                if (returnDistance != null) break;
            }
            if (returnDistance != null) break;
        }
        return returnDistance;*/
    }


    @Override
    public Vector getDistance(String repairName) {
        return distanceMap.get(repairName);
    }
}
