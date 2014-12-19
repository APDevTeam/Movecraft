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

package net.countercraft.movecraft.items;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.external.CardboardBox;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StorageChestItem {
	private static final Map<World, Map<MovecraftLocation, Inventory>> crateInventories = new HashMap<World, Map<MovecraftLocation, Inventory>>();
	private final ItemStack itemStack;

	public StorageChestItem() {
		this.itemStack = new ItemStack( 54, 1 );
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName( String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) );
		itemStack.setItemMeta( itemMeta );
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public static Inventory getInventoryOfCrateAtLocation( MovecraftLocation location, World w ) {
		if(Settings.DisableCrates==true)
			return null;
		return crateInventories.get( w ).get( location );
	}

	public static void setInventoryOfCrateAtLocation( Inventory i, MovecraftLocation l, World w ) {
		crateInventories.get( w ).put( l, i );
	}

	public static void removeInventoryAtLocation( World w, MovecraftLocation l ) {
		crateInventories.get( w ).remove( l );
	}

	public static void createNewInventory( MovecraftLocation l, World w ) {
		crateInventories.get( w ).put( l, Bukkit.createInventory( null, 27, String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) ) );
	}

	public static void addRecipie() {
		ShapedRecipe storageCrateRecipie = new ShapedRecipe( new StorageChestItem().getItemStack() );
		storageCrateRecipie.shape( "WWW", "WCW", "WWW" );
		storageCrateRecipie.setIngredient( 'C', Material.CHEST );
		storageCrateRecipie.setIngredient( 'W', Material.WOOD );
		Movecraft.getInstance().getServer().addRecipe( storageCrateRecipie );
	}

	public static void saveToDisk() {
		Map<String, CardboardBox[]> data = new HashMap<String, CardboardBox[]>();

		for ( World w : crateInventories.keySet() ) {
			for ( MovecraftLocation l : crateInventories.get( w ).keySet() ) {
				Inventory inventory = crateInventories.get( w ).get( l );
				ItemStack[] is = inventory.getContents();
				CardboardBox[] cardboardBoxes = new CardboardBox[is.length];

				for ( int i = 0; i < is.length; i++ ) {
					if ( is[i] != null ) {
						cardboardBoxes[i] = new CardboardBox( is[i] );
					} else {
						cardboardBoxes[i] = null;
					}
				}

				String key = w.getName() + " " + l.getX() + " " + l.getY() + " " + l.getZ();
				data.put( key, cardboardBoxes );
			}
		}

		try {
			File f = new File( Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/crates" );

			if ( !f.exists() ) {
				f.mkdirs();
			}


			FileOutputStream fileOut = new FileOutputStream( new File( Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/crates/inventories.txt" ) );

			ObjectOutputStream out = new ObjectOutputStream( fileOut );
			out.writeObject( data );
			out.close();
			fileOut.close();

		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	public static void readFromDisk() {
		// Initialise a List for every world
		for ( World w : Movecraft.getInstance().getServer().getWorlds() ) {
			crateInventories.put( w, new HashMap<MovecraftLocation, Inventory>() );
		}

		try {

			File f = new File( Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/crates/inventories.txt" );
			FileInputStream input = new FileInputStream( f );
			ObjectInputStream in = new ObjectInputStream( input );
			Map<String, CardboardBox[]> data = ( Map<String, CardboardBox[]> ) in.readObject();

			for ( String s : data.keySet() ) {
				CardboardBox[] cardboardBoxes = data.get( s );
				ItemStack[] is = new ItemStack[cardboardBoxes.length];

				for ( int i = 0; i < is.length; i++ ) {
					if ( cardboardBoxes[i] != null ) {
						is[i] = cardboardBoxes[i].unbox();
					} else {
						is[i] = null;
					}
				}

				Inventory inv = Bukkit.createInventory( null, 27, String.format( I18nSupport.getInternationalisedString( "Item - Storage Crate name" ) ) );
				inv.setContents( is );
				String[] split = s.split( " " );
				World w = Movecraft.getInstance().getServer().getWorld( split[0] );
				if ( w != null ) {

					int x = Integer.parseInt( split[1] );
					int y = Integer.parseInt( split[2] );
					int z = Integer.parseInt( split[3] );
					MovecraftLocation l = new MovecraftLocation( x, y, z );

					crateInventories.get( w ).put( l, inv );

				}
			}
			in.close();
			input.close();

		} catch ( FileNotFoundException ignored ) {
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}
