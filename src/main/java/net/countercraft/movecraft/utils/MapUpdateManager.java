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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.items.StorageChestItem;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.datastructures.InventoryTransferHolder;
import net.countercraft.movecraft.utils.datastructures.SignTransferHolder;
import net.countercraft.movecraft.utils.datastructures.CommandBlockTransferHolder;
import net.countercraft.movecraft.utils.datastructures.StorageCrateTransferHolder;
import net.countercraft.movecraft.utils.datastructures.TransferData;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R1.EnumSkyBlock;
import net.minecraft.server.v1_8_R1.IBlockData;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class MapUpdateManager extends BukkitRunnable {
	private final HashMap<World, ArrayList<MapUpdateCommand>> updates = new HashMap<World, ArrayList<MapUpdateCommand>>();
	private final HashMap<World, ArrayList<EntityUpdateCommand>> entityUpdates = new HashMap<World, ArrayList<EntityUpdateCommand>>();
        private final HashMap<World, ArrayList<ItemDropUpdateCommand>> itemDropUpdates = new HashMap<World, ArrayList<ItemDropUpdateCommand>>();
	private HashSet<Location> protectedBlocks = new HashSet<Location>();
	
	private MapUpdateManager() {
	}

	public static MapUpdateManager getInstance() {
		return MapUpdateManagerHolder.INSTANCE;
	}

	private static class MapUpdateManagerHolder {
		private static final MapUpdateManager INSTANCE = new MapUpdateManager();
	}
	
	private void updateBlock(MapUpdateCommand m, World w, Map<MovecraftLocation, TransferData> dataMap, Set<net.minecraft.server.v1_8_R1.Chunk> chunks, Set<Chunk> cmChunks, HashMap<MovecraftLocation, Byte> origLightMap, boolean placeDispensers) {
		MovecraftLocation workingL = m.getNewBlockLocation();
		final int[] blocksToBlankOut = new int[]{ 54, 61, 62, 63, 68, 116, 117, 146, 149, 150, 154, 158, 145 };		

		int x = workingL.getX();
		int y = workingL.getY();
		int z = workingL.getZ();
		Chunk chunk=null;

		int newTypeID = m.getTypeID();

		if(newTypeID==152 && !placeDispensers) {
			return;
		}
			
		
		chunk = w.getBlockAt( x, y, z ).getChunk();

		net.minecraft.server.v1_8_R1.Chunk c = null;
		Chunk cmC = null;
		if(Settings.CompatibilityMode) {
			cmC = chunk;
		} else {
			c = ( ( CraftChunk ) chunk ).getHandle();
		}

		TransferData transferData = dataMap.get( workingL );

		byte data;
		if ( transferData != null ) {
			data = transferData.getData();
		} else {
			data = 0;
		}
		
		if(newTypeID==23 && !placeDispensers) {
			newTypeID=44;
			data=8;
		}
		
		int origType=w.getBlockAt( x, y, z ).getTypeId();
		byte origData=w.getBlockAt( x, y, z ).getData();
		boolean success = false;

		if(Settings.CompatibilityMode) { 

			if(origType!=newTypeID || origData!=data) {
				boolean doBlankOut=(Arrays.binarySearch(blocksToBlankOut,newTypeID)>=0);
				if(doBlankOut) {
					w.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR);
				}
				
				if(origType==149 || origType==150) { // necessary because bukkit does not handle comparators correctly. This code does not prevent console spam, but it does prevent chunk corruption
					w.getBlockAt(x, y, z).setType(org.bukkit.Material.SIGN_POST);
					BlockState state=w.getBlockAt( x, y, z ).getState();
					Sign s=(Sign)state;
					s.setLine(0, "PLACEHOLDER");
					s.update();
					w.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR);
					}
				if(newTypeID==149 || newTypeID==150) {
					if(newTypeID==149)
						w.getBlockAt(x, y, z).setType(org.bukkit.Material.REDSTONE_COMPARATOR_OFF);
					else
						w.getBlockAt(x, y, z).setType(org.bukkit.Material.REDSTONE_COMPARATOR_ON);
				} else {
					w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
				}
			}
			if ( !cmChunks.contains( cmC ) ) {
				cmChunks.add( cmC );
			}
		} else {
			BlockPosition position = new BlockPosition(x, y, z);
			if(origType==149 || origType==150) { // bukkit can't remove comparators safely, it screws up the NBT data. So turn it to a sign, then remove it.

				c.a( position, CraftMagicNumbers.getBlock(org.bukkit.Material.AIR).fromLegacyData(0));
				c.a( position, CraftMagicNumbers.getBlock(org.bukkit.Material.SIGN_POST).fromLegacyData(0));
				
				BlockState state=w.getBlockAt( x, y, z ).getState();
				Sign s=(Sign)state;
				s.setLine(0, "PLACEHOLDER");
				s.update();
				c.a(position, CraftMagicNumbers.getBlock(org.bukkit.Material.SIGN_POST).fromLegacyData(0));
				success = c.a( position, CraftMagicNumbers.getBlock(newTypeID).fromLegacyData(data) ) != null;
				if ( !success ) {
					w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
				}
				if ( !chunks.contains( c ) ) {
					chunks.add( c );
				}
			} else {
				if(origType==50 || origType==89 || origType==124) {
					// if removing a light source, remove lighting from nearby terrain to avoid light pollution
					int centerX=x;
					int centerY=y;
					int centerZ=z;
					for(int posx=centerX-14;posx<=centerX+14;posx++) {
						for(int posy=centerY-14;posy<=centerY+14;posy++) {
							if(posy>0 && posy<=255)
								for(int posz=centerZ-14;posz<=centerZ+14;posz++) {
									int linearDist=Math.abs(posx-centerX);
									linearDist+=Math.abs(posy-centerY);
									linearDist+=Math.abs(posz-centerZ);
									if(linearDist<=15) {
//										((CraftWorld) w).getHandle().b(EnumSkyBlock.BLOCK, x, y, z, lightLevel); Changed for 1.8, and quite possibly wrong:
										BlockPosition positioni = new BlockPosition(posx, posy, posz);
										((CraftWorld) w).getHandle().b(EnumSkyBlock.BLOCK, positioni);
									}
								}
						}
					}
				}
		
				if(origType!=newTypeID || origData!=data) {
					success = c.a( position, CraftMagicNumbers.getBlock(newTypeID).fromLegacyData(data) ) != null;
				} else {
					success=true;
				}
				if ( !success ) {
					w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
				}
				// set light level to whatever it was before the move
				if(m.getOldBlockLocation()!=null) {
//					((CraftWorld) w).getHandle().b(EnumSkyBlock.BLOCK, x, y, z, lightLevel); Changed for 1.8, and quite possibly wrong:
					((CraftWorld) w).getHandle().b(EnumSkyBlock.BLOCK, position);
				}
				if ( !chunks.contains( c ) ) {
					chunks.add( c );
				}
			}
		}						

	}

	public void run() {
		if ( updates.isEmpty() ) return;

		long startTime=System.currentTimeMillis();

		final int[] fragileBlocks = new int[]{ 26, 29, 33, 34, 50, 52, 54, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, 404 };
		Arrays.sort(fragileBlocks);
				
		for ( World w : updates.keySet() ) {
			if ( w != null ) {
				List<MapUpdateCommand> updatesInWorld = updates.get( w );
				List<EntityUpdateCommand> entityUpdatesInWorld = entityUpdates.get( w );
                                List<ItemDropUpdateCommand> itemDropUpdatesInWorld = itemDropUpdates.get( w );
				Map<MovecraftLocation, List<EntityUpdateCommand>> entityMap = new HashMap<MovecraftLocation, List<EntityUpdateCommand>>();
                                Map<MovecraftLocation, List<ItemDropUpdateCommand>> itemMap = new HashMap<MovecraftLocation, List<ItemDropUpdateCommand>>();
				Map<MovecraftLocation, TransferData> dataMap = new HashMap<MovecraftLocation, TransferData>();
				HashMap<MovecraftLocation, Byte> origLightMap = new HashMap<MovecraftLocation, Byte>();
				Set<net.minecraft.server.v1_8_R1.Chunk> chunks = null; 
				Set<Chunk> cmChunks = null;
				if(Settings.CompatibilityMode) {
					cmChunks = new HashSet<Chunk>();					
				} else {
					chunks = new HashSet<net.minecraft.server.v1_8_R1.Chunk>();
				}
//unused			ArrayList<Player> unupdatedPlayers=new ArrayList<Player>(Movecraft.getInstance().getServer().getOnlinePlayers());

//				ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
//unused			boolean isFirstChunk=true;
				
				// set all old and new blocks that are fragile to be protected
/*				for ( MapUpdateCommand c : updatesInWorld ) {
					if(c!=null) {
						boolean isFragile=(Arrays.binarySearch(fragileBlocks,c.getTypeID())>=0);
						if(isFragile) {
							if(c.getNewBlockLocation()!=null) {
								Location loc=new Location(w,c.getNewBlockLocation().getX(),c.getNewBlockLocation().getY(),c.getNewBlockLocation().getZ());
								if(!protectedBlocks.contains(loc))
									addProtectedBlock(loc);
							}
							if(c.getOldBlockLocation()!=null) {
								Location loc=new Location(w,c.getOldBlockLocation().getX(),c.getOldBlockLocation().getY(),c.getOldBlockLocation().getZ());
								if(!protectedBlocks.contains(loc))
									addProtectedBlock(loc);
							}
						}
					}
				}*/
                                
                                
				// Preprocessing
				for ( MapUpdateCommand c : updatesInWorld ) {
					MovecraftLocation l;
					if(c!=null)
						l = c.getOldBlockLocation();
					else 
						l = null;
					
					if ( l != null ) {
						// keep track of the light levels that were present before moving the craft
						origLightMap.put(l, w.getBlockAt(l.getX(), l.getY(), l.getZ()).getLightLevel());
						
						// keep track of block data for later reconstruction
						TransferData blockDataPacket = getBlockDataPacket( w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState(), c.getRotation() );
						if ( blockDataPacket != null ) {
							dataMap.put( c.getNewBlockLocation(), blockDataPacket );
						}
						
						//remove dispensers and replace them with half slabs to prevent them firing during reconstruction
						if(w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getTypeId()==23) {
							MapUpdateCommand blankCommand=new MapUpdateCommand(c.getOldBlockLocation(), 23, c.getCraft());
							updateBlock(blankCommand, w, dataMap, chunks, cmChunks, origLightMap, false);
						}
						//remove redstone blocks and replace them with stone to prevent redstone activation during reconstruction
						if(w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getTypeId()==152) {
							MapUpdateCommand blankCommand=new MapUpdateCommand(c.getOldBlockLocation(), 1, c.getCraft());
							updateBlock(blankCommand, w, dataMap, chunks, cmChunks, origLightMap, false);
						}
					}					
				}
                                
				// track the blocks that entities will be standing on to move them smoothly with the craft
				if(entityUpdatesInWorld!=null) {
					for( EntityUpdateCommand i : entityUpdatesInWorld) {
						if(i!=null) {
							MovecraftLocation entityLoc=new MovecraftLocation(i.getNewLocation().getBlockX(), i.getNewLocation().getBlockY()-1, i.getNewLocation().getBlockZ());
							if(!entityMap.containsKey(entityLoc)) {
								List<EntityUpdateCommand> entUpdateList=new ArrayList<EntityUpdateCommand>();
								entUpdateList.add(i);
								entityMap.put(entityLoc, entUpdateList);
							} else {
								List<EntityUpdateCommand> entUpdateList=entityMap.get(entityLoc);
								entUpdateList.add(i);
							}
							if(i.getEntity() instanceof Player) {
								// send a fake glass block so the player doesn't fall
								Player p=(Player)i.getEntity();
								Location loc=i.getNewLocation().clone();
								loc=loc.subtract(0, 1, 0);
								p.sendBlockChange(loc, 20, (byte) 0);
								Location loc2=i.getNewLocation().clone();
								loc2=loc2.add(0, 1, 0);
								p.sendBlockChange(loc2, 0, (byte) 0);
								
								final Block b1=w.getBlockAt(loc);
								final Block b2=w.getBlockAt(loc2);
								BukkitTask restoreblockTask = new BukkitRunnable() {
									@Override
									public void run() {
										b1.getState().update();
										b2.getState().update();
									}
								}.runTaskLater( Movecraft.getInstance(), ( 20 * 1 ) );
							}
						}
					}
				}
				
				// Place any blocks that replace "fragiles", other than other fragiles
				for ( MapUpdateCommand i : updatesInWorld ) {
					if(i!=null) {
						if(i.getTypeID()>=0) {
							int prevType=w.getBlockAt(i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ()).getTypeId();
							boolean prevIsFragile=(Arrays.binarySearch(fragileBlocks,prevType)>=0);
							boolean isFragile=(Arrays.binarySearch(fragileBlocks,i.getTypeID())>=0);
							if(prevIsFragile && (!isFragile)) {
								updateBlock(i, w, dataMap, chunks, cmChunks, origLightMap, false);
							}
							if(prevIsFragile && isFragile) {
								MapUpdateCommand blankCommand=new MapUpdateCommand(i.getNewBlockLocation(), 0, i.getCraft());
								updateBlock(blankCommand, w, dataMap, chunks, cmChunks, origLightMap, false);
							}
						}
					}
				}
				
				// Perform core block updates, don't do "fragiles" yet. Don't do Dispensers yet either
				for ( MapUpdateCommand m : updatesInWorld ) {
					if(m!=null) {
						boolean isFragile=(Arrays.binarySearch(fragileBlocks,m.getTypeID())>=0);
						
						if(!isFragile) {
							// a TypeID less than 0 indicates an explosion
							if(m.getTypeID()<0) {
								if(m.getTypeID()<-10) { // don't bother with tiny explosions
									float explosionPower=m.getTypeID();
									explosionPower=0.0F-explosionPower/100.0F;
									w.createExplosion(m.getNewBlockLocation().getX()+0.5, m.getNewBlockLocation().getY()+0.5, m.getNewBlockLocation().getZ()+0.5, explosionPower);
								}
							} else {
								updateBlock(m, w, dataMap, chunks, cmChunks, origLightMap, false);
							}
						}
						
						// if the block you just updated had any entities on it, move them. If they are moving, add in their motion to the craft motion
						if( entityMap.containsKey(m.getNewBlockLocation()) ) {
							List<EntityUpdateCommand> mapUpdateList=entityMap.get(m.getNewBlockLocation());
							for(EntityUpdateCommand entityUpdate : mapUpdateList) {
								Entity entity=entityUpdate.getEntity();
/*								if(entity instanceof Player) {
									Player p=(Player)entity;
									int dx=entityUpdate.getNewLocation().getBlockX()-entityUpdate.getOldLocation().getBlockX();
									int dy=entityUpdate.getNewLocation().getBlockY()-entityUpdate.getOldLocation().getBlockY();
									int dz=entityUpdate.getNewLocation().getBlockZ()-entityUpdate.getOldLocation().getBlockZ();
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tp "+p.getName()+" ~"+dx+" ~"+dy+" ~"+dz);
								}
*/
//								Vector pVel=new Vector(entity.getVelocity().getX(),0.0,entity.getVelocity().getZ());
								entity.teleport(entityUpdate.getNewLocation());
//								entity.setVelocity(pVel);
							}
							entityMap.remove(m.getNewBlockLocation());
						}
					}
	
				}

				// Fix redstone and other "fragiles"				
				for ( MapUpdateCommand i : updatesInWorld ) {
					if(i!=null) {
						boolean isFragile=(Arrays.binarySearch(fragileBlocks,i.getTypeID())>=0);
						if(isFragile) {
							updateBlock(i, w, dataMap, chunks, cmChunks, origLightMap, false);
						}
					}
				}

				for ( MapUpdateCommand i : updatesInWorld ) {
					if(i!=null) {
						// Put Dispensers back in now that the ship is reconstructed
						if(i.getTypeID()==23 || i.getTypeID()==152) {
							updateBlock(i, w, dataMap, chunks, cmChunks, origLightMap, true);					
						}
						
					}
				}

				// put in smoke or effects
				for ( MapUpdateCommand i : updatesInWorld ) {
					if(i!=null) {
						if(i.getSmoke()==1) {
							Location loc=new Location(w, i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(),  i.getNewBlockLocation().getZ());
							w.playEffect(loc, Effect.SMOKE, 4);
						}
					}
				}
				
				// Restore block specific information
				for ( MovecraftLocation l : dataMap.keySet() ) {
					try {
						TransferData transferData = dataMap.get( l );

						if ( transferData instanceof SignTransferHolder ) {

							SignTransferHolder signData = ( SignTransferHolder ) transferData;
							Sign sign = ( Sign ) w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState();
							for ( int i = 0; i < signData.getLines().length; i++ ) {
								sign.setLine( i, signData.getLines()[i] );
							}
							for(Player p : w.getPlayers()) { // this is necessary because signs do not get updated client side correctly without refreshing the chunks, which causes a memory leak in the clients							
								int playerChunkX=p.getLocation().getBlockX()>>4;
								int playerChunkZ=p.getLocation().getBlockZ()>>4;
								if(Math.abs(playerChunkX-sign.getChunk().getX())<Bukkit.getServer().getViewDistance())
									if(Math.abs(playerChunkZ-sign.getChunk().getZ())<Bukkit.getServer().getViewDistance()) {
										p.sendBlockChange(sign.getLocation(), 63, (byte) 0);
//										p.sendSignChange(sign.getLocation(), sign.getLines());
										p.sendBlockChange(sign.getLocation(), sign.getTypeId(), sign.getRawData());
//										p.sendSignChange(sign.getLocation(), sign.getLines());
										
									}
							}
							sign.update( true, false );

						} else if ( transferData instanceof StorageCrateTransferHolder ) {
							Inventory inventory = Bukkit.createInventory( null, 27, String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) );
							inventory.setContents( ( ( StorageCrateTransferHolder ) transferData ).getInvetory() );
							StorageChestItem.setInventoryOfCrateAtLocation( inventory, l, w );

						} else if ( transferData instanceof InventoryTransferHolder ) {
							InventoryTransferHolder invData = ( InventoryTransferHolder ) transferData;
							InventoryHolder inventoryHolder = ( InventoryHolder ) w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState();
							inventoryHolder.getInventory().setContents( invData.getInvetory() );
						} else if ( transferData instanceof CommandBlockTransferHolder) {
							CommandBlockTransferHolder cbData=(CommandBlockTransferHolder) transferData;
							CommandBlock cblock=(CommandBlock) w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState();
							cblock.setCommand(cbData.getText());
							cblock.setName(cbData.getName());
							cblock.update();
						}
						w.getBlockAt( l.getX(), l.getY(), l.getZ() ).setData( transferData.getData() );
					} catch ( IndexOutOfBoundsException e ) {
						Movecraft.getInstance().getLogger().log( Level.SEVERE, "Severe error in map updater" );
					} catch (IllegalArgumentException e) {
                                            Movecraft.getInstance().getLogger().log( Level.SEVERE, "Severe error in map updater" );
                    }

				}
				
				if(Settings.CompatibilityMode) {
					for ( Chunk c : cmChunks ) {
//						w.refreshChunk(c.getX(), c.getZ());  for some reason uncommenting this leads to a memory leak on the CLIENT, although I'm pretty sure it shouldn't
					}
					
				} else {
					for ( net.minecraft.server.v1_8_R1.Chunk c : chunks ) {
//						c.initLighting();
						ChunkCoordIntPair ccip = new ChunkCoordIntPair( c.locX, c.locZ ); // changed from c.x to c.locX and c.locZ

						for ( Player p : w.getPlayers() ) {
							List<ChunkCoordIntPair> chunkCoordIntPairQueue = ( List<ChunkCoordIntPair> ) ( ( CraftPlayer ) p ).getHandle().chunkCoordIntPairQueue;
							int playerChunkX=p.getLocation().getBlockX()>>4;
							int playerChunkZ=p.getLocation().getBlockZ()>>4;
							
							// only send the chunk if the player is near enough to see it and it's not still in the queue, but always send the chunk if the player is standing in it
							if(playerChunkX==c.locX && playerChunkZ==c.locZ) {
								chunkCoordIntPairQueue.add( 0, ccip );
							} else {
								if(Math.abs(playerChunkX-c.locX)<Bukkit.getServer().getViewDistance())
									if(Math.abs(playerChunkZ-c.locZ)<Bukkit.getServer().getViewDistance())
										if ( !chunkCoordIntPairQueue.contains( ccip ) )
											chunkCoordIntPairQueue.add( ccip );
							}
						}
					}
				}

				// set all old and new blocks to no longer be protected
/*				for ( MapUpdateCommand c : updatesInWorld ) {
					if(c!=null) {
						boolean isFragile=(Arrays.binarySearch(fragileBlocks,c.getTypeID())>=0);
						if(isFragile) {
							if(c.getNewBlockLocation()!=null) {
								Location loc=new Location(w,c.getNewBlockLocation().getX(),c.getNewBlockLocation().getY(),c.getNewBlockLocation().getZ());
								if(protectedBlocks.contains(loc))
									removeProtectedBlock(loc);
							}
							if(c.getOldBlockLocation()!=null) {
								Location loc=new Location(w,c.getOldBlockLocation().getX(),c.getOldBlockLocation().getY(),c.getOldBlockLocation().getZ());
								if(protectedBlocks.contains(loc))
									removeProtectedBlock(loc);
							}
						}
					}
				}*/
                                
                                //drop harvested yield 
                                if(itemDropUpdatesInWorld!=null) {
					for( ItemDropUpdateCommand i : itemDropUpdatesInWorld) {
						if(i!=null) {                                                        
                                                        final World world = w;
                                                        final Location loc = i.getLocation();
                                                        final ItemStack stack = i.getItemStack();
							if(i.getItemStack() instanceof ItemStack) {
								// drop Item
								BukkitTask dropTask = new BukkitRunnable() {
									@Override
									public void run() {
                                                                            world.dropItemNaturally(loc, stack);
									}
								}.runTaskLater( Movecraft.getInstance(), ( 20 * 1 ) );
							}
						}
					}
				}
				
				if(CraftManager.getInstance().getCraftsInWorld(w)!=null) {
					
					//move entities again to reduce falling out of crafts
					/*if(entityUpdatesInWorld!=null) {
						for(EntityUpdateCommand entityUpdate : entityUpdatesInWorld) {
							if(entityUpdate!=null) {
								Entity entity=entityUpdate.getEntity();
								Vector pVel=new Vector(entity.getVelocity().getX(),0.0,entity.getVelocity().getZ());

								entity.teleport(entityUpdate.getNewLocation());
								entity.setVelocity(pVel);
							}
						}
					}*/

					// and set all crafts that were updated to not processing
					for ( MapUpdateCommand c : updatesInWorld ) {
						if(c!=null) {
							Craft craft=c.getCraft();
							if(craft!=null) {
								if(!craft.isNotProcessing()) {
									craft.setProcessing(false);
								}
							}

						}						
					}
				}
				
			}
		}

		
		updates.clear();
		entityUpdates.clear();
                itemDropUpdates.clear();
		long endTime=System.currentTimeMillis();
