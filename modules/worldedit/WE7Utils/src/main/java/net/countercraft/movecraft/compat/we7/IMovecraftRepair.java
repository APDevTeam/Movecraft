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
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Slab;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class IMovecraftRepair extends MovecraftRepair {
    private final HashMap<String, ArrayDeque<Pair<Vector,Vector>>> locMissingBlocksMap = new HashMap<>();
    private final HashMap<String, Long> numDiffBlocksMap = new HashMap<>();
    private final HashMap<String, HashMap<Pair<Material, Byte>, Double>> missingBlocksMap = new HashMap<>();
    private final HashMap<String, Vector> distanceMap = new HashMap<>();
    private final Plugin plugin;

    public IMovecraftRepair(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean saveCraftRepairState(Craft craft, Sign sign) {
        BitmapHitBox hitBox = craft.getHitBox();
        File saveDirectory = new File(plugin.getDataFolder(), "RepairStates");
        World world = craft.getWorld();
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        File playerDirectory = new File(saveDirectory, craft.getNotificationPlayer().getUniqueId().toString());
        if (!playerDirectory.exists()){
            playerDirectory.mkdirs();
        }
        BlockVector3 origin = BlockVector3.at(sign.getX(),sign.getY(),sign.getZ());
        BlockVector3 minPos = BlockVector3.at(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ());
        BlockVector3 maxPos = BlockVector3.at(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ());
        CuboidRegion region = new CuboidRegion(minPos, maxPos);
        com.sk89q.worldedit.world.World weWorld = new BukkitWorld(world);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);
        Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        ForwardExtentCopy copy = new ForwardExtentCopy(source, region, clipboard.getOrigin(), clipboard, clipboard.getOrigin());
        BlockMask mask = new BlockMask(source, baseBlocksFromCraft(craft));
        copy.setSourceMask(mask);
        final HitBox outsideLocs = CollectionUtils.filter(solidBlockLocs(world, region), hitBox);
        try {
            Operations.complete(copy);
            for (MovecraftLocation ml : outsideLocs){
                clipboard.setBlock(BlockVector3.at(ml.getX(), ml.getY(), ml.getZ()), BukkitAdapter.asBlockType(Material.AIR).getDefaultState());
            }
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
        File schematicFile = new File(playerDirectory, sign.getLine(1) + ".schematic");
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
    public boolean saveRegionRepairState(World world, ProtectedRegion region) {
        File saveDirectory = new File(plugin.getDataFolder(), "AssaultSnapshots");
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
            ForwardExtentCopy copy = new ForwardExtentCopy(source, weRegion, clipboard, weMinPos);
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
    public Clipboard loadCraftRepairStateClipboard(Craft craft, Sign sign) {
        String s = craft.getNotificationPlayer().getUniqueId().toString() + sign.getLine(1);
        File dataDirectory = new File(plugin.getDataFolder(), "RepairStates");
        File playerDirectory = new File(dataDirectory, craft.getNotificationPlayer().getUniqueId().toString());
        File file = new File(playerDirectory, ChatColor.stripColor(sign.getLine(1)) + ".schematic"); // The schematic file
        Clipboard clipboard = null;
        ClipboardFormat format = ClipboardFormats.findByFile(file);
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
            HashMap<Pair<Material, Byte>, Double> missingBlocks = new HashMap<>();
            ArrayDeque<Pair<Vector,Vector>> locMissingBlocks = new ArrayDeque<>();
            HashMap<Vector,Material> materials = new HashMap<>();
            BlockVector3 minPoint = clipboard.getMinimumPoint();
            BlockVector3 length = clipboard.getDimensions();
            BlockVector3 distance = clipboard.getOrigin().subtract(clipboard.getMinimumPoint());

            Vector offset = new Vector(sign.getX()-distance.getBlockX(),sign.getY()-distance.getBlockY(),sign.getZ()-distance.getBlockZ());
            Vector difference = new Vector(minPoint.getBlockX() - offset.getBlockX(),minPoint.getBlockY() - offset.getBlockY(), minPoint.getBlockZ() - offset.getBlockZ());
            for (int y = 0; y <= length.getBlockY(); y++) {
                for (int z = 0; z <= length.getBlockZ(); z++) {
                    for (int x = 0; x <= length.getBlockX(); x++) {
                        BlockVector3 position = BlockVector3.at(x+clipboard.getMinimumPoint().getBlockX(),y+clipboard.getMinimumPoint().getBlockY(),z+clipboard.getMinimumPoint().getBlockZ());
                        int cx = offset.getBlockX() + x;
                        int cy = offset.getBlockY() + y;
                        int cz = offset.getBlockZ() + z;
                        BaseBlock block = clipboard.getFullBlock(position);
                        Material type = BukkitAdapter.adapt(block.getBlockType());
                        Location loc = new Location(sign.getWorld(),cx,cy,cz);
                        Block bukkitBlock = sign.getWorld().getBlockAt(loc);
                        boolean isImportant = true;

                        if (type.equals(Material.AIR) || type.equals(Material.CAVE_AIR) || type.equals(Material.VOID_AIR)){
                            isImportant = false;
                        }
                        boolean blockMissing = isImportant && type != bukkitBlock.getType();
                        //Check if single slabs are at locations where double slabs should be located and vice versa
                        if (type.name().endsWith("_SLAB")){
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
                            if (type.name().equals("WALL_SIGN")){
                                typeToConsume = Material.getMaterial("SIGN");
                            }
                            if (type == Material.ACACIA_WALL_SIGN){
                                typeToConsume = Material.ACACIA_SIGN;
                            }
                            if (type == Material.BIRCH_WALL_SIGN){
                                typeToConsume = Material.BIRCH_SIGN;
                            }
                            if (type == Material.DARK_OAK_WALL_SIGN){
                                typeToConsume = Material.DARK_OAK_SIGN;
                            }
                            if (type == Material.OAK_WALL_SIGN){
                                typeToConsume = Material.OAK_SIGN;
                            }
                            if (type == Material.SPRUCE_WALL_SIGN){
                                typeToConsume = Material.SPRUCE_SIGN;
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
                                Pair<Material, Byte> content;
                                if (numTNT > 0){
                                    content = new Pair<>(Material.TNT, (byte) 0);
                                    if (missingBlocks.containsKey(content)){
                                        double count = missingBlocks.get(content);
                                        count += numTNT;
                                        missingBlocks.put(content, count);
                                    } else {
                                        missingBlocks.put(content, (double) numTNT);
                                    }
                                }
                                if (numFirecharge > 0){
                                    content = new Pair<>(Material.FIRE_CHARGE, (byte) 0);
                                    if (missingBlocks.containsKey(content)){
                                        double count = missingBlocks.get(content);
                                        count += numFirecharge;
                                        missingBlocks.put(content, count);
                                    } else {
                                        missingBlocks.put(content, (double) numFirecharge);
                                    }
                                }
                                if (numWaterBucket > 0){
                                    content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                                    if (missingBlocks.containsKey(content)){
                                        double count = missingBlocks.get(content);
                                        count += numWaterBucket;
                                        missingBlocks.put(content, count);
                                    } else {
                                        missingBlocks.put(content, (double) numWaterBucket);
                                    }
                                }
                            }
                            locMissingBlocks.addLast(new Pair<>(new Vector(cx,cy,cz),new Vector(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
                            numDiffBlocks++;
                            Pair<Material, Byte> missingBlock = new Pair<>(typeToConsume, (byte) 0);
                            if (missingBlocks.containsKey(missingBlock)){
                                double count = missingBlocks.get(missingBlock);
                                count += qtyToConsume;
                                missingBlocks.put(missingBlock,count);
                            } else {
                                missingBlocks.put(missingBlock,qtyToConsume);
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
                            Pair<Material, Byte> content;
                            if (numTNT > 0){
                                content = new Pair<>(Material.TNT, (byte) 0);
                                if (missingBlocks.containsKey(content)){
                                    double count = missingBlocks.get(content);
                                    count += numTNT;
                                    missingBlocks.put(content, count);
                                } else {
                                    missingBlocks.put(content, (double) numTNT);
                                }
                                needsReplace = true;
                            }
                            if (numFireCharge > 0){
                                content = new Pair<>(Material.FIRE_CHARGE, (byte) 0);
                                if (missingBlocks.containsKey(content)){
                                    double count = missingBlocks.get(content);
                                    count += numFireCharge;
                                    missingBlocks.put(content, count);
                                } else {
                                    missingBlocks.put(content, (double) numFireCharge);
                                }
                                needsReplace = true;
                            }
                            if (numWaterBucket > 0){
                                content = new Pair<>(Material.WATER_BUCKET, (byte) 0);
                                if (missingBlocks.containsKey(content)){
                                    double count = missingBlocks.get(content);
                                    count += numWaterBucket;
                                    missingBlocks.put(content, count);
                                } else {
                                    missingBlocks.put(content, (double) numWaterBucket);
                                }
                                needsReplace = true;
                            }
                            if (needsReplace){
                                locMissingBlocks.addLast(new Pair<>(new Vector(cx,cy,cz),new Vector(position.getBlockX(),position.getBlockY(),position.getBlockZ())));
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
    public Clipboard loadRegionRepairStateClipboard(String s, World world) {
        Clipboard clipboard;
        File dataDirectory = new File(plugin.getDataFolder(), "AssaultSnapshots");
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
    public HashMap<Pair<Material, Byte>, Double> getMissingBlocks(String s) {
        return missingBlocksMap.get(s);
    }

    @Override
    public ArrayDeque<Pair<Vector, Vector>> getMissingBlockLocations(String s) {
        return locMissingBlocksMap.get(s);
    }

    @Override
    public long getNumDiffBlocks(String s) {
        return numDiffBlocksMap.get(s);
    }

    private Set<BaseBlock> baseBlocksFromCraft(Craft craft) {
        HashSet<BaseBlock> returnSet = new HashSet<>();
        BitmapHitBox hitBox = craft.getHitBox();
        World w = craft.getWorld();
        for (MovecraftLocation location : hitBox) {
            Material type = w.getBlockAt(location.toBukkit(w)).getType();
            returnSet.add(BukkitAdapter.asBlockType(type).getDefaultState().toBaseBlock());
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
