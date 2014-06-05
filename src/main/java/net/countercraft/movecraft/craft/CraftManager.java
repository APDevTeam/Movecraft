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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class CraftManager {
	private static final CraftManager ourInstance = new CraftManager();
	private CraftType[] craftTypes;
	private final Map<World, Set<Craft>> craftList = new ConcurrentHashMap<World, Set<Craft>>();
	private final HashMap<Player, Craft> craftPlayerIndex = new HashMap<Player, Craft>();
	private final HashMap<Player, BukkitTask> releaseEvents = new HashMap<Player, BukkitTask>();

	public static CraftManager getInstance() {
		return ourInstance;
	}

	private CraftManager() {
		initCraftTypes();
	}

	public CraftType[] getCraftTypes() {
		return craftTypes;
	}

	void initCraftTypes() {
		File craftsFile = new File( Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/types" );

		if ( !craftsFile.exists() ) {
			craftsFile.mkdirs();
		}

		HashSet<CraftType> craftTypesSet = new HashSet<CraftType>();

		boolean foundCraft=false;
		for ( File file : craftsFile.listFiles() ) {
			if ( file.isFile() ) {

				if ( file.getName().contains( ".craft" ) ) {
					CraftType type = new CraftType( file );
					craftTypesSet.add( type );
					foundCraft=true;
				}
			}
		}
		if(!foundCraft) {
			Movecraft.getInstance().getLogger().log(Level.SEVERE, "ERROR: NO CRAFTS FOUND!!!");
		}
		craftTypes = craftTypesSet.toArray( new CraftType[1] );
		Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Startup - Number of craft files loaded" ), craftTypes.length ) );
	}

	public void addCraft( Craft c, Player p ) {
		Set<Craft> crafts = craftList.get( c.getW() );
		if ( crafts == null ) {
			craftList.put( c.getW(), new HashSet<Craft>() );
		}
		craftList.get( c.getW() ).add( c );
		craftPlayerIndex.put( p, c );
	}

	public void removeCraft( Craft c ) {
		removeReleaseTask(c);
		// don't just release torpedoes, make them sink so they don't clutter up the place
		if(c.getType().getCruiseOnPilot()==true) {
			c.setCruising(false);
			c.setSinking(true);
			return;
		}
		craftList.get( c.getW() ).remove( c );
		if ( getPlayerFromCraft( c ) != null ) {
			getPlayerFromCraft( c ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Release - Craft has been released message" ) ) );
			Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Release - Player has released a craft console" ), getPlayerFromCraft( c ).getName(), c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
		} else {
			Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "NULL Player has released a craft of type %s with size %d at coordinates : %d x , %d z" ),  c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
		}
		craftPlayerIndex.remove( getPlayerFromCraft( c ) );
	}

	public Craft[] getCraftsInWorld( World w ) {
		Set<Craft> crafts = craftList.get( w );
		if ( crafts == null || crafts.isEmpty() ) {
			return null;
		} else {
			return craftList.get( w ).toArray( new Craft[1] );
		}
	}

	public Craft getCraftByPlayer( Player p ) {
		return craftPlayerIndex.get( p );
	}


	public Craft getCraftByPlayerName( String name ) {
		Set<Player> players = craftPlayerIndex.keySet();
		for(Player player : players) {
			if(player!=null) {
				if(player.getName().equals(name)) {
					return craftPlayerIndex.get( player );
				}
			}
		}
		return null;
	}

	public Player getPlayerFromCraft( Craft c ) {
		for ( Map.Entry<Player, Craft> playerCraftEntry : craftPlayerIndex.entrySet() ) {

			if ( playerCraftEntry.getValue() == c ) {
				return playerCraftEntry.getKey();
			}

		}

		return null;
	}
	
	public void removePlayerFromCraft( Craft c ) {
		if ( getPlayerFromCraft( c ) != null ) {
			removeReleaseTask(c);
			getPlayerFromCraft( c ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Release - Craft has been released message" ) ) );
			Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Release - Player has released a craft console" ), getPlayerFromCraft( c ).getName(), c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
			Player p=getPlayerFromCraft( c );
			craftPlayerIndex.put(null, c);
			craftPlayerIndex.remove( p );		
		}
	}
	
	public HashMap<Player, BukkitTask> getReleaseEvents(){
		return releaseEvents;
	}
	
		     
	public final void addReleaseTask(final Craft c){
		Player p = getPlayerFromCraft( c );
		if ( !getReleaseEvents().containsKey( p ) ) {
			p.sendMessage( String.format( I18nSupport.getInternationalisedString( "Release - Player has left craft" ) ) );
			BukkitTask releaseTask = new BukkitRunnable() {
				@Override
				public void run() {
					removeCraft(c);
				}
			}.runTaskLater( Movecraft.getInstance(), ( 20 * 15 ) );
			CraftManager.getInstance().getReleaseEvents().put( p, releaseTask );
		}
	}
		 
	public final void removeReleaseTask(final Craft c){
		Player p = getPlayerFromCraft( c );
		if (p!= null){
			if ( releaseEvents.containsKey(p) ) {
				releaseEvents.get(p).cancel();
				releaseEvents.remove(p);
			}
		}
	}
}
