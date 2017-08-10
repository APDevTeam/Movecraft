/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.mapUpdater.update;

import com.earth2me.essentials.User;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.EnumBlockRotation;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.StructureBoundingBox;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.TileEntitySign;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class MapUpdateCommand implements UpdateCommand {
    private static EnumBlockRotation ROTATION[];

    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }

    private final MovecraftLocation newBlockLocation;
    private final Material type;
    private final byte dataID;
    private final Rotation rotation;
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};
    private MovecraftLocation blockLocation;
    private Craft craft;
    private World updateWorld;

    public MapUpdateCommand(MovecraftLocation blockLocation, MovecraftLocation newBlockLocation, Material type, byte dataID, Rotation rotation, Craft craft) {
        this.blockLocation = blockLocation;
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.rotation = rotation;
        this.craft = craft;
    }

    public MapUpdateCommand(MovecraftLocation blockLocation, MovecraftLocation newBlockLocation, Material type, byte dataID, Craft craft) {
        this.blockLocation = blockLocation;
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.rotation = Rotation.NONE;
        this.craft = craft;

    }

    public MapUpdateCommand( MovecraftLocation newBlockLocation, Material type, byte dataID, Craft craft) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.rotation = Rotation.NONE;
        this.craft = craft;
    }

    public Material getType() {
        return type;
    }

    public byte getDataID() {
        return dataID;
    }

    public MovecraftLocation getOldBlockLocation() {
        return blockLocation;
    }

    public MovecraftLocation getNewBlockLocation() {
        return newBlockLocation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Craft getCraft() {
        return craft;
    }

    @Override
    public void doUpdate() {
        //TODO: fix this
        net.minecraft.server.v1_10_R1.World nativeWorld = ((CraftWorld) updateWorld).getHandle();
        IBlockData blockData = null;
        TileEntity tile=null;
        NextTickListEntry nextTickEntry = null;

        //Removed chunkloading and relighting

        // TODO: make this go in chunks instead of block by block, same with the block placement system

        if (type != Material.AIR &&  blockLocation != null) {
            Block srcBlock = updateWorld.getBlockAt(blockLocation.getX(), blockLocation.getY(), blockLocation.getZ());
            net.minecraft.server.v1_10_R1.Chunk nativeSrcChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
            StructureBoundingBox srcBoundingBox = new StructureBoundingBox(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ(), srcBlock.getX() + 1, srcBlock.getY() + 1, srcBlock.getZ() + 1);
            List<NextTickListEntry> entries = nativeWorld.a(srcBoundingBox, true);
            if (entries != null) {
                nextTickEntry = entries.get(0);//new NextTickListEntry(entries.get(0).a,entries.get(0).);
            }

            BlockPosition srcBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
            blockData = nativeSrcChunk.getBlockData(srcBlockPos);

            if (Arrays.binarySearch(tileEntityBlocksToPreserve, srcBlock.getTypeId()) >= 0) {
                tile = nativeSrcChunk.getTileEntities().get(srcBlockPos);
            }
        }
        //TODO: Reset block updates
        //blockUpdatesPerCraft.clear();

        // now do the block updates, move entities when you set the block they are on

        boolean madeChanges = false;
        if (type != Material.AIR) {
            Block srcBlock;
            if (blockLocation != null) {
                srcBlock = updateWorld.getBlockAt(blockLocation.getX(), blockLocation.getY(), blockLocation.getZ());
            } else {
                srcBlock = null;
            }
            Block dstBlock = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            Material existingType = dstBlock.getType();
            byte existingData = dstBlock.getData();
            Material newType = type;
            byte newData = dataID;
            boolean delayed = false;

            // delay color changes if possible
            if (Settings.DelayColorChanges && craft != null) {
                if (existingType == newType && existingData != newData) {
                    boolean canBeDelayed = false;
                    if (existingType == Material.WOOL) { // you can delay wool blocks, except light gray ones
                        canBeDelayed = !(existingData == 8 || newData == 8);
                    }
                    if (existingType == Material.STAINED_CLAY) { // you can delay stained clay, except all gray and black ones
                        canBeDelayed = !(existingData == 7 || newData == 7);
                        if (existingData == 8 || newData == 8) {
                            canBeDelayed = false;
                        }
                        if (existingData == 15 || newData == 15) {
                            canBeDelayed = false;
                        }
                    }
                    if (existingType == Material.STAINED_GLASS) { // all stained glass can be delayed
                        canBeDelayed = true;
                    }
                    if (existingType == Material.STAINED_GLASS_PANE) { // all glass panes can be delayed
                        canBeDelayed = true;
                    }
                    if (existingType == Material.CARPET) { // all carpet can be delayed
                        canBeDelayed = true;
                    }
//                                        if (existingType == 239) { // all glazed terracotta be delayed
//                                            canBeDelayed = true;
//                                        }
//                                        if (existingType == 251) { // all concrete can be delayed
//                                            canBeDelayed = true;
//                                        }
//                                        if (existingType == 252) { // all concrete powder can be delayed
//                                            canBeDelayed = true;
//                                        }

                    if (canBeDelayed && craft.getScheduledBlockChanges() != null) {
                        long whenToChange = System.currentTimeMillis() + 1000;
                        MapUpdateCommand newMup = new MapUpdateCommand(newBlockLocation, newType, newData, null);
                        HashMap<MapUpdateCommand, Long> sUpd = craft.getScheduledBlockChanges();
                        boolean alreadyPresent = false;
                        for (MapUpdateCommand j : sUpd.keySet()) {
                            if (j.getNewBlockLocation().equals(newMup.getNewBlockLocation())) {
                                alreadyPresent = true;
                                break;
                            }
                        }
                        if (!alreadyPresent) {
                            craft.getScheduledBlockChanges().put(newMup, whenToChange);
                        }
                        delayed = true;
                    }
                }
            }

            // move the actual block
            if (!delayed) {
                net.minecraft.server.v1_10_R1.Chunk nativeDstChunk = ((CraftChunk) dstBlock.getChunk()).getHandle();
                BlockPosition dstBlockPos = new BlockPosition(dstBlock.getX(), dstBlock.getY(), dstBlock.getZ());
                net.minecraft.server.v1_10_R1.Chunk nativeSrcChunk = null;
                BlockPosition srcBlockPos = null;
                if (srcBlock != null) {
                    nativeSrcChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                    srcBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                }
                IBlockData dstIBD;

                if (existingType != newType || existingData != newData) { // only place the actual block if it has changed
                    // if there is a source block, copy the data from it, modifying with rotation (note that some updates don't have source blocks, like a repair)
                    if (srcBlock != null) {
                        dstIBD = blockData.a(ROTATION[rotation.ordinal()]);

                    } else {
                        // if no source block, just make the new block using the type and data info
                        dstIBD = CraftMagicNumbers.getBlock(newType).fromLegacyData(newData);
                    }
                    // this actually creates the block
                    net.minecraft.server.v1_10_R1.ChunkSection dstSection = nativeDstChunk.getSections()[dstBlock.getY() >> 4];
                    if (dstSection == null) {
                        // Put a GLASS block to initialize the section. It will be replaced next with the real block.
                        nativeDstChunk.a(dstBlockPos, net.minecraft.server.v1_10_R1.Blocks.GLASS.getBlockData());
                        dstSection = nativeDstChunk.getSections()[dstBlockPos.getY() >> 4];
                    }

                    dstSection.setType(dstBlock.getX() & 15, dstBlock.getY() & 15, dstBlock.getZ() & 15, dstIBD);
                    madeChanges = true;
                    craft.incrementBlockUpdates();
                }

                if (srcBlock != null) {
                    // if you had a source block, also move the tile entity, and if there is a next tick entry, move that too
                    if (tile != null) {
                        tile.setPosition(dstBlockPos);
                        craft.incrementBlockUpdates();
                        madeChanges = true;
                        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                            craft.incrementBlockUpdates ((int) (craft.getOrigBlockCount() * 0.005));
                        }
                        nativeDstChunk.getTileEntities().put(dstBlockPos, tile);
                        if (tile instanceof TileEntitySign) {
                            sendSignToPlayers(updateWorld, this);
                        }
                        if (nativeWorld.capturedTileEntities.containsKey(srcBlockPos)) {
                            // Is this really necessary?
                            nativeWorld.capturedTileEntities.remove(srcBlockPos);
                            nativeWorld.capturedTileEntities.put(dstBlockPos, tile);
                        }
                    }

                    //NextTickListEntry entry = nextTickMap.get(mapUpdateIndex);
                    if (nextTickEntry != null) {
                        final long currentTime = nativeWorld.worldData.getTime();
                        BlockPosition position = nextTickEntry.a;
                        int dx = newBlockLocation.getX() - blockLocation.getX();
                        int dy = newBlockLocation.getY() - blockLocation.getY();
                        int dz = newBlockLocation.getZ() - blockLocation.getZ();
                        position = position.a(dx, dy, dz);
                        nativeWorld.b(position, nextTickEntry.a(), (int) (nextTickEntry.b - currentTime), nextTickEntry.c);
                    }

                }
            }

        }

        if (madeChanges) { // send map updates to clients, and perform various checks
            //Location loc = new Location(updateWorld, newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            updateWorld.getBlockAt(newBlockLocation.toBukkit(updateWorld)).getState().update(false, false);
        }


        //Do comperator stuff

        if (type == Material.REDSTONE_COMPARATOR_OFF) { // for some reason comparators are flakey, have to do it twice sometimes
            //Block b = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            Block b = newBlockLocation.toBukkit(updateWorld).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR_OFF) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }
        if (type == Material.REDSTONE_COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            if (b.getType() != Material.REDSTONE_COMPARATOR) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }


        // clean up any left over tile entities on blocks that do not need tile entities, also process signs

        int blockType;
        Block srcBlock;
        BlockPosition dstBlockPos;
        net.minecraft.server.v1_10_R1.Chunk nativeDstChunk;
        if (blockLocation != null) {
            blockType = blockLocation.toBukkit(updateWorld).getBlock().getTypeId();
            // updateWorld.getBlockTypeIdAt(blockLocation.getX(), blockLocation.getY(), i.getOldBlockLocation().getZ());
            srcBlock = blockLocation.toBukkit(updateWorld).getBlock();
            //w.getBlockAt(i.getOldBlockLocation().getX(), i.getOldBlockLocation().getY(), i.getOldBlockLocation().getZ());
            nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
            dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
            if (Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
                nativeDstChunk.getTileEntities().remove(dstBlockPos);
            }

        }
        // now remove the tile entities in the new location if they shouldn't be there
        //blockType = updateWorld.getBlockTypeIdAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
        srcBlock = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
        blockType = srcBlock.getTypeId();
        nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
        dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
        if (Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
            nativeDstChunk.getTileEntities().remove(dstBlockPos);
        }

        if (type == Material.SIGN_POST || type == Material.WALL_SIGN) {
            if (craft != null) {
                //srcBlock = updateWorld.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                TileEntity tileEntity = nativeDstChunk.getTileEntities().get(dstBlockPos);
                if (tileEntity instanceof TileEntitySign) {
                    processSign(tileEntity, craft);
                    sendSignToPlayers(updateWorld, this);
                    nativeDstChunk.getTileEntities().put(dstBlockPos, tileEntity);
                }
            }
        }
    }

    private void sendSignToPlayers(World w, MapUpdateCommand i) {
        BlockState bs = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).getState();
        if (bs instanceof Sign) {
            Sign sign = (Sign) bs;
            for (Player p : w.getPlayers()) { // this is necessary because signs do not get updated client side correctly without refreshing the chunks, which causes a memory leak in the clients
                int playerChunkX = p.getLocation().getBlockX() >> 4;
                int playerChunkZ = p.getLocation().getBlockZ() >> 4;
                if (Math.abs(playerChunkX - sign.getChunk().getX()) < 4)
                    if (Math.abs(playerChunkZ - sign.getChunk().getZ()) < 4) {
                        p.sendBlockChange(sign.getLocation(), 63, (byte) 0);
                        p.sendBlockChange(sign.getLocation(), sign.getTypeId(), sign.getRawData());
                    }
            }
            sign.update(false, false);
        }
    }

    private void processSign(TileEntity tileEntity, Craft craft) {
        if (craft == null) {
            return;
        }
        TileEntitySign TESign = (TileEntitySign) tileEntity;
        if (TESign == null)
            return;
        if (TESign.lines == null)
            return;
        if (TESign.lines[0] == null)
            return;
        if (Settings.AllowCrewSigns && TESign.lines[0].toPlainText().equalsIgnoreCase("Crew:")) {
            String crewName = TESign.lines[1].toPlainText();
            Player crewPlayer = Movecraft.getInstance().getServer().getPlayer(crewName);
            if (crewPlayer != null) {
                Location loc = new Location(craft.getW(), TESign.getPosition().getX(), TESign.getPosition().getY(), TESign.getPosition().getZ());
                loc = loc.subtract(0, 1, 0);
                if (craft.getW().getBlockAt(loc).getType().equals(Material.BED_BLOCK)) {
                    crewPlayer.setBedSpawnLocation(loc);
                    if (Settings.SetHomeToCrewSign)

                        if (Movecraft.getInstance().getEssentialsPlugin() != null) {
                            User u = Movecraft.getInstance().getEssentialsPlugin().getUser(crewPlayer);
                            u.setHome("home", loc);
                        }

                }
            }
        }
        String firstLine = TESign.lines[0].toPlainText();
        if (firstLine.equalsIgnoreCase("Contacts:")) {
            if (CraftManager.getInstance().getCraftsInWorld(craft.getW()) != null) {
                if (craft != null) {
                    boolean foundContact = false;
                    int signLine = 1;
                    for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(craft.getW())) {
                        long cposx = craft.getMaxX() + craft.getMinX();
                        long cposy = craft.getMaxY() + craft.getMinY();
                        long cposz = craft.getMaxZ() + craft.getMinZ();
                        cposx = cposx >> 1;
                        cposy = cposy >> 1;
                        cposz = cposz >> 1;
                        long tposx = tcraft.getMaxX() + tcraft.getMinX();
                        long tposy = tcraft.getMaxY() + tcraft.getMinY();
                        long tposz = tcraft.getMaxZ() + tcraft.getMinZ();
                        tposx = tposx >> 1;
                        tposy = tposy >> 1;
                        tposz = tposz >> 1;
                        long diffx = cposx - tposx;
                        long diffy = cposy - tposy;
                        long diffz = cposz - tposz;
                        long distsquared = Math.abs(diffx) * Math.abs(diffx);
                        distsquared += Math.abs(diffy) * Math.abs(diffy);
                        distsquared += Math.abs(diffz) * Math.abs(diffz);
                        long detectionRange = 0;
                        if (tposy > tcraft.getW().getSeaLevel()) {
                            detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getDetectionMultiplier());
                        } else {
                            detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getUnderwaterDetectionMultiplier());
                        }
                        if (distsquared < detectionRange * detectionRange && tcraft.getNotificationPlayer() != craft.getNotificationPlayer()) {
                            // craft has been detected
                            foundContact = true;
                            String notification = "" + ChatColor.BLUE;
                            notification += tcraft.getType().getCraftName();
                            if (notification.length() > 9)
                                notification = notification.substring(0, 7);
                            notification += " ";
                            notification += (int) Math.sqrt(distsquared);
                            if (Math.abs(diffx) > Math.abs(diffz))
                                if (diffx < 0)
                                    notification += " E";
                                else
                                    notification += " W";
                            else if (diffz < 0)
                                notification += " S";
                            else
                                notification += " N";
                            if (signLine <= 3) {
                                TESign.lines[signLine] = new ChatComponentText(notification);
                                signLine++;
                            }
                        }
                    }
                    if (signLine < 4) {
                        for (int i = signLine; i < 4; i++) {
                            TESign.lines[signLine] = new ChatComponentText("");
                        }
                    }
                }
            } else {
                TESign.lines[1] = new ChatComponentText("");
                TESign.lines[2] = new ChatComponentText("");
                TESign.lines[3] = new ChatComponentText("");
            }
        }
        if (firstLine.equalsIgnoreCase("Status:")) {
            if (craft != null) {
                int fuel = 0;
                int totalBlocks = 0;
                HashMap<Integer, Integer> foundBlocks = new HashMap<>();
                for (MovecraftLocation ml : craft.getBlockList()) {
                    Integer blockID = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getTypeId();

                    if (foundBlocks.containsKey(blockID)) {
                        Integer count = foundBlocks.get(blockID);
                        if (count == null) {
                            foundBlocks.put(blockID, 1);
                        } else {
                            foundBlocks.put(blockID, count + 1);
                        }
                    } else {
                        foundBlocks.put(blockID, 1);
                    }

                    if (blockID == 61) {
                        Block b = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ());
                        InventoryHolder inventoryHolder = (InventoryHolder) craft.getW().getBlockAt(ml.getX(), ml.getY(),
                                ml.getZ()).getState();
                        if (inventoryHolder.getInventory().contains(263)
                                || inventoryHolder.getInventory().contains(173)) {
                            ItemStack[] istack = inventoryHolder.getInventory().getContents();
                            for (ItemStack i : istack) {
                                if (i != null) {
                                    if (i.getTypeId() == 263) {
                                        fuel += i.getAmount() * 8;
                                    }
                                    if (i.getTypeId() == 173) {
                                        fuel += i.getAmount() * 80;
                                    }
                                }
                            }
                        }
                    }
                    if (blockID != 0) {
                        totalBlocks++;
                    }
                }
                int signLine = 1;
                int signColumn = 0;
                for (ArrayList<Integer> alFlyBlockID : craft.getType().getFlyBlocks().keySet()) {
                    int flyBlockID = alFlyBlockID.get(0);
                    Double minimum = craft.getType().getFlyBlocks().get(alFlyBlockID).get(0);
                    if (foundBlocks.containsKey(flyBlockID) && minimum > 0) { // if it has a minimum, it should be considered for sinking consideration
                        int amount = foundBlocks.get(flyBlockID);
                        Double percentPresent = (double) (amount * 100 / totalBlocks);
                        int deshiftedID = flyBlockID;
                        if (deshiftedID > 10000) {
                            deshiftedID = (deshiftedID - 10000) >> 4;
                        }
                        String signText = "";
                        if (percentPresent > minimum * 1.04) {
                            signText += ChatColor.GREEN;
                        } else if (percentPresent > minimum * 1.02) {
                            signText += ChatColor.YELLOW;
                        } else {
                            signText += ChatColor.RED;
                        }
                        if (deshiftedID == 152) {
                            signText += "R";
                        } else if (deshiftedID == 42) {
                            signText += "I";
                        } else {
                            signText += CraftMagicNumbers.getBlock(deshiftedID).getName().substring(0, 1);
                        }

                        signText += " ";
                        signText += percentPresent.intValue();
                        signText += "/";
                        signText += minimum.intValue();
                        signText += "  ";
                        if (signColumn == 0) {
                            TESign.lines[signLine] = new ChatComponentText(signText);
                            signColumn++;
                        } else if (signLine < 3) {
                            String existingLine = TESign.lines[signLine].getText();
                            existingLine += signText;
                            TESign.lines[signLine] = new ChatComponentText(existingLine);
                            signLine++;
                            signColumn = 0;
                        }
                    }
                }
                String fuelText = "";
                Integer fuelRange = (int) ((fuel * (1 + craft.getType().getCruiseSkipBlocks())) / craft.getType().getFuelBurnRate());
                if (fuelRange > 1000) {
                    fuelText += ChatColor.GREEN;
                } else if (fuelRange > 100) {
                    fuelText += ChatColor.YELLOW;
                } else {
                    fuelText += ChatColor.RED;
                }
                fuelText += "Fuel range:";
                fuelText += fuelRange.toString();
                TESign.lines[signLine] = new ChatComponentText(fuelText);
            }
        }

        if (firstLine.equalsIgnoreCase("Speed:")) {
            if (craft != null) {
                String signText = "";
                String updateQty = "";
                if (craft.getCruising()) {
                    if (craft.getCruiseDirection() > 40) { // means ship is going vertical
                        signText += (int) ((craft.getCurSpeed() * 10) * (1 + craft.getType().getVertCruiseSkipBlocks()));
                        signText += " / ";
                        signText += (int) ((craft.getMaxSpeed() * 10) * (1 + craft.getType().getVertCruiseSkipBlocks()));
                    } else { // must be horizontal
                        signText += (int) ((craft.getCurSpeed() * 10) * (1 + craft.getType().getCruiseSkipBlocks()));
                        signText += " / ";
                        signText += (int) ((craft.getMaxSpeed() * 10) * (1 + craft.getType().getCruiseSkipBlocks()));
                    }
                }
                updateQty += craft.getBlockUpdates();
                TESign.lines[2] = new ChatComponentText(signText);
                TESign.lines[3] = new ChatComponentText(updateQty);
            }
        }
    }
}

