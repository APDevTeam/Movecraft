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
import net.minecraft.server.v1_6_R3.ChunkCoordIntPair;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_6_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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

	public void run() {
		if ( updates.isEmpty() ) return;
		
		for ( World w : updates.keySet() ) {
			if ( w != null ) {
				List<MapUpdateCommand> updatesInWorld = updates.get( w );
				List<EntityUpdateCommand> entityUpdatesInWorld = entityUpdates.get( w );
				Map<MovecraftLocation, TransferData> dataMap = new HashMap<MovecraftLocation, TransferData>();
				Set<net.minecraft.server.v1_6_R3.Chunk> chunks = null; 
				Set<Chunk> cmChunks = null;
				if(Settings.CompatibilityMode) {
					cmChunks = new HashSet<Chunk>();					
				} else {
					chunks = new HashSet<net.minecraft.server.v1_6_R3.Chunk>();
				}

				// Preprocessing
				for ( MapUpdateCommand c : updatesInWorld ) {
					MovecraftLocation l = c.getOldBlockLocation();

					if ( l != null ) {
						TransferData blockDataPacket = getBlockDataPacket( w.getBlockAt( l.getX(), l.getY(), l.getZ() ).getState(), c.getRotation() );
						if ( blockDataPacket != null ) {
							dataMap.put( c.getNewBlockLocation(), blockDataPacket );
						}
					}

				} 
				
				ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
				boolean isFirstChunk=true;
				
				final int[] fragileBlocks = new int[]{ 50, 52, 55, 63, 65, 68, 69, 70, 71, 72, 75, 76, 77, 93, 94, 96, 131, 132, 143, 147, 148, 149, 150, 151, 171, 323, 324, 330, 331, 356, };
				Arrays.sort(fragileBlocks);
				
				// Perform core block updates, don't do "fragiles" yet. 
				for ( MapUpdateCommand m : updatesInWorld ) {
					boolean isFragile=(Arrays.binarySearch(fragileBlocks,m.getTypeID())>=0);
					
					if(!isFragile) {
						MovecraftLocation workingL = m.getNewBlockLocation();

						int x = workingL.getX();
						int y = workingL.getY();
						int z = workingL.getZ();
						Chunk chunk=null;
					
						// Calculate chunk if necessary, check list of chunks already loaded first
					
						if(isFirstChunk) {
							chunk = w.getBlockAt( x, y, z ).getChunk();					
							chunkList.add(chunk);
							isFirstChunk=false;
						} else {
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
						}
				
						net.minecraft.server.v1_6_R3.Chunk c = null;
						Chunk cmC = null;
						if(Settings.CompatibilityMode) {
							cmC = chunk;
						} else {
							c = ( ( CraftChunk ) chunk ).getHandle();
						}

						//get the inner-chunk index of the block to change
						//modify the block in the chunk

						int newTypeID = m.getTypeID();
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
							w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
							if ( !cmChunks.contains( cmC ) ) {
								cmChunks.add( cmC );
							}
						} else {
							if((origType!=0)&&(origType!=newTypeID)) {
								c.a( x & 15, y, z & 15, 0, 0 );
							} 
							success = c.a( x & 15, y, z & 15, newTypeID, data );
							if ( !success ) {
								w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
							}
							if ( !chunks.contains( c ) ) {
								chunks.add( c );
							}
						}						
					}
				}

				// Fix redstone and other "fragiles"				
				for ( MapUpdateCommand i : updatesInWorld ) {
					boolean isFragile=(Arrays.binarySearch(fragileBlocks,i.getTypeID())>=0);
					if(isFragile) {
						MovecraftLocation workingL = i.getNewBlockLocation();

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
								
						net.minecraft.server.v1_6_R3.Chunk c = null;
						Chunk cmC = null;
						if(Settings.CompatibilityMode) {
							cmC = chunk;
						} else {
							c = ( ( CraftChunk ) chunk ).getHandle();
						}

						//get the inner-chunk index of the block to change
						//modify the block in the chunk

						int newTypeID = i.getTypeID();
						TransferData transferData = dataMap.get( workingL );

						byte data;
						if ( transferData != null ) {
							data = transferData.getData();
						} else {
							data = 0;
						}

						int origType=w.getBlockAt( x, y, z ).getTypeId();
						boolean success = false;

						//don't blank out block if it's already air, or if blocktype will not be changed
						if(Settings.CompatibilityMode) {
							if((origType!=0)&&(origType!=newTypeID)) {
								w.getBlockAt( x, y, z ).setTypeIdAndData( 0, (byte) 0, false );
							} 
							w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
							if ( !cmChunks.contains( cmC ) ) {
								cmChunks.add( cmC );
							}
						} else {
							if((origType!=0)&&(origType!=newTypeID)) {
								c.a( x & 15, y, z & 15, 0, 0 );
							} 
							success = c.a( x & 15, y, z & 15, newTypeID, data );
							if ( !success ) {
								w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
							}
							if ( !chunks.contains( c ) ) {
								chunks.add( c );
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
					for (Chunk cmC : cmChunks ) {

					}
				} else {
					for ( net.minecraft.server.v1_6_R3.Chunk c : chunks ) {
						c.initLighting();
						ChunkCoordIntPair ccip = new ChunkCoordIntPair( c.x, c.z );


						for ( Player p : w.getPlayers() ) {
							List<ChunkCoordIntPair> chunkCoordIntPairQueue = ( List<ChunkCoordIntPair> ) ( ( CraftPlayer ) p ).getHandle().chunkCoordIntPairQueue;

							if ( !chunkCoordIntPairQueue.contains( ccip ) )
								chunkCoordIntPairQueue.add( ccip );
						}
					}
				}
				
				// teleport any entities that are slated to be moved
				if(entityUpdatesInWorld!=null) {
					Iterator<EntityUpdateCommand> iEntityUpdate=entityUpdatesInWorld.iterator();
					while (iEntityUpdate.hasNext()) {
						EntityUpdateCommand eUpdate=iEntityUpdate.next();
						Entity eTest=eUpdate.getEntity();
						Location lTest=eUpdate.getNewLocation().clone();
						if(eTest!=null) {
							org.bukkit.util.Vector velocity=eTest.getVelocity().clone();

							eTest.teleport( lTest );
							eTest.setVelocity(velocity);
						}
					}
				}

				
				// finally clean up any dropped items on all crafts. They are likely garbage left on the ground from the block movements
				if(CraftManager.getInstance().getCraftsInWorld(w)!=null) {
					for(Craft cleanCraft : CraftManager.getInstance().getCraftsInWorld(w)) {
						Iterator<Entity> i=w.getEntities().iterator();
						while (i.hasNext()) {
							Entity eTest=i.next();
							if ( MathUtils.playerIsWithinBoundingPolygon( cleanCraft.getHitBox(), cleanCraft.getMinX(), cleanCraft.getMinZ(), MathUtils.bukkit2MovecraftLoc( eTest.getLocation() ) ) ) {
								if(eTest.getType()==org.bukkit.entity.EntityType.DROPPED_ITEM) {
									eTest.remove();
								}
							}
						}
					}	
				}
			}
		}

		
		updates.clear();
		entityUpdates.clear();
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
			ArrayList<EntityUpdateCommand> tempEUpdates = new ArrayList<EntityUpdateCommand>();
			for(EntityUpdateCommand e : eUpdates) {
				tempEUpdates.add(e);
			}

			entityUpdates.put(w, tempEUpdates);
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
			case 61:
			case 62:
			case 117:
				// Data and Inventory
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
