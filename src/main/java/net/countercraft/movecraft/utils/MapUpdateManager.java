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
import net.countercraft.movecraft.utils.datastructures.StorageCrateTransferHolder;
import net.countercraft.movecraft.utils.datastructures.TransferData;
import net.minecraft.server.v1_7_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_7_R2.Material;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_7_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class MapUpdateManager extends BukkitRunnable {
	private final HashMap<World, ArrayList<MapUpdateCommand>> updates = new HashMap<World, ArrayList<MapUpdateCommand>>();
	private final HashMap<World, ArrayList<EntityUpdateCommand>> entityUpdates = new HashMap<World, ArrayList<EntityUpdateCommand>>();
	
	private MapUpdateManager() {
	}

	public static MapUpdateManager getInstance() {
		return MapUpdateManagerHolder.INSTANCE;
	}

	private static class MapUpdateManagerHolder {
		private static final MapUpdateManager INSTANCE = new MapUpdateManager();
	}
	
	private void updateBlock(MapUpdateCommand m, ArrayList<Chunk> chunkList, World w, Map<MovecraftLocation, TransferData> dataMap, Set<net.minecraft.server.v1_7_R2.Chunk> chunks, Set<Chunk> cmChunks, boolean placeDispensers) {
		MovecraftLocation workingL = m.getNewBlockLocation();

		int x = workingL.getX();
		int y = workingL.getY();
		int z = workingL.getZ();
		Chunk chunk=null;
	
		// Calculate chunk if necessary, check list of chunks already loaded first
	
		boolean foundChunk=false;
		for (Chunk testChunk : chunkList) {
			int sx=x>>4;
			int sz=z>>4;
			if((testChunk.getX()==sx)&&(testChunk.getZ()==sz)) {
				foundChunk=true;
				chunk=testChunk;
			}
		}
		if(!foundChunk) {
			chunk = w.getBlockAt( x, y, z ).getChunk();
			chunkList.add(chunk);							
		}

		net.minecraft.server.v1_7_R2.Chunk c = null;
		Chunk cmC = null;
		if(Settings.CompatibilityMode) {
			cmC = chunk;
		} else {
			c = ( ( CraftChunk ) chunk ).getHandle();
		}

		//get the inner-chunk index of the block to change
		//modify the block in the chunk

		int newTypeID = m.getTypeID();
		if(newTypeID==23 && !placeDispensers) {
			newTypeID=1;
		}
		TransferData transferData = dataMap.get( workingL );

		byte data;
		if ( transferData != null ) {
			data = transferData.getData();
		} else {
			data = 0;
		}

		int origType=w.getBlockAt( x, y, z ).getTypeId();
		byte origData=w.getBlockAt( x, y, z ).getData();
		boolean success = false;

		//don't blank out block if it's already air, or if blocktype will not be changed
		if(Settings.CompatibilityMode) {  
			if((origType!=0)&&(origType!=newTypeID)) {
				w.getBlockAt( x, y, z ).setTypeIdAndData( 0, (byte) 0, false );
			}
			if(origType!=newTypeID || origData!=data) {
				w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
			}
			if ( !cmChunks.contains( cmC ) ) {
				cmChunks.add( cmC );
			}
		} else {
			if(newTypeID==149 || newTypeID==150) { // have to use slower calls for comparators for some reason
				w.getBlockAt( x, y, z ).setTypeIdAndData( 0, (byte) 0, false );
				w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
			} else {
				if((origType!=0)&&(origType!=newTypeID)) {
					c.a( x & 15, y, z & 15, CraftMagicNumbers.getBlock(0), 0 );
				} 
				if(origType!=newTypeID || origData!=data) {
					success = c.a( x & 15, y, z & 15, CraftMagicNumbers.getBlock(newTypeID), data );
				} else {
					success=true;
				}
				if ( !success ) {
					w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
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
		for ( World w : updates.keySet() ) {
			if ( w != null ) {
				List<MapUpdateCommand> updatesInWorld = updates.get( w );
				List<EntityUpdateCommand> entityUpdatesInWorld = entityUpdates.get( w );
				Map<MovecraftLocation, List<EntityUpdateCommand>> entityMap = new HashMap<MovecraftLocation, List<EntityUpdateCommand>>();
				Map<MovecraftLocation, TransferData> dataMap = new HashMap<MovecraftLocation, TransferData>();
				Set<net.minecraft.server.v1_7_R2.Chunk> chunks = null; 
				Set<Chunk> cmChunks = null;
				if(Settings.CompatibilityMode) {
					cmChunks = new HashSet<Chunk>();					
				} else {
					chunks = new HashSet<net.minecraft.server.v1_7_R2.Chunk>();
				}
				ArrayList<Player> unupdatedPlayers=new ArrayList<Player>(Arrays.asList(Movecraft.getInstance().getServer().getOnlinePlayers()));

				// Preprocessing
				for ( MapUpdateCommand c : updatesInWorld ) {
					MovecraftLocation l;
					if(c!=null)
						l = c.getOldBlockLocation();
					else 
						l = null;

					if ( l != null ) {
						TransferData blockDataPacket = getBlockDataPacket( w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState(), c.getRotation() );
						if ( blockDataPacket != null ) {
							dataMap.put( c.getNewBlockLocation(), blockDataPacket );
						}
						
						//remove dispensers and replace them with stone blocks to prevent firing during ship reconstruction
						if(w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getTypeId()==23) {
							w.getBlockAt( l.getX(), l.getY(), l.getZ() ).setTypeIdAndData( 1, (byte) 0, false );
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
						}
					}
				}
				
				ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
				boolean isFirstChunk=true;
				
				final int[] fragileBlocks = new int[]{ 26, 29, 33, 34, 50, 52, 54, 55, 63, 64, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, 404 };
				Arrays.sort(fragileBlocks);
						
			/*	// place blocks that will have entities standing on them, and the 2 blocks above it
				for ( MapUpdateCommand m : updatesInWorld ) {
					if(m!=null) {
						MovecraftLocation oneDown=new MovecraftLocation(m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY()-1, m.getNewBlockLocation().getZ());
						MovecraftLocation twoDown=new MovecraftLocation(m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY()-2, m.getNewBlockLocation().getZ());
						if( entityMap.containsKey(m.getNewBlockLocation()) || entityMap.containsKey(oneDown) || entityMap.containsKey(twoDown)) {
							w.getBlockAt( m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY(), m.getNewBlockLocation().getZ() ).setTypeIdAndData( 0, (byte) 0, false );
							w.getBlockAt( m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY(), m.getNewBlockLocation().getZ() ).setTypeIdAndData( 20, (byte) 0, false );
							w.getBlockAt( m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY()+1, m.getNewBlockLocation().getZ() ).setTypeIdAndData( 0, (byte) 0, false );
							w.getBlockAt( m.getNewBlockLocation().getX(), m.getNewBlockLocation().getY()+2, m.getNewBlockLocation().getZ() ).setTypeIdAndData( 0, (byte) 0, false );
							}
					}
				}*/
				
				// Perform core block updates, don't do "fragiles" yet. Don't do Dispensers yet either
				for ( MapUpdateCommand m : updatesInWorld ) {
					if(m!=null) {
						boolean isFragile=(Arrays.binarySearch(fragileBlocks,m.getTypeID())>=0);
						
						if(!isFragile) {
							// a TypeID less than 0 indicates an explosion
							if(m.getTypeID()<0) {
								float explosionPower=m.getTypeID();
								explosionPower=0.0F-explosionPower/100.0F;
								w.createExplosion(m.getNewBlockLocation().getX()+0.5, m.getNewBlockLocation().getY()+0.5, m.getNewBlockLocation().getZ()+0.5, explosionPower);
							} else {
								updateBlock(m, chunkList, w, dataMap, chunks, cmChunks, false);
							}
						}
						
						// if the block you just updated had any entities on it, move them. If they are moving, add in their motion to the craft motion
						if( entityMap.containsKey(m.getNewBlockLocation()) ) {
							List<EntityUpdateCommand> mapUpdateList=entityMap.get(m.getNewBlockLocation());
							for(EntityUpdateCommand entityUpdate : mapUpdateList) {
								Entity entity=entityUpdate.getEntity();
								Vector pVel=new Vector(entity.getVelocity().getX(),0.0,entity.getVelocity().getZ());
							/*	Location newLoc=entity.getLocation();
								newLoc.setX(entityUpdate.getNewLocation().getX());
								newLoc.setY(entityUpdate.getNewLocation().getY());
								newLoc.setZ(entityUpdate.getNewLocation().getZ());*/
								entity.teleport(entityUpdate.getNewLocation());
								entity.setVelocity(pVel);
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
							updateBlock(i, chunkList, w, dataMap, chunks, cmChunks, false);
							

						}
					}
				}

				for ( MapUpdateCommand i : updatesInWorld ) {
					if(i!=null) {
						// Put Dispensers back in now that the ship is reconstructed
						if(i.getTypeID()==23) {
							updateBlock(i, chunkList, w, dataMap, chunks, cmChunks, true);					
						}
						
						// if a bed was moved, check to see if any spawn points need to be updated
						if(i.getTypeID()==26) {
							Iterator<Player> iter=unupdatedPlayers.iterator();
							while (iter.hasNext()) {
								Player p=iter.next();
							
								if(p!=null) {
									if(p.getBedSpawnLocation()!=null) {
										MovecraftLocation spawnLoc=MathUtils.bukkit2MovecraftLoc( p.getBedSpawnLocation() );
										
										// is the spawn point within 1 block of where the bed used to be?
										boolean foundSpawn=false;
										if(i.getOldBlockLocation().getX()-spawnLoc.getX()<=1 && i.getOldBlockLocation().getX()-spawnLoc.getX()>=-1) {
											if(i.getOldBlockLocation().getZ()-spawnLoc.getZ()<=1 && i.getOldBlockLocation().getZ()-spawnLoc.getZ()>=-1) {
												foundSpawn=true;
											}
										}
										
										if(foundSpawn) {
											Location newSpawnLoc = new Location( w, i.getNewBlockLocation().getX(), i.getNewBlockLocation().getY(), i.getNewBlockLocation().getZ() );
											p.setBedSpawnLocation(newSpawnLoc, true);
											iter.remove();
										}
									}
								}
							}
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
							sign.update( true );

						} else if ( transferData instanceof StorageCrateTransferHolder ) {
							Inventory inventory = Bukkit.createInventory( null, 27, String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) );
							inventory.setContents( ( ( StorageCrateTransferHolder ) transferData ).getInvetory() );
							StorageChestItem.setInventoryOfCrateAtLocation( inventory, l, w );

						} else if ( transferData instanceof InventoryTransferHolder ) {

							InventoryTransferHolder invData = ( InventoryTransferHolder ) transferData;
							InventoryHolder inventoryHolder = ( InventoryHolder ) w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState();
							inventoryHolder.getInventory().setContents( invData.getInvetory() );

						}
						w.getBlockAt( l.getX(), l.getY(), l.getZ() ).setData( transferData.getData() );
					} catch ( Exception e ) {
						Movecraft.getInstance().getLogger().log( Level.SEVERE, "Severe error in map updater" );
					}

				}
				
				if(Settings.CompatibilityMode) {
					// todo: lighting stuff here
				} else {
					for ( net.minecraft.server.v1_7_R2.Chunk c : chunks ) {
						c.initLighting();
						ChunkCoordIntPair ccip = new ChunkCoordIntPair( c.locX, c.locZ ); // changed from c.x to c.locX and c.locZ


						for ( Player p : w.getPlayers() ) {
							List<ChunkCoordIntPair> chunkCoordIntPairQueue = ( List<ChunkCoordIntPair> ) ( ( CraftPlayer ) p ).getHandle().chunkCoordIntPairQueue;

							if ( !chunkCoordIntPairQueue.contains( ccip ) )
								chunkCoordIntPairQueue.add( ccip );
						}
					}
				}
				
				
				if(CraftManager.getInstance().getCraftsInWorld(w)!=null) {
					// clean up dropped items that are fragile block types on or below all crafts. They are likely garbage left on the ground from the block movements
					for(Craft cleanCraft : CraftManager.getInstance().getCraftsInWorld(w)) {
						Iterator<Entity> i=w.getEntities().iterator();
						while (i.hasNext()) {
							Entity eTest=i.next();
							if (eTest.getTicksLived()<100 && eTest.getType()==org.bukkit.entity.EntityType.DROPPED_ITEM) {
								int adjX=eTest.getLocation().getBlockX()-cleanCraft.getMinX();
								int adjZ=eTest.getLocation().getBlockZ()-cleanCraft.getMinZ();
								int[][][] hb=cleanCraft.getHitBox();
								if(adjX>=-1 && adjX<=hb.length) {
									if(adjX<0) {
										adjX=0;
									}
									if(adjX>=hb.length) {
										adjX=hb.length-1;
									}
									if(adjZ>-1 && adjZ<=hb[adjX].length) {
										Item it=(Item)eTest;

										if(Arrays.binarySearch(fragileBlocks,it.getItemStack().getTypeId())>=0) {
											eTest.remove();
										}
									}
								}
							}
						}
					}
					
					//move entities again to reduce falling out of crafts
					if(entityUpdatesInWorld!=null) {
						for(EntityUpdateCommand entityUpdate : entityUpdatesInWorld) {
							if(entityUpdate!=null) {
								Entity entity=entityUpdate.getEntity();
								Vector pVel=new Vector(entity.getVelocity().getX(),0.0,entity.getVelocity().getZ());
								
							/*	Location newLoc=entity.getLocation();
								newLoc.setX(entityUpdate.getNewLocation().getX());
								newLoc.setY(entityUpdate.getNewLocation().getY());
								newLoc.setZ(entityUpdate.getNewLocation().getZ());*/
								entity.teleport(entityUpdate.getNewLocation());
								entity.setVelocity(pVel);
							}
						}
					}

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
		long endTime=System.currentTimeMillis();
//		Movecraft.getInstance().getLogger().log( Level.INFO, "Map update took (ms): "+(endTime-startTime));
	}

	public boolean addWorldUpdate( World w, MapUpdateCommand[] mapUpdates, EntityUpdateCommand[] eUpdates) {
		ArrayList<MapUpdateCommand> get = updates.get( w );
		if ( get != null ) {
			updates.remove( w );
		} else {
			get = new ArrayList<MapUpdateCommand>();
		}

		ArrayList<MapUpdateCommand> tempSet = new ArrayList<MapUpdateCommand>();
		for ( MapUpdateCommand m : mapUpdates ) {

			if ( setContainsConflict( get, m ) ) {
				return true;
			} else {
				tempSet.add( m );
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
			for(EntityUpdateCommand e : eUpdates) {
				tempEUpdates.add(e);
			}
			eGet.addAll( tempEUpdates );
			entityUpdates.put(w, eGet);
		}		
		return false;
	}

	private boolean setContainsConflict( ArrayList<MapUpdateCommand> set, MapUpdateCommand c ) {
		for ( MapUpdateCommand command : set ) {
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

			default:
				return new TransferData( data );

		}
	}

}
