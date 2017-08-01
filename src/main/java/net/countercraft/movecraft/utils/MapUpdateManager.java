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

package net.countercraft.movecraft.utils;

import com.earth2me.essentials.User;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.DispenserBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.FastBlockChanger.ChunkUpdater;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.ChatComponentText;
import net.minecraft.server.v1_10_R1.EnumBlockRotation;
import net.minecraft.server.v1_10_R1.EnumParticle;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.PacketPlayOutWorldParticles;
import net.minecraft.server.v1_10_R1.StructureBoundingBox;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.TileEntitySign;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_10_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftBlockState;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MapUpdateManager extends BukkitRunnable {
    private static EnumBlockRotation ROTATION[];

    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }

    // only bother to store tile entities for blocks we care about (chests, dispensers, etc)
    // this is more important than it may seem. The more blocks that matter, the more likely
    // a circular trap will occur (a=b, b=c. c=a), potentially damaging tile data
    final int[] tileEntityBlocksToPreserve = {23, 25, 54, 61, 62, 63, 68, 137, 146, 151, 154, 158, 178, 210, 211};
    private final HashMap<World, ArrayList<MapUpdateCommand>> updates = new HashMap<World, ArrayList<MapUpdateCommand>>();
    ;
    private final HashMap<World, ArrayList<EntityUpdateCommand>> entityUpdates = new HashMap<World, ArrayList<EntityUpdateCommand>>();
    private final HashMap<World, ArrayList<ItemDropUpdateCommand>> itemDropUpdates = new HashMap<World, ArrayList<ItemDropUpdateCommand>>();
    public HashMap<Craft, Integer> blockUpdatesPerCraft = new HashMap<Craft, Integer>();
    private Random rand = new Random();

    private MapUpdateManager() {
    }

    public static MapUpdateManager getInstance() {
        return MapUpdateManagerHolder.INSTANCE;
    }

    private void addBlockUpdateTracking(Craft craft, int qty) {
        if (craft == null)
            return;
        if (blockUpdatesPerCraft.containsKey(craft)) {
            blockUpdatesPerCraft.put(craft, blockUpdatesPerCraft.get(craft) + qty);
        } else {
            blockUpdatesPerCraft.put(craft, qty);
        }
    }

    private void addBlockUpdateTracking(Craft craft) {
        if (craft == null)
            return;
        if (blockUpdatesPerCraft.containsKey(craft)) {
            blockUpdatesPerCraft.put(craft, blockUpdatesPerCraft.get(craft) + 1);
        } else {
            blockUpdatesPerCraft.put(craft, 1);
        }
    }

    public void run() {
        if (updates.isEmpty()) return;

        long startTime = System.currentTimeMillis();

        for (World w : updates.keySet()) {
            if (w != null) {
                int silhouetteBlocksSent = 0;
                List<MapUpdateCommand> updatesInWorld = updates.get(w);
                List<EntityUpdateCommand> entityUpdatesInWorld = entityUpdates.get(w);
                List<ItemDropUpdateCommand> itemDropUpdatesInWorld = itemDropUpdates.get(w);
                Map<MovecraftLocation, List<EntityUpdateCommand>> entityMap = new HashMap<MovecraftLocation, List<EntityUpdateCommand>>();
                Map<MovecraftLocation, List<ItemDropUpdateCommand>> itemMap = new HashMap<MovecraftLocation, List<ItemDropUpdateCommand>>();
                ArrayList<net.minecraft.server.v1_10_R1.Chunk> chunksToRelight = new ArrayList<net.minecraft.server.v1_10_R1.Chunk>();
                net.minecraft.server.v1_10_R1.World nativeWorld = ((CraftWorld) w).getHandle();

                Integer minx = Integer.MAX_VALUE;
                Integer maxx = Integer.MIN_VALUE;
                Integer miny = Integer.MAX_VALUE;
                Integer maxy = Integer.MIN_VALUE;
                Integer minz = Integer.MAX_VALUE;
                Integer maxz = Integer.MIN_VALUE;

                // Make sure all chunks are loaded, and mark them for relighting later
                for (MapUpdateCommand c : updatesInWorld) {

                    if (c != null) {
                        if (c.getOldBlockLocation() != null) {
                            if (c.getOldBlockLocation().getX() < minx)
                                minx = c.getOldBlockLocation().getX();
                            if (c.getOldBlockLocation().getY() < miny)
                                miny = c.getOldBlockLocation().getY();
                            if (c.getOldBlockLocation().getZ() < minz)
                                minz = c.getOldBlockLocation().getZ();
                            if (c.getOldBlockLocation().getX() > maxx)
                                maxx = c.getOldBlockLocation().getX();
                            if (c.getOldBlockLocation().getY() > maxy)
                                maxy = c.getOldBlockLocation().getY();
                            if (c.getOldBlockLocation().getZ() > maxz)
                                maxz = c.getOldBlockLocation().getZ();
                        }
                        if (c.getNewBlockLocation() != null) {
                            if (Settings.CompatibilityMode == false) {
                                Chunk chunk = w.getBlockAt(c.getNewBlockLocation().getX(), c.getNewBlockLocation().getY(), c.getNewBlockLocation().getZ()).getChunk();
                                net.minecraft.server.v1_10_R1.Chunk nativeChunk = ((CraftChunk) chunk).getHandle();
                                if (!chunksToRelight.contains(nativeChunk))
                                    chunksToRelight.add(nativeChunk);
                            }
                            if (!w.isChunkLoaded(c.getNewBlockLocation().getX() >> 4, c.getNewBlockLocation().getZ() >> 4)) {
                                w.loadChunk(c.getNewBlockLocation().getX() >> 4, c.getNewBlockLocation().getZ() >> 4);
                            }
                        }
                        if (c.getOldBlockLocation() != null) {
                            if (Settings.CompatibilityMode == false) {
                                Chunk chunk = w.getBlockAt(c.getOldBlockLocation().getX(), c.getOldBlockLocation().getY(), c.getOldBlockLocation().getZ()).getChunk();
                                net.minecraft.server.v1_10_R1.Chunk nativeChunk = ((CraftChunk) chunk).getHandle();
                                if (!chunksToRelight.contains(nativeChunk))
                                    chunksToRelight.add(nativeChunk);
                            }
                            if (!w.isChunkLoaded(c.getOldBlockLocation().getX() >> 4, c.getOldBlockLocation().getZ() >> 4)) {
                                w.loadChunk(c.getOldBlockLocation().getX() >> 4, c.getOldBlockLocation().getZ() >> 4);
                            }
                        }
                    }
                }

                // figure out block locations of entities, so you can move them with their blocks
                if (entityUpdatesInWorld != null) {
                    for (EntityUpdateCommand i : entityUpdatesInWorld) {
                        if (i != null) {
                            MovecraftLocation entityLoc = new MovecraftLocation(i.getNewLocation().getBlockX(), i.getNewLocation().getBlockY() - 1, i.getNewLocation().getBlockZ());
                            if (!entityMap.containsKey(entityLoc)) {
                                List<EntityUpdateCommand> entUpdateList = new ArrayList<EntityUpdateCommand>();
                                entUpdateList.add(i);
                                entityMap.put(entityLoc, entUpdateList);
                            } else {
                                List<EntityUpdateCommand> entUpdateList = entityMap.get(entityLoc);
                                entUpdateList.add(i);
                            }
                        }
                    }
                }

                // get any future redstone updates, IBData, and tile data so they can later be moved
/*				HashMap<MovecraftLocation,NextTickListEntry> nextTickMap=new HashMap<MovecraftLocation,NextTickListEntry>();

				if(minx!=Integer.MAX_VALUE) {
					StructureBoundingBox srcBoundingBox=new StructureBoundingBox(minx,miny,minz,maxx,maxy,maxz);
					List<NextTickListEntry> entries=nativeWorld.a(srcBoundingBox, true);
					if(entries!=null)
						for(NextTickListEntry entry : entries) {
							int x=entry.a.getX();
							int y=entry.a.getY();
							int z=entry.a.getZ();
							MovecraftLocation mloc=new MovecraftLocation(x,y,z);
							nextTickMap.put(mloc, entry);
							long currentTime = nativeWorld.worldData.getTime();
							Movecraft.getInstance().getServer().broadcastMessage("tick time: "+(entry.b-currentTime));
						}
				}*/

//				HashMap<MovecraftLocation,IBlockData> IBDMap=new HashMap<MovecraftLocation,IBlockData>();
//				HashMap<MovecraftLocation,TileEntity> tileMap=new HashMap<MovecraftLocation,TileEntity>();
                ArrayList<IBlockData> IBDMap = new ArrayList<IBlockData>();
                ArrayList<TileEntity> tileMap = new ArrayList<TileEntity>();
                ArrayList<NextTickListEntry> nextTickMap = new ArrayList<NextTickListEntry>();

                for (int mapUpdateIndex = 0; mapUpdateIndex < updatesInWorld.size(); mapUpdateIndex++) { // TODO: make this go in chunks instead of block by block, same with the block placement system
                    MapUpdateCommand i = updatesInWorld.get(mapUpdateIndex);
                    if (i != null) {
                        if (i.getTypeID() >= 0 && i.getWorldEditBaseBlock() == null && i.getOldBlockLocation() != null) {
                            Block srcBlock = w.getBlockAt(i.getOldBlockLocation().getX(), i.getOldBlockLocation().getY(), i.getOldBlockLocation().getZ());
                            net.minecraft.server.v1_10_R1.Chunk nativeSrcChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                            StructureBoundingBox srcBoundingBox = new StructureBoundingBox(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ(), srcBlock.getX() + 1, srcBlock.getY() + 1, srcBlock.getZ() + 1);
                            List<NextTickListEntry> entries = nativeWorld.a(srcBoundingBox, true);
                            if (entries != null) {
                                NextTickListEntry entry = entries.get(0);//new NextTickListEntry(entries.get(0).a,entries.get(0).);
                                long currentTime = nativeWorld.worldData.getTime();
//								if(entry.b-currentTime<100) {
//									Movecraft.getInstance().getServer().broadcastMessage("tick time: "+(entry.b-currentTime));
//								}
                                nextTickMap.add(entry);
                            }

                            BlockPosition srcBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                            IBlockData IBData = nativeSrcChunk.getBlockData(srcBlockPos);
                            IBDMap.add(IBData);

                            if (Arrays.binarySearch(tileEntityBlocksToPreserve, srcBlock.getTypeId()) >= 0) {
                                TileEntity tileEntity = nativeSrcChunk.getTileEntities().get(srcBlockPos);
//								if(tileEntity instanceof TileEntitySign) {
//									processSign(tileEntity, i.getCraft());
//								}
                                tileMap.add(tileEntity);
                            }
                        }
                    }
                    if (IBDMap.size() <= mapUpdateIndex)
                        IBDMap.add(null);
                    if (tileMap.size() <= mapUpdateIndex)
                        tileMap.add(null);
                    if (nextTickMap.size() <= mapUpdateIndex)
                        nextTickMap.add(null);
                }

                blockUpdatesPerCraft.clear();

                // now do the block updates, move entities when you set the block they are on
                for (int mapUpdateIndex = 0; mapUpdateIndex < updatesInWorld.size(); mapUpdateIndex++) {
                    MapUpdateCommand i = updatesInWorld.get(mapUpdateIndex);
                    boolean madeChanges = false;
                    if (i != null) {
                        if (i.getTypeID() >= 0) {
                            if (i.getWorldEditBaseBlock() == null) {
                                Block srcBlock;
                                if (i.getOldBlockLocation() != null) {
                                    srcBlock = w.getBlockAt(i.getOldBlockLocation().getX(), i.getOldBlockLocation().getY(), i.getOldBlockLocation().getZ());
                                } else {
                                    srcBlock = null;
                                }
                                Block dstBlock = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                                int existingType = dstBlock.getTypeId();
                                byte existingData = dstBlock.getData();
                                int newType = i.getTypeID();
                                byte newData = i.getDataID();
                                boolean delayed = false;

                                // delay color changes if possible
                                if (Settings.DelayColorChanges && i.getCraft() != null) {
                                    if (existingType == newType && existingData != newData) {
                                        boolean canBeDelayed = false;
                                        if (existingType == 35) { // you can delay wool blocks, except light gray ones
                                            canBeDelayed = true;
                                            if (existingData == 8 || newData == 8) {
                                                canBeDelayed = false;
                                            }
                                        }
                                        if (existingType == 159) { // you can delay stained clay, except all gray and black ones
                                            canBeDelayed = true;
                                            if (existingData == 7 || newData == 7) {
                                                canBeDelayed = false;
                                            }
                                            if (existingData == 8 || newData == 8) {
                                                canBeDelayed = false;
                                            }
                                            if (existingData == 15 || newData == 15) {
                                                canBeDelayed = false;
                                            }
                                        }
                                        if (existingType == 95) { // all stained glass can be delayed
                                            canBeDelayed = true;
                                        }
                                        if (existingType == 160) { // all glass panes can be delayed
                                            canBeDelayed = true;
                                        }
                                        if (existingType == 171) { // all carpet can be delayed
                                            canBeDelayed = true;
                                        }
                                        if (existingType == 239) { // all glazed terracotta be delayed
                                            canBeDelayed = true;
                                        }
                                        if (existingType == 251) { // all concrete can be delayed
                                            canBeDelayed = true;
                                        }
                                        if (existingType == 252) { // all concrete powder can be delayed
                                            canBeDelayed = true;
                                        }

                                        if (canBeDelayed && i.getCraft().getScheduledBlockChanges() != null) {
                                            long whenToChange = System.currentTimeMillis() + 1000;
                                            MapUpdateCommand newMup = new MapUpdateCommand(i.getNewBlockLocation(), newType, newData, null);
                                            HashMap<MapUpdateCommand, Long> sUpd = i.getCraft().getScheduledBlockChanges();
                                            boolean alreadyPresent = false;
                                            for (MapUpdateCommand j : sUpd.keySet()) {
                                                if (j.getNewBlockLocation().equals(newMup.getNewBlockLocation())) {
                                                    alreadyPresent = true;
                                                    break;
                                                }
                                            }
                                            if (!alreadyPresent) {
                                                i.getCraft().getScheduledBlockChanges().put(newMup, whenToChange);
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
                                            dstIBD = IBDMap.get(mapUpdateIndex);
                                            dstIBD = dstIBD.a(ROTATION[i.getRotation().ordinal()]);

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
                                        addBlockUpdateTracking(i.getCraft());
                                    }

                                    if (srcBlock != null) {
                                        // if you had a source block, also move the tile entity, and if there is a next tick entry, move that too
                                        TileEntity tileEntity = tileMap.get(mapUpdateIndex);
                                        if (tileEntity != null) {
                                            tileEntity.setPosition(dstBlockPos);
                                            addBlockUpdateTracking(i.getCraft());
                                            madeChanges = true;
                                            if (i.getTypeID() == 54 || i.getTypeID() == 146) {
                                                addBlockUpdateTracking(i.getCraft(), (int) (i.getCraft().getOrigBlockCount() * 0.005));
                                            }
                                            nativeDstChunk.getTileEntities().put(dstBlockPos, tileEntity);
                                            if (tileEntity instanceof TileEntitySign) {
                                                sendSignToPlayers(w, i);
                                            }
                                            if (nativeWorld.capturedTileEntities.containsKey(srcBlockPos)) {
                                                // Is this really necessary?
                                                nativeWorld.capturedTileEntities.remove(srcBlockPos);
                                                nativeWorld.capturedTileEntities.put(dstBlockPos, tileEntity);
                                            }
                                        }

                                        NextTickListEntry entry = nextTickMap.get(mapUpdateIndex);
                                        if (entry != null) {
                                            final long currentTime = nativeWorld.worldData.getTime();
                                            BlockPosition position = entry.a;
                                            int dx = i.getNewBlockLocation().getX() - i.getOldBlockLocation().getX();
                                            int dy = i.getNewBlockLocation().getY() - i.getOldBlockLocation().getY();
                                            int dz = i.getNewBlockLocation().getZ() - i.getOldBlockLocation().getZ();
                                            position = position.a(dx, dy, dz);
                                            nativeWorld.b(position, entry.a(), (int) (entry.b - currentTime), entry.c);
                                        }

                                    }
                                }

                                // move entities that were on the block you just placed
                                if (entityMap.containsKey(i.getNewBlockLocation())) {
                                    List<EntityUpdateCommand> mapUpdateList = entityMap.get(i.getNewBlockLocation());
                                    for (EntityUpdateCommand entityUpdate : mapUpdateList) {
                                        Entity entity = entityUpdate.getEntity();
                                        if (entity instanceof Player) {
                                            net.minecraft.server.v1_10_R1.EntityPlayer craftPlayer = ((CraftPlayer) entity).getHandle();
                                            craftPlayer.setPositionRotation(entityUpdate.getNewLocation().getX(), entityUpdate.getNewLocation().getY(), entityUpdate.getNewLocation().getZ(), entityUpdate.getNewLocation().getYaw(), craftPlayer.pitch);
                                            Location location = new Location(null, craftPlayer.locX, craftPlayer.locY, craftPlayer.locZ, craftPlayer.yaw, craftPlayer.pitch);
                                            craftPlayer.playerConnection.teleport(location);
                                            // send the blocks around the player to the player, so they don't fall through the floor or get bumped by other blocks
                                            Player p = (Player) entity;
                                            for (MapUpdateCommand muc : updatesInWorld) {
                                                if (muc != null) {
                                                    int disty = Math.abs(muc.getNewBlockLocation().getY() - entityUpdate.getNewLocation().getBlockY());
                                                    int distx = Math.abs(muc.getNewBlockLocation().getX() - entityUpdate.getNewLocation().getBlockX());
                                                    int distz = Math.abs(muc.getNewBlockLocation().getZ() - entityUpdate.getNewLocation().getBlockZ());
                                                    if (disty < 2 && distx < 2 && distz < 2) {
                                                        Location nloc = new Location(w, muc.getNewBlockLocation().getX(), muc.getNewBlockLocation().getY(), muc.getNewBlockLocation().getZ());
                                                        p.sendBlockChange(nloc, muc.getTypeID(), muc.getDataID());
                                                    }
                                                }
                                            }
                                        } else {
                                            entity.teleport(entityUpdate.getNewLocation());
                                        }
                                    }
                                    entityMap.remove(i.getNewBlockLocation());
                                }
                            } else { // this is for worldeditbaseblock!=null, IE: a repair
                                w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).setTypeId(i.getTypeID());
                                w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).setData(i.getDataID());
                                madeChanges = true;
                                // put inventory into dispensers if its a repair
                                if (i.getTypeID() == 23) {
                                    BaseBlock bb = (BaseBlock) i.getWorldEditBaseBlock();
                                    DispenserBlock dispBlock = new DispenserBlock(bb.getData());
                                    dispBlock.setNbtData(bb.getNbtData());
                                    int numFireCharges = 0;
                                    int numTNT = 0;
                                    int numWater = 0;
                                    for (BaseItemStack bi : dispBlock.getItems()) {
                                        if (bi != null) {
                                            if (bi.getType() == 46)
                                                numTNT += bi.getAmount();
                                            if (bi.getType() == 385)
                                                numFireCharges += bi.getAmount();
                                            if (bi.getType() == 326)
                                                numWater += bi.getAmount();
                                        }
                                    }
                                    Dispenser disp = (Dispenser) w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).getState();
                                    if (numFireCharges > 0) {
                                        ItemStack fireItems = new ItemStack(Material.FIREBALL, numFireCharges);
                                        disp.getInventory().addItem(fireItems);
                                    }
                                    if (numTNT > 0) {
                                        ItemStack TNTItems = new ItemStack(Material.TNT, numTNT);
                                        disp.getInventory().addItem(TNTItems);
                                    }
                                    if (numWater > 0) {
                                        ItemStack WaterItems = new ItemStack(Material.WATER_BUCKET, numWater);
                                        disp.getInventory().addItem(WaterItems);
                                    }
                                }
                                if (i.getWorldEditBaseBlock() instanceof SignBlock) {
                                    BlockState state = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).getState();
                                    if (state instanceof Sign) {
                                        Sign s = (Sign) state;
                                        for (int line = 0; line < ((SignBlock) i.getWorldEditBaseBlock()).getText().length; line++) {
                                            s.setLine(line, ((SignBlock) i.getWorldEditBaseBlock()).getText()[line]);
                                        }
                                        ((CraftBlockState) s).update(false, false);
                                    }
                                }
                            }
                        } else {
                            if (i.getTypeID() < -10) { // don't bother with tiny explosions
                                float explosionPower = i.getTypeID();
                                explosionPower = 0.0F - explosionPower / 100.0F;
                                Location loc = new Location(w, i.getNewBlockLocation().getX() + 0.5, i.getNewBlockLocation().getY() + 0.5, i.getNewBlockLocation().getZ());
                                this.createExplosion(loc, explosionPower);
                                //w.createExplosion(m.getNewBlockLocation().getX()+0.5, m.getNewBlockLocation().getY()+0.5, m.getNewBlockLocation().getZ()+0.5, explosionPower);
                            }
                        }
                    }
                    if (madeChanges) { // send map updates to clients, and perform various checks
                        Location loc = new Location(w, i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                        ((CraftBlockState) w.getBlockAt(loc).getState()).update(false, false);
                    }
                }

                for (MapUpdateCommand i : updatesInWorld) {
                    if (i != null) {
                        if (i.getTypeID() == 149) { // for some reason comparators are flakey, have to do it twice sometimes
                            Block b = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                            if (b.getTypeId() != 149) {
                                b.setTypeIdAndData(i.getTypeID(), i.getDataID(), false);
                            }
                        }
                        if (i.getTypeID() == 150) { // for some reason comparators are flakey, have to do it twice sometimes
                            Block b = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                            if (b.getTypeId() != 150) {
                                b.setTypeIdAndData(i.getTypeID(), i.getDataID(), false);
                            }
                        }
                        if (i.getWorldEditBaseBlock() != null) { // worldedit updates (IE: repairs) can have reconstruction issues due to block order, so paste twice
                            w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).setTypeId(i.getTypeID());
                            w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).setData(i.getDataID());
                            if (i.getWorldEditBaseBlock() instanceof SignBlock) {
                                BlockState state = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).getState();
                                if (state instanceof Sign) {
                                    Sign s = (Sign) state;
                                    for (int line = 0; line < ((SignBlock) i.getWorldEditBaseBlock()).getText().length; line++) {
                                        s.setLine(line, ((SignBlock) i.getWorldEditBaseBlock()).getText()[line]);
                                    }
                                    ((CraftBlockState) s).update(false, false);
                                }
                            }
                        }
                    }
                }

                // clean up any left over tile entities on blocks that do not need tile entities, also process signs
                for (MapUpdateCommand i : updatesInWorld) {
                    if (i != null) {
                        int blockType;
                        Block srcBlock;
                        BlockPosition dstBlockPos;
                        net.minecraft.server.v1_10_R1.Chunk nativeDstChunk;
                        if (i.getOldBlockLocation() != null) {
                            blockType = w.getBlockTypeIdAt(i.getOldBlockLocation().getX(), i.getOldBlockLocation().getY(), i.getOldBlockLocation().getZ());
                            srcBlock = w.getBlockAt(i.getOldBlockLocation().getX(), i.getOldBlockLocation().getY(), i.getOldBlockLocation().getZ());
                            nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                            dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                            if (Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
                                nativeDstChunk.getTileEntities().remove(dstBlockPos);
                            }

                        }
                        // now remove the tile entities in the new location if they shouldn't be there
                        blockType = w.getBlockTypeIdAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                        srcBlock = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                        nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                        dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                        if (Arrays.binarySearch(tileEntityBlocksToPreserve, blockType) < 0) { // TODO: make this only run when the original block had tile data, but after it cleans up my corrupt chunks >.>
                            nativeDstChunk.getTileEntities().remove(dstBlockPos);
                        }

                        if (i.getTypeID() == 63 || i.getTypeID() == 68) {
                            if (i.getCraft() != null) {
                                srcBlock = w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                                nativeDstChunk = ((CraftChunk) srcBlock.getChunk()).getHandle();
                                dstBlockPos = new BlockPosition(srcBlock.getX(), srcBlock.getY(), srcBlock.getZ());
                                TileEntity tileEntity = nativeDstChunk.getTileEntities().get(dstBlockPos);
                                if (tileEntity instanceof TileEntitySign) {
                                    processSign(tileEntity, i.getCraft());
                                    sendSignToPlayers(w, i);
                                    nativeDstChunk.getTileEntities().put(dstBlockPos, tileEntity);
                                }
                            }
                        }
                    }
                }

                // put in smoke or effects
                for (MapUpdateCommand i : updatesInWorld) {
                    if (i != null) {
                        if (i.getSmoke() == 1) {
                            Location loc = new Location(w, i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                            w.playEffect(loc, Effect.SMOKE, 4);
                        }
                        if (Settings.SilhouetteViewDistance > 0 && silhouetteBlocksSent < Settings.SilhouetteBlockCount) {
                            if (sendSilhouetteToPlayers(w, i))
                                silhouetteBlocksSent++;
                        }
                    }
                }

                // and set all crafts that were updated to not processing
                if (CraftManager.getInstance().getCraftsInWorld(w) != null) {
                    for (MapUpdateCommand c : updatesInWorld) {
                        if (c != null) {
                            Craft craft = c.getCraft();
                            if (craft != null) {
                                if (!craft.isNotProcessing()) {
                                    craft.setProcessing(false);
                                }
                            }

                        }
                    }
                }

/*				// send updates to clients
				for ( MapUpdateCommand c : updatesInWorld ) {
					if(c!=null) {
						Location loc=new Location(w,c.getNewBlockLocation().getX(),c.getNewBlockLocation().getY(),c.getNewBlockLocation().getZ());
						if(c.getTypeID()!=c.getCurrentTypeID() || c.getDataID()!=c.getCurrentDataID())
							w.getBlockAt(loc).getState().update();
					}
				}*/

                // queue chunks for lighting recalc
                if (Settings.CompatibilityMode == false) {
                    for (net.minecraft.server.v1_10_R1.Chunk c : chunksToRelight) {
                        ChunkUpdater fChunk = FastBlockChanger.getInstance().getChunk(c.world, c.locX, c.locZ, true);
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                for (int by = 0; by < 4; by++) {
                                    fChunk.bits[bx][bz][by] = Long.MAX_VALUE;
                                }
                            }
                        }
                        fChunk.last_modified = System.currentTimeMillis();
                        c.e();
                    }
                }

                long endTime = System.currentTimeMillis();
                if (Settings.Debug) {
                    Movecraft.getInstance().getServer().broadcastMessage("Map update took (ms): " + (endTime - startTime));
                }

                //drop harvested yield 
                if (itemDropUpdatesInWorld != null) {
                    for (ItemDropUpdateCommand i : itemDropUpdatesInWorld) {
                        if (i != null) {
                            final World world = w;
                            final Location loc = i.getLocation();
                            final ItemStack stack = i.getItemStack();
                            if (i.getItemStack() instanceof ItemStack) {
                                // drop Item
                                BukkitTask dropTask = new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        world.dropItemNaturally(loc, stack);
                                    }
                                }.runTaskLater(Movecraft.getInstance(), (20 * 1));
                            }
                        }
                    }
                }
            }
        }

        updates.clear();
        entityUpdates.clear();
        itemDropUpdates.clear();
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
            ((CraftBlockState) sign).update(false, false);
        }
    }

    private boolean sendSilhouetteToPlayers(World w, MapUpdateCommand i) {
        boolean sendSil = false;
        if (rand.nextInt(100) < 15) {
            sendSil = true;
        }
/*		if(i.getTypeID()==0)
			sendSil=true;
		else
			if(i.getCurrentTypeID()!=null)
				if(i.getTypeID()!=0 && i.getCurrentTypeID()==0)
					sendSil=true;*/
        if (sendSil) {
            for (Player p : w.getPlayers()) { // this is necessary because signs do not get updated client side correctly without refreshing the chunks, which causes a memory leak in the clients
                int playerX = p.getLocation().getBlockX();
                int playerY = p.getLocation().getBlockY();
                int playerZ = p.getLocation().getBlockZ();
                int dist = (i.getNewBlockLocation().getX() - playerX) * (i.getNewBlockLocation().getX() - playerX);
                dist += (i.getNewBlockLocation().getY() - playerY) * (i.getNewBlockLocation().getY() - playerY);
                dist += (i.getNewBlockLocation().getZ() - playerZ) * (i.getNewBlockLocation().getZ() - playerZ);
                if ((dist < Settings.SilhouetteViewDistance * Settings.SilhouetteViewDistance) && (dist > 32 * 32)) {
                    net.minecraft.server.v1_10_R1.EntityPlayer craftPlayer = ((CraftPlayer) p).getHandle();
                    BlockPosition blockPosition = new BlockPosition(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ());
                    PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.VILLAGER_HAPPY, true, (float) i.getNewBlockLocation().getX(), (float) i.getNewBlockLocation().getY(), (float) i.getNewBlockLocation().getZ(), (float) 1, (float) 1, (float) 1, 0, 9);

                    craftPlayer.playerConnection.sendPacket(packet);
                }
            }
            return true;
        } else {
            return false;
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
                    if (Settings.SetHomeToCrewSign == true)

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
                HashMap<Integer, Integer> foundBlocks = new HashMap<Integer, Integer>();
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
                updateQty += blockUpdatesPerCraft.get(craft);
                TESign.lines[2] = new ChatComponentText(signText);
                TESign.lines[3] = new ChatComponentText(updateQty);
            }
        }
    }

    // NOTE: The below is slow and should NOT be run synchronously if it can be avoided!
    public void sortUpdates(MapUpdateCommand[] mapUpdates) {
        // the point of this is to sort the block updates so that an update never overwrites the source of a later update

/*		boolean sorted=false;
		HashMap<MovecraftLocation,Integer> newBlockLocationIndexes=new HashMap<MovecraftLocation,Integer>();
		Integer index=0;
		for(MapUpdateCommand i : mapUpdates) {
			if(i.getOldBlockLocation()!=null) {
//				if((Arrays.binarySearch(tileEntityBlocksToPreserve,i.getTypeID())>=0) || (Arrays.binarySearch(tileEntityBlocksToPreserve,i.getCurrentTypeID())>=0)) {
					newBlockLocationIndexes.put(i.getNewBlockLocation(), index);
	//			}
		//	} else {
			//	if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getTypeID())>=0)
					newBlockLocationIndexes.put(i.getNewBlockLocation(), index);
			}
			index++;
		}

		int iterations=0;
		while(!sorted && iterations<25) {
			iterations++;
			sorted=true;
			for(index=0; index<mapUpdates.length; index++) {
				MapUpdateCommand i=mapUpdates[index];
				if(i.getOldBlockLocation()!=null) {
					boolean needsSort=false;
					if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getTypeID())>=0) {
						needsSort=true;
					} else {
//						int sourceIndex=newBlockLocationIndexes.get(i.getOldBlockLocation());
						if(Arrays.binarySearch(tileEntityBlocksToPreserve,i.getCurrentTypeID())>=0) {
							needsSort=true;
						}
					}
					if(needsSort) {
						Integer sourceIndex=newBlockLocationIndexes.get(i.getOldBlockLocation());
						if((sourceIndex!=null) && (sourceIndex<index)) {
							sorted=false;
							MapUpdateCommand temp=i;
							mapUpdates[index]=mapUpdates[sourceIndex];
							mapUpdates[sourceIndex]=temp;
							newBlockLocationIndexes.put(i.getOldBlockLocation(), index);
							newBlockLocationIndexes.put(i.getNewBlockLocation(), sourceIndex);
						}
					}
				}
			}
		}
		iterations++; // just to give a convenient breakpoint*/
    }

    public boolean addWorldUpdate(World w, MapUpdateCommand[] mapUpdates, EntityUpdateCommand[] eUpdates, ItemDropUpdateCommand[] iUpdates) {

        if (mapUpdates != null) {
            ArrayList<MapUpdateCommand> get = updates.get(w);
            if (get != null) {
                updates.remove(w);
                ArrayList<MapUpdateCommand> tempUpdates = new ArrayList<MapUpdateCommand>();
                tempUpdates.addAll(Arrays.asList(mapUpdates));
                get.addAll(tempUpdates);
            } else {
                get = new ArrayList<MapUpdateCommand>(Arrays.asList(mapUpdates));
            }
            updates.put(w, get);
        }

        //now do entity updates
        if (eUpdates != null) {
            ArrayList<EntityUpdateCommand> eGet = entityUpdates.get(w);
            if (eGet != null) {
                entityUpdates.remove(w);
                ArrayList<EntityUpdateCommand> tempEUpdates = new ArrayList<EntityUpdateCommand>();
                tempEUpdates.addAll(Arrays.asList(eUpdates));
                eGet.addAll(tempEUpdates);
            } else {
                eGet = new ArrayList<EntityUpdateCommand>(Arrays.asList(eUpdates));
            }
            entityUpdates.put(w, eGet);
        }

        //now do item drop updates
        if (iUpdates != null) {
            ArrayList<ItemDropUpdateCommand> iGet = itemDropUpdates.get(w);
            if (iGet != null) {
                entityUpdates.remove(w);
                ArrayList<ItemDropUpdateCommand> tempIDUpdates = new ArrayList<ItemDropUpdateCommand>();
                tempIDUpdates.addAll(Arrays.asList(iUpdates));
                iGet.addAll(tempIDUpdates);
            } else {
                iGet = new ArrayList<ItemDropUpdateCommand>(Arrays.asList(iUpdates));
            }
            itemDropUpdates.put(w, iGet);
        }

        return false;
    }

    private boolean setContainsConflict(ArrayList<MapUpdateCommand> set, MapUpdateCommand c) {
        for (MapUpdateCommand command : set) {
            if (command != null && c != null)
                if (command.getNewBlockLocation().equals(c.getNewBlockLocation())) {
                    return true;
                }
        }

        return false;
    }

    private boolean arrayContains(int[] oA, int o) {
        for (int testO : oA) {
            if (testO == o) {
                return true;
            }
        }

        return false;
    }

    private void createExplosion(Location loc, float explosionPower) {
        boolean explosionblocked = false;
        if (Movecraft.getInstance().getWorldGuardPlugin() != null) {
            ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(loc.getWorld()).getApplicableRegions(loc);
            if (set.allows(DefaultFlag.OTHER_EXPLOSION) == false) {
                explosionblocked = true;
            }
        }
        if (!explosionblocked)
            loc.getWorld().createExplosion(loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5, explosionPower);
        return;
    }

    private static class MapUpdateManagerHolder {
        private static final MapUpdateManager INSTANCE = new MapUpdateManager();
    }

}
