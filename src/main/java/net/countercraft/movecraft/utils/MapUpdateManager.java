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
import net.countercraft.movecraft.items.StorageChestItem;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.datastructures.InventoryTransferHolder;
import net.countercraft.movecraft.utils.datastructures.SignTransferHolder;
import net.countercraft.movecraft.utils.datastructures.StorageCrateTransferHolder;
import net.countercraft.movecraft.utils.datastructures.TransferData;
import net.minecraft.server.v1_6_R2.ChunkCoordIntPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_6_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

public class MapUpdateManager extends BukkitRunnable {
	private final HashMap<World, ArrayList<MapUpdateCommand>> updates = new HashMap<World, ArrayList<MapUpdateCommand>>();

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
				Map<MovecraftLocation, TransferData> dataMap = new HashMap<MovecraftLocation, TransferData>();
				Set<net.minecraft.server.v1_6_R2.Chunk> chunks = new HashSet<net.minecraft.server.v1_6_R2.Chunk>();

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

				// Perform core block updates
				for ( MapUpdateCommand m : updatesInWorld ) {
					MovecraftLocation workingL = m.getNewBlockLocation();


					int x = workingL.getX();
					int y = workingL.getY();
					int z = workingL.getZ();

					// Calculate chunk

					Chunk chunk = w.getBlockAt( x, y, z ).getChunk();
					net.minecraft.server.v1_6_R2.Chunk c = ( ( CraftChunk ) chunk ).getHandle();
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

					c.a( x & 15, y, z & 15, 0, 0 );
					boolean success = c.a( x & 15, y, z & 15, newTypeID, data );

					if ( !success && newTypeID != 0 ) {
						boolean b = w.getBlockAt( x, y, z ).setTypeIdAndData( newTypeID, data, false );
						if ( !b ) {
							Movecraft.getInstance().getLogger().log( Level.SEVERE, "Map interface error" );
						}
					}

					if ( !chunks.contains( c ) ) {
						chunks.add( c );
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


				for ( net.minecraft.server.v1_6_R2.Chunk c : chunks ) {
					c.initLighting();
					ChunkCoordIntPair ccip = new ChunkCoordIntPair( c.x, c.z );


					for ( Player p : w.getPlayers() ) {
						List<ChunkCoordIntPair> chunkCoordIntPairQueue = ( List<ChunkCoordIntPair> ) ( ( CraftPlayer ) p ).getHandle().chunkCoordIntPairQueue;

						if ( !chunkCoordIntPairQueue.contains( ccip ) )
							chunkCoordIntPairQueue.add( ccip );
					}
				}
			}
		}

		updates.clear();

	}

	public boolean addWorldUpdate( World w, MapUpdateCommand[] mapUpdates ) {
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
