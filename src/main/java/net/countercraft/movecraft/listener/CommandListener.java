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

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

//public class CommandListener implements Listener {
public class CommandListener implements CommandExecutor {

	private CraftType getCraftTypeFromString( String s ) {
		for ( CraftType t : CraftManager.getInstance().getCraftTypes() ) {
			if ( s.equalsIgnoreCase( t.getCraftName() ) ) {
				return t;
			}
		}

		return null;
	}
	
	private MovecraftLocation getCraftMidPoint(Craft craft) {
		int maxDX=0;
		int maxDZ=0;
		int maxY=0;
		int minY=32767;
		for(int[][] i1 : craft.getHitBox()) {
			maxDX++;
			if(i1!=null) {
				int indexZ=0;
				for(int[] i2 : i1) {
					indexZ++;
					if(i2!=null) {
						if(i2[0]<minY) {
							minY=i2[0];
						}
					}
					if(i2!=null) {
						if(i2[1]<maxY) {
							maxY=i2[1];
						}
					}
				}
				if(indexZ>maxDZ) {
					maxDZ=indexZ;
				}
				
			}
		}
		int midX=craft.getMinX()+(maxDX/2);
		int midY=(minY+maxY)/2;
		int midZ=craft.getMinZ()+(maxDZ/2);
		MovecraftLocation midPoint=new MovecraftLocation(midX, midY, midZ);
		return midPoint;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//	public void onCommand( PlayerCommandPreprocessEvent e ) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player.");
			return false;
		}
		
		Player player=(Player) sender;

		if ( cmd.getName().equalsIgnoreCase( "release" ) ) {
			final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( pCraft != null ) {
				CraftManager.getInstance().removeCraft( pCraft );
				//e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft has been released" ) ) );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Error - You do not have a craft to release!" ) ) );
			}

			return true;
		}

		if ( cmd.getName().equalsIgnoreCase("pilot" ) ) {
			if(args.length>0) {
				if ( player.hasPermission( "movecraft." + args[0] + ".pilot" ) ) {				
					MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());
					Craft c = new Craft( getCraftTypeFromString( args[0] ), player.getWorld() );
		
					if ( CraftManager.getInstance().getCraftByPlayerName( player.getName() ) == null ) {
						c.detect( player, startPoint );
					} else {
						Craft oldCraft=CraftManager.getInstance().getCraftByPlayerName( player.getName() );
						CraftManager.getInstance().removeCraft( oldCraft );
						c.detect( player, startPoint );
					}
		
				} else {
					player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
				return true;
			}
		}
		
		if( cmd.getName().equalsIgnoreCase("rotateleft")) {
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
				MovecraftLocation midPoint = getCraftMidPoint(craft);
				CraftManager.getInstance().getCraftByPlayer( player ).rotate( Rotation.ANTICLOCKWISE, midPoint );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );				
			}
			
			return true;
		}

		if(  cmd.getName().equalsIgnoreCase("rotateright")) {
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
				MovecraftLocation midPoint = getCraftMidPoint(craft);
				CraftManager.getInstance().getCraftByPlayer( player ).rotate( Rotation.CLOCKWISE, midPoint );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );				
			}
			
			return true;
		}

		if( cmd.getName().equalsIgnoreCase("cruise")) {
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".move" ) ) {
				if(craft.getType().getCanCruise()) {
					if(args[0].equalsIgnoreCase("north")) {
						craft.setCruiseDirection((byte)0x3);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("south")) {
						craft.setCruiseDirection((byte)0x2);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("east")) {
						craft.setCruiseDirection((byte)0x5);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("west")) {
						craft.setCruiseDirection((byte)0x4);
						craft.setCruising(true);
					}
				}
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );				
			}
			
			return true;
		}

		if(cmd.getName().equalsIgnoreCase("cruiseoff")) {
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );
			if(craft!=null) {
				craft.setCruising(false);
			}
			return true;
		}
		
		return false;
	}

}
