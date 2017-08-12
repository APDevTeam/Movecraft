package net.countercraft.movecraft.mapUpdater.update;

import com.earth2me.essentials.User;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.ChunkSection;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.TileEntitySign;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;



public class BlockCreateCommand extends UpdateCommand {

    @SuppressWarnings("deprecation")
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};


    private MovecraftLocation newBlockLocation;
    private Material type;
    private byte dataID;
    private Craft craft;

    public BlockCreateCommand(@NotNull MovecraftLocation newBlockLocation, @NotNull Material type, byte dataID, Craft craft) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.craft = craft;
    }



    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on

        boolean madeChanges = false;
        if (type != Material.AIR) {
            Block dstBlock = newBlockLocation.toBukkit(craft.getW()).getBlock();
            Material existingType = dstBlock.getType();
            byte existingData = dstBlock.getData();
            Material newType = type;
            byte newData = dataID;
            boolean delayed = false;


            //Removed for refactor
            // delay color changes if possible
            /*if (Settings.DelayColorChanges && craft != null) {
                if (existingType == newType && existingData != newData) {
                    boolean canBeDelayed = false;
                    if (existingType == Material.WOOL) { // you can delay wool blocks, except light gray ones
                        canBeDelayed = !(existingData == 8 || newData == 8);
                    }
                    if (existingType == Material.STAINED_CLAY) { // you can delay stained clay, except all gray and black ones
                        canBeDelayed = !(existingData == 7 || newData == 7);
                        if (existingData == 8 || newData == 8 || existingData == 15 || newData == 15) {
                            canBeDelayed = false;
                        }
                    }
                    if (
                            existingType == Material.STAINED_GLASS ||
                                    existingType == Material.STAINED_GLASS_PANE ||
                                    existingType == Material.CARPET) { // all stained glass can be delayed
                        canBeDelayed = true;
                    }
                    if (canBeDelayed && craft.getScheduledBlockChanges() != null) {
                        long whenToChange = System.currentTimeMillis() + 1000;
                        BlockCreateCommand newMup = new BlockCreateCommand(newBlockLocation, newType, newData, null);
                        HashMap<BlockTranslateCommand, Long> sUpd = craft.getScheduledBlockChanges();
                        boolean alreadyPresent = false;
                        for (BlockTranslateCommand j : sUpd.keySet()) {
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
            }*/

            // move the actual block
            if (!delayed) {
                Chunk nativeDstChunk = ((CraftChunk) dstBlock.getChunk()).getHandle();
                BlockPosition dstBlockPos = new BlockPosition(dstBlock.getX(), dstBlock.getY(), dstBlock.getZ());
                IBlockData dstIBD;

                if (existingType != newType || existingData != newData) { // only place the actual block if it has changed
                    // if no source block, just make the new block using the type and data info
                    dstIBD = CraftMagicNumbers.getBlock(newType).fromLegacyData(newData);
                    // this actually creates the block
                    ChunkSection dstSection = nativeDstChunk.getSections()[dstBlock.getY() >> 4];
                    if (dstSection == null) {
                        // Put a GLASS block to initialize the section. It will be replaced next with the real block.
                        nativeDstChunk.a(dstBlockPos, Blocks.GLASS.getBlockData());
                        dstSection = nativeDstChunk.getSections()[dstBlockPos.getY() >> 4];
                    }

                    dstSection.setType(dstBlock.getX() & 15, dstBlock.getY() & 15, dstBlock.getZ() & 15, dstIBD);
                    madeChanges = true;
                    craft.incrementBlockUpdates();
                }
            }

        }else{
            //It's air
            Block dstBlock = newBlockLocation.toBukkit(craft.getW()).getBlock();
            Chunk nativeDstChunk = ((CraftChunk) dstBlock.getChunk()).getHandle();
            BlockPosition dstBlockPos = new BlockPosition(dstBlock.getX(), dstBlock.getY(), dstBlock.getZ());
            IBlockData dstIBD;
            // if no source block, just make the new block using the type and data info
            dstIBD = CraftMagicNumbers.getBlock(type).fromLegacyData(dataID);
            // this actually creates the block
            ChunkSection dstSection = nativeDstChunk.getSections()[dstBlock.getY() >> 4];
            if (dstSection == null) {
                // Put a GLASS block to initialize the section. It will be replaced next with the real block.
                nativeDstChunk.a(dstBlockPos, Blocks.GLASS.getBlockData());
                dstSection = nativeDstChunk.getSections()[dstBlockPos.getY() >> 4];
            }

            dstSection.setType(dstBlock.getX() & 15, dstBlock.getY() & 15, dstBlock.getZ() & 15, dstIBD);
            madeChanges = true;
            craft.incrementBlockUpdates();


        }

        if (madeChanges) { // send map updates to clients, and perform various checks
            //Location loc = new Location(updateWorld, newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            newBlockLocation.toBukkit(craft.getW()).getBlock().getState().update(false, false);
        }


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

        int blockType;
        Block srcBlock;
        BlockPosition dstBlockPos;
        Chunk nativeDstChunk;
        // now remove the tile entities in the new location if they shouldn't be there
        //blockType = updateWorld.getBlockTypeIdAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
        srcBlock = newBlockLocation.toBukkit(craft.getW()).getBlock();
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
                    sendSignToPlayers();
                    nativeDstChunk.getTileEntities().put(dstBlockPos, tileEntity);
                }
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
