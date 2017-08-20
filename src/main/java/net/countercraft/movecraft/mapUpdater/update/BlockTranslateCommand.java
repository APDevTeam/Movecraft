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
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.ChunkSection;
import net.minecraft.server.v1_10_R1.EnumBlockRotation;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.StructureBoundingBox;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.TileEntitySign;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class that stores the data about a single blocks changes to the map in an unspecified world. The world is retrieved contextually from the submitting craft.
 */
public class BlockTranslateCommand extends UpdateCommand {
    private static final EnumBlockRotation ROTATION[];

    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }

    private final @NotNull MovecraftLocation newBlockLocation;
    private final @NotNull MovecraftLocation oldBlockLocation;
    private final @NotNull Material type;
    private final byte dataID;
    private final @NotNull Rotation rotation;
    private final @NotNull Craft craft;

    @SuppressWarnings("deprecation")
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};



    public BlockTranslateCommand(@NotNull MovecraftLocation oldBlockLocation, @NotNull MovecraftLocation newBlockLocation, @NotNull Material type, @NotNull byte dataID, @NotNull Rotation rotation, @NotNull Craft craft) {
        this.oldBlockLocation = oldBlockLocation;
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.rotation = rotation;
        this.craft = craft;
    }

    public BlockTranslateCommand(@NotNull MovecraftLocation oldBlockLocation, @NotNull MovecraftLocation newBlockLocation, @NotNull Material type, @NotNull byte dataID, @NotNull Craft craft) {
        this.oldBlockLocation = oldBlockLocation;
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.rotation = Rotation.NONE;
        this.craft = craft;

    }


    @NotNull
    public Material getType() {
        return type;
    }

    public byte getDataID() {
        return dataID;
    }

    @NotNull
    public MovecraftLocation getOldBlockLocation() {
        return oldBlockLocation;
    }

    @NotNull
    public MovecraftLocation getNewBlockLocation() {
        return newBlockLocation;
    }

    @NotNull
    public Rotation getRotation() {
        return rotation;
    }

    @NotNull
    public Craft getCraft() {
        return craft;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate(){
        World nativeWorld = ((CraftWorld) craft.getW()).getHandle();
        //Old block
        Block oldBlock = oldBlockLocation.toBukkit(craft.getW()).getBlock();
        Chunk oldChunk = ((CraftChunk) oldBlock.getChunk()).getHandle();
        BlockPosition oldBlockPos = new BlockPosition(oldBlock.getX(), oldBlock.getY(), oldBlock.getZ());
        IBlockData blockData = oldChunk.getBlockData(oldBlockPos).a(ROTATION[rotation.ordinal()]);

        //New block
        Block newBlock = newBlockLocation.toBukkit(craft.getW()).getBlock();
        Chunk newChunk = ((CraftChunk) newBlock.getChunk()).getHandle();
        BlockPosition newBlockPosition = new BlockPosition(newBlock.getX(), newBlock.getY(), newBlock.getZ());
        Material existingType = newBlock.getType();
        byte existingData = newBlock.getData();

        TileEntity tile = null;
        if (Arrays.binarySearch(tileEntityBlocksToPreserve, oldBlock.getTypeId()) >= 0) {
            tile = oldChunk.getTileEntities().get(oldBlockPos);
        }
        if (existingType == type && existingData == dataID && tile == null) {
            return;
        }

        //get the nextTick to move with the tile
        StructureBoundingBox srcBoundingBox = new StructureBoundingBox(oldBlock.getX(), oldBlock.getY(), oldBlock.getZ(), oldBlock.getX() + 1, oldBlock.getY() + 1, oldBlock.getZ() + 1);
        List<NextTickListEntry> entries = nativeWorld.a(srcBoundingBox, true);
        NextTickListEntry nextTickEntry = null;
        if (entries != null) {
            nextTickEntry = entries.get(0);//new NextTickListEntry(entries.get(0).a,entries.get(0).);
        }



        // this actually creates the block
        ChunkSection newSection = newChunk.getSections()[newBlockPosition.getY() >> 4];
        if (newSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            newChunk.a(newBlockPosition, Blocks.GLASS.getBlockData());
            newSection = newChunk.getSections()[newBlockPosition.getY() >> 4];
        }

        //TODO: figure out what the bitwise is for
        newSection.setType(newBlock.getX() & 15, newBlock.getY() & 15, newBlock.getZ() & 15, blockData);
        //craft.incrementBlockUpdates();

        // if you had a source block, also move the tile entity, and if there is a next tick entry, move that too
        if (tile != null) {
            tile.setPosition(newBlockPosition);
            craft.incrementBlockUpdates();
            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                craft.incrementBlockUpdates ((int) (craft.getOrigBlockCount() * 0.005));
            }
            newChunk.getTileEntities().put(newBlockPosition, tile);
            if (tile instanceof TileEntitySign) {
                sendSignToPlayers();
            }
            if (nativeWorld.capturedTileEntities.containsKey(oldBlockPos)) {
                // Is this really necessary?
                nativeWorld.capturedTileEntities.remove(oldBlockPos);
                nativeWorld.capturedTileEntities.put(newBlockPosition, tile);
            }
        }

        if (nextTickEntry != null) {
            final long currentTime = nativeWorld.worldData.getTime();
            BlockPosition position = nextTickEntry.a;
            int dx = newBlockLocation.getX() - oldBlockLocation.getX();
            int dy = newBlockLocation.getY() - oldBlockLocation.getY();
            int dz = newBlockLocation.getZ() - oldBlockLocation.getZ();
            position = position.a(dx, dy, dz);
            nativeWorld.b(position, nextTickEntry.a(), (int) (nextTickEntry.b - currentTime), nextTickEntry.c);
        }

        //send updates to players
        newBlockLocation.toBukkit(craft.getW()).getBlock().getState().update(false, false);

        //Do comperator stuff
        if (type == Material.REDSTONE_COMPARATOR_OFF) { // for some reason comparators are flakey, have to do it twice sometimes
            //Block b = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            Block b = newBlockLocation.toBukkit(craft.getW()).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR_OFF) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }
        if (type == Material.REDSTONE_COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = newBlockLocation.toBukkit(craft.getW()).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }


        // clean up any left over tile entities on blocks that do not need tile entities, also process signs
        int blockType = oldBlockLocation.toBukkit(craft.getW()).getBlock().getTypeId();
        if (tile!=null && Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
            oldChunk.getTileEntities().remove(oldBlockPos);
        }


        // now remove the tile entities in the new location if they shouldn't be there
        //blockType = updateWorld.getBlockTypeIdAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
        if (Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
            newChunk.getTileEntities().remove(newBlockPosition);
        }

        //Now remove the old block
        boolean foundReplacement = false;
        for (UpdateCommand updateCommand : MapUpdateManager.getInstance().getUpdates()) {
            if (updateCommand instanceof BlockTranslateCommand && ((BlockTranslateCommand) updateCommand).newBlockLocation.equals(this.oldBlockLocation)) {
                foundReplacement = true;
                break;
            }
        }
        if (!foundReplacement)
            oldBlockLocation.toBukkit(craft.getW()).getBlock().setType(Material.AIR);


        if (type == Material.SIGN_POST || type == Material.WALL_SIGN) {
            //srcBlock = updateWorld.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
            TileEntity tileEntity = newChunk.getTileEntities().get(newBlockPosition);
            if (tileEntity instanceof TileEntitySign) {
                processSign(tileEntity, craft);
                sendSignToPlayers();
                newChunk.getTileEntities().put(newBlockPosition, tileEntity);
            }
        }

    }

    @SuppressWarnings("deprecation")
    private void sendSignToPlayers() {
        BlockState bs = newBlockLocation.toBukkit(craft.getW()).getBlock().getState();
        if (bs instanceof Sign) {
            Sign sign = (Sign) bs;
            for (Player p : craft.getW().getPlayers()) { // this is necessary because signs do not get updated client side correctly without refreshing the chunks, which causes a memory leak in the clients
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

    @SuppressWarnings("deprecation")
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
                    long detectionRange;
                    if (tposy > tcraft.getW().getSeaLevel()) {
                        detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getDetectionMultiplier());
                    } else {
                        detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getUnderwaterDetectionMultiplier());
                    }
                    if (distsquared < detectionRange * detectionRange && tcraft.getNotificationPlayer() != craft.getNotificationPlayer()) {
                        // craft has been detected
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
            } else {
                TESign.lines[1] = new ChatComponentText("");
                TESign.lines[2] = new ChatComponentText("");
                TESign.lines[3] = new ChatComponentText("");
            }
        }
        if (firstLine.equalsIgnoreCase("Status:")) {
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
                    InventoryHolder inventoryHolder = (InventoryHolder) ml.toBukkit(craft.getW()).getBlock().getState();
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

        if (firstLine.equalsIgnoreCase("Speed:")) {
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

