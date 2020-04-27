package net.countercraft.movecraft.compat.we6;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
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
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class IMovecraftRepair extends MovecraftRepair {
    private final Plugin plugin;
    private HashMap<String, ArrayDeque<Pair<Vector,Vector>>> locMissingBlocksMap = new HashMap<>();
    private HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    private HashMap<String, HashMap<Pair<Material, Byte>, Double>> missingBlocksMap = new HashMap<>();

    public IMovecraftRepair(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean saveCraftRepairState(Craft craft, Sign sign) {
        BitmapHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "RepairStates");
        World world = craft.getW();
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        com.sk89q.worldedit.Vector origin = new com.sk89q.worldedit.Vector(sign.getX(),sign.getY(),sign.getZ());
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        com.sk89q.worldedit.Vector minPos = new com.sk89q.worldedit.Vector(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        com.sk89q.worldedit.Vector maxPos = new com.sk89q.worldedit.Vector(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion cRegion = new CuboidRegion(minPos, maxPos);
        File playerDirectory = new File(saveDirectory,craft.getNotificationPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            playerDirectory.mkdirs();
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += ".schematic";
        File repairStateFile = new File(playerDirectory, repairName);
        Set<BaseBlock> blockSet = baseBlocksFromCraft(craft);
        final HitBox outsideLocs = CollectionUtils.filter(solidBlockLocs(craft.getW(), cRegion), hitBox);
        try {

            BlockArrayClipboard clipboard = new BlockArrayClipboard(cRegion);
            clipboard.setOrigin(origin);
            Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, cRegion, clipboard.getOrigin(), clipboard, clipboard.getOrigin());
            BlockMask mask = new BlockMask(source, blockSet);
            copy.setSourceMask(mask);
            Operations.completeLegacy(copy);
            ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(repairStateFile, false));
            for (MovecraftLocation ml : outsideLocs){
                clipboard.setBlock(new com.sk89q.worldedit.Vector(ml.getX(), ml.getY(), ml.getZ()), new BaseBlock(0));
            }
            writer.write(clipboard, worldData);
            writer.close();
            return true;

        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }


    }

    @Override
    public boolean saveRegionRepairState(World world, ProtectedRegion region) {

        File saveDirectory = new File(plugin.getDataFolder(), "AssaultSnapshots");
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        com.sk89q.worldedit.Vector weMinPos = region.getMinimumPoint();
        com.sk89q.worldedit.Vector weMaxPos = region.getMaximumPoint();
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        Set<BaseBlock> baseBlockSet = new HashSet<>();
        Region weRegion = null;
        if (region instanceof ProtectedCuboidRegion) {
            weRegion = new CuboidRegion(weMinPos, weMaxPos);
        } else if (region instanceof ProtectedPolygonalRegion) {
            ProtectedPolygonalRegion polyReg = (ProtectedPolygonalRegion) region;
            weRegion = new Polygonal2DRegion(weWorld, polyReg.getPoints(), polyReg.getMinimumPoint().getBlockY(), polyReg.getMaximumPoint().getBlockY());
        }


        File repairStateFile = new File(saveDirectory, region.getId().replaceAll("´\\s+", "_") + ".schematic");
        for (int x = weMinPos.getBlockX(); x <= weMaxPos.getBlockX(); x++) {
            for (int y = weMinPos.getBlockY(); y <= weMaxPos.getBlockY(); y++) {
                for (int z = weMinPos.getBlockZ(); z <= weMaxPos.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().equals(Material.AIR)) {
                        continue;
                    }
                    if (Settings.AssaultDestroyableBlocks.contains(block.getTypeId())) {
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
    public Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign) {
        File dataDirectory = new File(plugin.getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory,craft.getNotificationPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            return null;
        }
        String repairName = ChatColor.stripColor(sign.getLine(1));
        repairName += ".schematic";
        File file = new File(playerDirectory, repairName); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(sign.getWorld());
        WorldData worldData = weWorld.getWorldData();
        Clipboard clipboard;
        try {
            clipboard = ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (clipboard == null) {
            return null;
        }
        long numDiffBlocks = 0;
        HashMap<Pair<Material, Byte>, Double> missingBlocks = new HashMap<>();
        ArrayDeque<Pair<Vector,Vector>> locMissingBlocks = new ArrayDeque<>();
        Vector minPos = MathUtils.we6vectorToBukkitVector(clipboard.getMinimumPoint());
        Vector distance = MathUtils.we6vectorToBukkitVector(clipboard.getOrigin().subtract(clipboard.getMinimumPoint()));
        Vector size = MathUtils.we6vectorToBukkitVector(clipboard.getDimensions());
        Vector offset = new Vector(sign.getX() - distance.getBlockX(), sign.getY() - distance.getBlockY(), sign.getZ() - distance.getBlockZ());
        for (int x = 0; x <= size.getBlockX(); x++) {
            for (int y = 0; y <= size.getBlockY(); y++) {
                for (int z = 0; z <= size.getBlockZ(); z++) {
                    com.sk89q.worldedit.Vector position = new com.sk89q.worldedit.Vector(minPos.getBlockX() + x, minPos.getBlockY() + y, minPos.getBlockZ() + z);
                    Location bukkitLoc = new Location(sign.getWorld(), offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z);
                    BaseBlock block = clipboard.getBlock(position);
                    Block bukkitBlock = sign.getWorld().getBlockAt(bukkitLoc);
                    if (block.getType() != 0 && bukkitBlock.getTypeId() != block.getType()) {
                        int itemToConsume = block.getType();
                        byte dataToConsume = (byte) block.getData();
                        double qtyToConsume = 1.0;
                        numDiffBlocks++;
                        //some blocks aren't represented by items with the same number as the block
                        switch (itemToConsume) {
                            case 61:
                                //Count fuel in furnaces
                                ListTag list = block.getNbtData().getListTag("Items");
                                if (list != null){
                                    for (Tag t : list.getValue()){
                                        if (!(t instanceof CompoundTag)){
                                            continue;
                                        }
                                        CompoundTag ct = (CompoundTag) t;
                                        if (ct.getByte("Slot") == 2){//Ignore the result slot
                                            continue;
                                        }
                                        String id = ct.getString("id");
                                        Pair<Material, Byte> content;
                                        if (id.equals("minecraft:coal")){
                                            byte data = (byte) ct.getShort("Damage");
                                            content = new Pair<>(Material.COAL, data);
                                            if (!missingBlocks.containsKey(content)){
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num += (double) ct.getByte("Count");
                                                missingBlocks.put(content, num);
                                            }
                                        }
                                        if (id.equals("minecraft:coal_block")){
                                            content = new Pair<>(Material.COAL_BLOCK, (byte) 0);
                                            if (!missingBlocks.containsKey(content)){
                                                missingBlocks.put(content, (double) ct.getByte("Count"));
                                            } else {
                                                double num = missingBlocks.get(content);
                                                num += (double) ct.getByte("Count");
                                                missingBlocks.put(content, num);
                                            }
                                        }
                                    }
                                }
                                break;
                            case 62://burning furnace
                                itemToConsume = 61;
                                break;
                            case 63:// signs
                            case 68:
                                itemToConsume = 323;
                                break;
                            case 93:// repeaters
                            case 94:
                                itemToConsume = 356;
                                break;
                            case 149:// comparators
                            case 150:
                                itemToConsume = 404;
                                break;
                            case 55:// redstone
                                itemToConsume = 331;
                                break;
                            case 118:// cauldron
                                itemToConsume = 380;
                                break;
                            case 124: // lit redstone lamp
                                itemToConsume = 123;
                                break;
                            case 75: // lit redstone torch
                                itemToConsume = 76;
                                break;
                            case 8:
                            case 9:  // don't require water to be in the chest
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                                break;
                            case 10:
                            case 11: // don't require lava either, yeah you could exploit this for free lava, so make sure you set a price per block
                                itemToConsume = 0;
                                qtyToConsume = 0.0;
                                break;
                            case 26:  //beds
                                itemToConsume = 355;
                                qtyToConsume = 0.5;
                                break;
                            case 64:  //doors
                                itemToConsume = 324;   //since doors and beds encompass two blocks, require only 0.5 block for each of the two blocks
                                qtyToConsume = 0.5;
                                break;
                            case 71:
                                itemToConsume = 330;
                                qtyToConsume = 0.5;
                                break;
                            case 193:
                                itemToConsume = 427;
                                qtyToConsume = 0.5;
                                break;
                            case 194:
                                itemToConsume = 428;
                                qtyToConsume = 0.5;
                                break;
                            case 195:
                                itemToConsume = 429;
                                qtyToConsume = 0.5;
                                break;
                            case 196:
                                itemToConsume = 430;
                                qtyToConsume = 0.5;
                                break;
                            case 197:
                                itemToConsume = 431;
                                qtyToConsume = 0.5;
                                break;
                            case 23: {
                                Tag t = block.getNbtData().getValue().get("Items");
                                ListTag lt = null;
                                if (t instanceof ListTag) {
                                    lt = (ListTag) t;
                                }
                                int numTNT = 0;
                                int numFireCharges = 0;
                                int numWaterBuckets = 0;
                                if (lt != null) {
                                    for (Tag entryTag : lt.getValue()) {
                                        if (entryTag instanceof CompoundTag) {
                                            CompoundTag cTag = (CompoundTag) entryTag;
                                            if (cTag.toString().contains("minecraft:tnt")) {
                                                numTNT += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:fire_charge")) {
                                                numFireCharges += cTag.getByte("Count");
                                            }
                                            if (cTag.toString().contains("minecraft:water_bucket")) {
                                                numWaterBuckets += cTag.getByte("Count");
                                            }
                                        }
                                    }
                                }
                                Pair<Material, Byte> content;
                                if (numTNT > 0) {
                                    content = new Pair<>(Material.TNT, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numTNT);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numTNT;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                if (numFireCharges > 0) {
                                    content = new Pair<>(Material.FIREBALL, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numFireCharges);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numFireCharges;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                if (numWaterBuckets > 0) {
                                    content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                                    if (!missingBlocks.containsKey(content)) {
                                        missingBlocks.put(content, (double) numWaterBuckets);
                                    } else {
                                        double num = missingBlocks.get(content);
                                        num += numWaterBuckets;
                                        missingBlocks.put(content, num);
                                    }
                                }
                                break;
                            }
                            case 43: { // for double slabs, require 2 slabs
                                itemToConsume = 44;
                                qtyToConsume = 2;
                                break;
                            }
                            case 125: { // for double wood slabs, require 2 wood slabs
                                itemToConsume = 126;
                                qtyToConsume = 2;
                                break;
                            }
                            case 181: { // for double red sandstone slabs, require 2 red sandstone slabs
                                itemToConsume = 182;
                                qtyToConsume = 2;
                                break;
                            }
                        }
                        if (itemToConsume != 0) {
                            Pair<Material, Byte> missingBlock = new Pair<>(Material.getMaterial(itemToConsume), (byte) 0);
                            if (!missingBlocks.containsKey(missingBlock)) {
                                missingBlocks.put(missingBlock, qtyToConsume);
                            } else {
                                Double num = missingBlocks.get(missingBlock);
                                num += qtyToConsume;
                                missingBlocks.put(missingBlock, num);
                            }
                        }
                        if (block.getType() != 0){
                            locMissingBlocks.addLast(new Pair<>(new Vector(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new Vector(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                    if (bukkitBlock.getType() == Material.DISPENSER && block.getType() == 23) {
                        boolean needReplace = false;
                        Tag t = block.getNbtData().getValue().get("Items");
                        ListTag lt = null;
                        if (t instanceof ListTag) {
                            lt = (ListTag) t;
                        }
                        int numTNT = 0;
                        int numFireCharges = 0;
                        int numWaterBuckets = 0;
                        if (lt != null) {
                            for (Tag entryTag : lt.getValue()) {
                                if (entryTag instanceof CompoundTag) {
                                    CompoundTag cTag = (CompoundTag) entryTag;
                                    if (cTag.toString().contains("minecraft:tnt")) {
                                        numTNT += cTag.getByte("Count");
                                    }
                                    if (cTag.toString().contains("minecraft:fire_charge")) {
                                        numFireCharges += cTag.getByte("Count");
                                    }
                                    if (cTag.toString().contains("minecraft:water_bucket")) {
                                        numWaterBuckets += cTag.getByte("Count");
                                    }
                                }
                            }
                        }
                        Dispenser bukkitDispenser = (Dispenser) bukkitBlock.getState();
                        //Bukkit.getLogger().info(String.format("TNT: %d, Fireballs: %d, Water buckets: %d", numTNT, numFireCharges, numWaterBuckets));
                        for (ItemStack iStack : bukkitDispenser.getInventory().getContents()) {
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
                        Pair<Material, Byte> content;
                        if (numTNT > 0) {
                            content = new Pair<>(Material.TNT, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numTNT);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numTNT;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (numFireCharges > 0) {
                            content = new Pair<>(Material.FIREBALL, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numFireCharges);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numFireCharges;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (numWaterBuckets > 0) {
                            content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                            if (!missingBlocks.containsKey(content)) {
                                missingBlocks.put(content, (double) numWaterBuckets);
                            } else {
                                double num = missingBlocks.get(content);
                                num += numWaterBuckets;
                                missingBlocks.put(content, num);
                            }
                            needReplace = true;
                        }
                        if (needReplace) {
                            numDiffBlocks++;
                            locMissingBlocks.addLast(new Pair<>(new Vector(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new Vector(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                    if (bukkitBlock.getType() == Material.FURNACE && block.getType() == 61){
                        //Count fuel in furnaces
                        ListTag list = block.getNbtData().getListTag("Items");
                        FurnaceInventory fInv = ((Furnace) bukkitBlock.getState()).getInventory();
                        byte needsRefill = 0;
                        if (list != null){
                            for (Tag t : list.getValue()){
                                if (!(t instanceof CompoundTag)){
                                    continue;
                                }
                                CompoundTag ct = (CompoundTag) t;
                                byte slot = ct.getByte("Slot");
                                if (slot == 2){//Ignore the result slot
                                    continue;
                                }
                                String id = ct.getString("id");
                                Pair<Material, Byte> content;
                                if (id.equals("minecraft:coal")){
                                    byte data = (byte)ct.getShort("Damage");
                                    byte count = ct.getByte("Count");
                                    if (slot == 0) {//Smelting slot
                                        if (fInv.getSmelting() != null && fInv.getSmelting().getData().getData() == data){
                                            count -= (byte) fInv.getSmelting().getAmount();
                                        }
                                    } else if (slot == 1) {//Fuel slot
                                        if (fInv.getFuel() != null && fInv.getFuel().getData().getData() == data){
                                            count -= (byte) fInv.getFuel().getAmount();
                                        }
                                    }
                                    if (count > 0) {
                                        content = new Pair<>(Material.COAL, data);
                                        if (!missingBlocks.containsKey(content)) {
                                            missingBlocks.put(content, (double) count);
                                        } else {
                                            double num = missingBlocks.get(content);
                                            num += (double) count;
                                            missingBlocks.put(content, num);
                                        }
                                        needsRefill++;
                                    }
                                }
                                if (id.equals("minecraft:coal_block")){
                                    byte count = ct.getByte("Count");
                                    //Smelting slot
                                    //Fuel slot
                                    if (slot == 0) {
                                        count -= fInv.getSmelting() != null ? (byte) fInv.getSmelting().getAmount() : 0;
                                    } else if (slot == 1) {
                                        count -= fInv.getFuel() != null ? (byte) fInv.getFuel().getAmount() : 0;
                                    }
                                    if (count > 0) {
                                        content = new Pair<>(Material.COAL_BLOCK, (byte) 0);
                                        if (!missingBlocks.containsKey(content)) {
                                            missingBlocks.put(content, (double) count);
                                        } else {
                                            double num = missingBlocks.get(content);
                                            num += (double) count;
                                            missingBlocks.put(content, num);
                                        }
                                        needsRefill++;
                                    }
                                }
                            }
                        }
                        if (needsRefill > 0){
                            numDiffBlocks++;
                            locMissingBlocks.addLast(new Pair<>(new Vector(offset.getBlockX() + x, offset.getBlockY() + y, offset.getBlockZ() + z),new Vector(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                        }
                    }
                }
            }
        }
        String repairStateName = craft.getNotificationPlayer().getUniqueId().toString();
        repairStateName += "_";
        repairStateName += repairName.replace(".schematic","");
        locMissingBlocksMap.put(repairStateName, locMissingBlocks);
        missingBlocksMap.put(repairStateName, missingBlocks);
        numDiffBlocksMap.put(repairStateName, numDiffBlocks);
        return clipboard;
    }

    @Override
    public Clipboard loadRegionRepairStateClipboard(String s, World world) {
        File dataDirectory = new File(plugin.getDataFolder(), "AssaultSnapshots");
        File file = new File(dataDirectory, s + ".schematic"); // The schematic file
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        WorldData worldData = weWorld.getWorldData();
        try {
            return ClipboardFormat.SCHEMATIC.getReader(new FileInputStream(file)).read(worldData);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String repairName) {
        return missingBlocksMap.get(repairName);
    }

    public ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String repairName) {
        return locMissingBlocksMap.get(repairName);
    }

    public long getNumDiffBlocks(String s) {
        return numDiffBlocksMap.get(s);
    }


    private Set<BaseBlock> baseBlocksFromCraft(Craft craft) {
        HashSet<BaseBlock> returnSet = new HashSet<>();
        BitmapHitBox hitBox = craft.getHitBox();
        World w = craft.getW();
        for (MovecraftLocation location : hitBox) {
            int id = w.getBlockTypeIdAt(location.toBukkit(w));
            byte data = w.getBlockAt(location.getX(), location.getY(), location.getZ()).getData();
            returnSet.add(new BaseBlock(id, data));
        }
        if (Settings.Debug) {
            Bukkit.getLogger().info(returnSet.toString());
        }
        return returnSet;
    }

    private HashHitBox solidBlockLocs(World w, CuboidRegion cr){
        HashHitBox returnSet = new HashHitBox();
        for (int x = cr.getMinimumPoint().getBlockX(); x <= cr.getMaximumPoint().getBlockX(); x++){
            for (int y = cr.getMinimumPoint().getBlockY(); y <= cr.getMaximumPoint().getBlockY(); y++){
                for (int z = cr.getMinimumPoint().getBlockZ(); z <= cr.getMaximumPoint().getBlockZ(); z++){
                    MovecraftLocation ml = new MovecraftLocation(x, y, z);
                    if (ml.toBukkit(w).getBlock().getType() != Material.AIR){
                        returnSet.add(ml);
                    }
                }
            }
        }
        return returnSet;
    }
}
