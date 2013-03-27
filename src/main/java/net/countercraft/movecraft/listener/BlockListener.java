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

package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.items.StorageChestItem;
import net.countercraft.movecraft.localisation.L18nSupport;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockListener implements Listener {

	@EventHandler
	public void onBlockPlace ( final BlockPlaceEvent e) {
		if ( e.getBlockAgainst().getTypeId() == 33 && e.getBlockAgainst().getData() == ((byte) 6) ) {
		e.setCancelled( true );
		} else if ( e.getItemInHand().getItemMeta() != null && e.getItemInHand().getItemMeta().getDisplayName() != null && e.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase( String.format( L18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) ) ) {
			e.getBlockPlaced().setTypeId( 33 );
			Location l = e.getBlockPlaced().getLocation();
			MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
			StorageChestItem.createNewInventory( l1, e.getBlockPlaced().getWorld() );
			new BukkitRunnable() {

				@Override
				public void run() {
					e.getBlockPlaced().setData( (byte) 6 );
				}

			}.runTask( Movecraft.getInstance() );
		}
	}

	@EventHandler
	public void onPlayerInteract ( PlayerInteractEvent event ) {

		if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
			if ( event.getClickedBlock().getTypeId() == 33 && event.getClickedBlock().getData() == ((byte) 6) ) {
				Location l = event.getClickedBlock().getLocation();
				MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
				Inventory i = StorageChestItem.getInventoryOfCrateAtLocation( l1, event.getPlayer().getWorld() );

				if ( i != null ) {
					event.getPlayer().openInventory( i );
				}
			}
		}
	}

	@EventHandler
	public void onBlockBreak ( final BlockBreakEvent e ) {
		if ( e.getBlock().getTypeId() == 33 && e.getBlock().getData() == ((byte) 6) ) {
			Location l = e.getBlock().getLocation();
			MovecraftLocation l1 = new MovecraftLocation( l.getBlockX(), l.getBlockY(), l.getBlockZ() );
			StorageChestItem.removeInventoryAtLocation( l1 );
		}
	}

}