//		Movecraft.getInstance().getServer().broadcastMessage("Map update took (ms): "+(endTime-startTime));
	}
        
        public boolean addWorldUpdate( World w, MapUpdateCommand[] mapUpdates, EntityUpdateCommand[] eUpdates, ItemDropUpdateCommand[] iUpdates) {
		ArrayList<MapUpdateCommand> get = updates.get( w );
		if ( get != null ) {
			updates.remove( w );
		} else {
			get = new ArrayList<MapUpdateCommand>();
		}

		ArrayList<MapUpdateCommand> tempSet = new ArrayList<MapUpdateCommand>();
		if(mapUpdates!=null) {
			for ( MapUpdateCommand m : mapUpdates ) {
	
				if ( setContainsConflict( get, m ) ) {
					return true;
				} else {
					tempSet.add( m );
				}
	
			}
		}
		get.addAll( tempSet );
		updates.put( w, get );

		//now do entity updates
		if(eUpdates!=null) {
			ArrayList<EntityUpdateCommand> eGet = entityUpdates.get( w );
			if ( eGet != null ) {
				entityUpdates.remove( w ); 
			} else {
				eGet = new ArrayList<EntityUpdateCommand>();
			}
			
			ArrayList<EntityUpdateCommand> tempEUpdates = new ArrayList<EntityUpdateCommand>();
                        tempEUpdates.addAll(Arrays.asList(eUpdates));
			eGet.addAll( tempEUpdates );
			entityUpdates.put(w, eGet);
		}
                
                //now do item drop updates
		if(iUpdates!=null) {
			ArrayList<ItemDropUpdateCommand> iGet = itemDropUpdates.get( w );
			if ( iGet != null ) {
				entityUpdates.remove( w ); 
			} else {
				iGet = new ArrayList<ItemDropUpdateCommand>();
			}
			
			ArrayList<ItemDropUpdateCommand> tempIDUpdates = new ArrayList<ItemDropUpdateCommand>();
                        tempIDUpdates.addAll(Arrays.asList(iUpdates));
			iGet.addAll( tempIDUpdates );
			itemDropUpdates.put(w, iGet);
		}
                
		return false;
	}
	
	public HashSet<Location> getProtectedBlocks() {
		return protectedBlocks;
	}

	public void addProtectedBlock(Location loc) {
		protectedBlocks.add(loc);
	}

	public void removeProtectedBlock(Location loc) {
		protectedBlocks.remove(loc);
	}

	private boolean setContainsConflict( ArrayList<MapUpdateCommand> set, MapUpdateCommand c ) {
		for ( MapUpdateCommand command : set ) {
			if( command!=null && c!=null)
				if ( command.getNewBlockLocation().equals( c.getNewBlockLocation() ) ) {
					return true;
				}
		}

		return false;
	}

	private boolean arrayContains( int[] oA, int o ) {
		for ( int testO : oA ) {
			if ( testO == o ) {
				return true;
			}
		}

		return false;
	}

	private TransferData getBlockDataPacket( BlockState s, Rotation r ) {
		if ( BlockUtils.blockHasNoData( s.getTypeId() ) ) {
			return null;
		}

		byte data = s.getRawData();

		if ( BlockUtils.blockRequiresRotation( s.getTypeId() ) && r != Rotation.NONE ) {
			data = BlockUtils.rotate( data, s.getTypeId(), r );
		}

		switch ( s.getTypeId() ) {
			case 23:
			case 54:
			case 61:
			case 62:
			case 117:
			case 146:
			case 158:
			case 154:
				// Data and Inventory
				if(( ( InventoryHolder ) s ).getInventory().getSize()==54) {
					Movecraft.getInstance().getLogger().log( Level.SEVERE, "ERROR: Double chest detected. This is not supported." );
					throw new IllegalArgumentException("INVALID BLOCK");
				}
				ItemStack[] contents = ( ( InventoryHolder ) s ).getInventory().getContents().clone();
				( ( InventoryHolder ) s ).getInventory().clear();
				return new InventoryTransferHolder( data, contents );

			case 68:
			case 63:
				// Data and sign lines
				return new SignTransferHolder( data, ( ( Sign ) s ).getLines() );

			case 33:
				MovecraftLocation l = MathUtils.bukkit2MovecraftLoc( s.getLocation() );
				Inventory i = StorageChestItem.getInventoryOfCrateAtLocation( l, s.getWorld() );
				if ( i != null ) {
					StorageChestItem.removeInventoryAtLocation( s.getWorld(), l );
					return new StorageCrateTransferHolder( data, i.getContents() );
				} else {
					return new TransferData( data );
				}
				
			case 137:
				CommandBlock cblock=(CommandBlock)s;
				return new CommandBlockTransferHolder( data, cblock.getCommand(), cblock.getName());

			default:
				return new TransferData( data );

		}
	}

}
