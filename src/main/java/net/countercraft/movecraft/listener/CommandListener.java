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

import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.logging.Level;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

//public class CommandListener implements Listener {
public class CommandListener implements CommandExecutor {

	private CraftType getCraftTypeFromString(String s) {
		for (CraftType t : CraftManager.getInstance().getCraftTypes()) {
			if (s.equalsIgnoreCase(t.getCraftName())) {
				return t;
			}
		}

		return null;
	}

	private Location getCraftTeleportPoint(Craft craft, World w) {
		int maxDX=0;
		int maxDZ=0;
		int maxY=0;
		int minY=32767;
		for(int[][] i1 : craft.getHitBox()) {
			maxDX++;
			if (i1!=null) {
				int indexZ=0;
				for (int[] i2 : i1) {
					indexZ++;
					if (i2!=null) {
						if (i2[0]<minY) {
							minY=i2[0];
						}
					}
					if (i2!=null) {
						if (i2[1]>maxY) {
							maxY=i2[1];
						}
					}
				}
				if (indexZ>maxDZ) {
					maxDZ=indexZ;
				}

			}
		}
		int telX=craft.getMinX()+(maxDX / 2);
		int telZ=craft.getMinZ()+(maxDZ / 2);
		int telY=maxY;
		Location telPoint=new Location(w, telX, telY, telZ);
		return telPoint;
	}

	private MovecraftLocation getCraftMidPoint(Craft craft) {
		int maxDX=0;
		int maxDZ=0;
		int maxY=0;
		int minY=32767;
		for(int[][] i1 : craft.getHitBox()) {
			maxDX++;
			if (i1!=null) {
				int indexZ=0;
				for(int[] i2 : i1) {
					indexZ++;
					if(i2!=null) {
						if(i2[0]<minY) {
							minY=i2[0];
						}
					}
					if (i2!=null) {
						if(i2[1]<maxY) {
							maxY=i2[1];
						}
					}
				}
				if (indexZ>maxDZ) {
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
			if ( !player.hasPermission( "movecraft.commands" ) && !player.hasPermission( "movecraft.commands.release" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			if(args.length > 0) {
				Player target = Bukkit.getPlayerExact(args[0]);
				if( target == null  && args[0] != "-a") {
					sender.sendMessage("That player could not be found");
				} else {
					if(!player.hasPermission("movecraft.commands.release.others")) {
						player.sendMessage("You do not have permission to make others release");
					} else {
						if (args[0] == "-a") {
							for (Player p : Bukkit.getOnlinePlayers()) {
								String name = p.getName();
								final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
								if(pCraft != null) {
									CraftManager.getInstance().removeCraft(pCraft);
									
							}
						}
							player.sendMessage("You forced release very player's ship");
						} else {
						final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
						if(pCraft != null) {
							CraftManager.getInstance().removeCraft(pCraft);
							player.sendMessage("You have successfully force released a ship");
						} else {
							player.sendMessage("That player is not piloting a craft");
						}
					}
				}
			}
				} else {
			final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( pCraft != null ) {
				CraftManager.getInstance().removeCraft( pCraft );
				//e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft has been released" ) ) );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Error - You do not have a craft to release!" ) ) );
			}

			return true;
		}
	}

		if ( cmd.getName().equalsIgnoreCase("pilot" ) ) {
			if ( !player.hasPermission( "movecraft.commands" ) && !player.hasPermission( "movecraft.commands.pilot" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			
			if(args.length>0) {
				if ( player.hasPermission( "movecraft." + args[0] + ".pilot" ) ) {				
					MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());
					Craft c = new Craft( getCraftTypeFromString( args[0] ), player.getWorld() );
		
					if ( CraftManager.getInstance().getCraftByPlayerName( player.getName() ) == null ) {
						c.detect( player, player, startPoint );
					} else {
						Craft oldCraft=CraftManager.getInstance().getCraftByPlayerName( player.getName() );
						CraftManager.getInstance().removeCraft( oldCraft );
						c.detect( player, player, startPoint );
					}
		
				} else {
					player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				}
				return true;
			}
		}
		
		if( cmd.getName().equalsIgnoreCase("rotateleft")) {
			if ( !player.hasPermission( "movecraft.commands" ) && !player.hasPermission( "movecraft.commands.rotateleft" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
				MovecraftLocation midPoint = getCraftMidPoint(craft);
				CraftManager.getInstance().getCraftByPlayerName( player.getName() ).rotate( Rotation.ANTICLOCKWISE, midPoint );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );				
			}
			
			return true;
		}

		if(  cmd.getName().equalsIgnoreCase("rotateright")) {
			if ( !player.hasPermission( "movecraft.commands" ) && !player.hasPermission( "movecraft.commands.rotateright" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".rotate" ) ) {
				MovecraftLocation midPoint = getCraftMidPoint(craft);
				CraftManager.getInstance().getCraftByPlayerName( player.getName() ).rotate( Rotation.CLOCKWISE, midPoint );
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );				
			}
			
			return true;
		}

		if( cmd.getName().equalsIgnoreCase("cruise")) {
			if ( !player.hasPermission( "movecraft.commands" ) && !player.hasPermission( "movecraft.commands.cruise" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			
			final Craft craft = CraftManager.getInstance().getCraftByPlayerName( player.getName() );

			if ( player.hasPermission( "movecraft." + craft.getType().getCraftName() + ".move" ) ) {
				if(craft.getType().getCanCruise()) {
					if(args.length == 0) {
						float yaw = player.getLocation().getYaw();
						if(yaw >= 135 || yaw < -135) {
							// north
							craft.setCruiseDirection((byte)0x3);
							craft.setCruising(true);
						} else if(yaw >= 45) {
							// west
							craft.setCruiseDirection((byte)0x5);
							craft.setCruising(true);
						} else if(yaw < -45) {
							// south
							craft.setCruiseDirection((byte)0x2);
							craft.setCruising(true);
						} else {
							// east
							craft.setCruiseDirection((byte)0x4);
							craft.setCruising(true);
						}
						return true;
					}
					if(args[0].equalsIgnoreCase("north")) {
						craft.setCruiseDirection((byte)0x3);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("south")) {
						craft.setCruiseDirection((byte)0x2);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("east")) {
						craft.setCruiseDirection((byte)0x4);
						craft.setCruising(true);
					}
					if(args[0].equalsIgnoreCase("west")) {
						craft.setCruiseDirection((byte)0x5);
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
		
		if(cmd.getName().equalsIgnoreCase("craftreport")) {
			if ( !player.hasPermission( "movecraft.commands" ) &&  !player.hasPermission( "movecraft.commands.craftreport" ) ) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			
			boolean noCraftsFound=true;
			if(CraftManager.getInstance().getCraftsInWorld(player.getWorld())!=null)
				for(Craft craft : CraftManager.getInstance().getCraftsInWorld(player.getWorld())) {
					if(craft!=null) {
						String output=new String();
						if(craft.getNotificationPlayer()!=null) {
							output=craft.getType().getCraftName()+" "+craft.getNotificationPlayer().getName()+" "+craft.getBlockList().length+" @ "+craft.getMinX()+","+craft.getMinY()+","+craft.getMinZ();
						} else {
							output=craft.getType().getCraftName()+" NULL "+craft.getBlockList().length+" @ "+craft.getMinX()+","+craft.getMinY()+","+craft.getMinZ();
							
						}
						player.sendMessage(output);
						noCraftsFound=false;
					}				
				}
			if(noCraftsFound) {
				player.sendMessage("No crafts found");
			}
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("contacts")) {
			if(CraftManager.getInstance().getCraftByPlayer(player)!=null ) {
				Craft ccraft=CraftManager.getInstance().getCraftByPlayer(player);
				boolean foundContact=false;
				for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(ccraft.getW())) {
					long cposx=ccraft.getMaxX()+ccraft.getMinX();
					long cposy=ccraft.getMaxY()+ccraft.getMinY();
					long cposz=ccraft.getMaxZ()+ccraft.getMinZ();
					cposx=cposx>>1;
					cposy=cposy>>1;
					cposz=cposz>>1;
					long tposx=tcraft.getMaxX()+tcraft.getMinX();
					long tposy=tcraft.getMaxY()+tcraft.getMinY();
					long tposz=tcraft.getMaxZ()+tcraft.getMinZ();
					tposx=tposx>>1;
					tposy=tposy>>1;
					tposz=tposz>>1;
					long diffx=cposx-tposx;
					long diffy=cposy-tposy;
					long diffz=cposz-tposz;
					long distsquared=Math.abs(diffx)*Math.abs(diffx);
					distsquared+=Math.abs(diffy)*Math.abs(diffy);
					distsquared+=Math.abs(diffz)*Math.abs(diffz);
					long detectionRange=0;
					if(tposy>tcraft.getW().getSeaLevel()) {
						detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getDetectionMultiplier());
					} else {
						detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getUnderwaterDetectionMultiplier());
					}
					if(distsquared<detectionRange*detectionRange && tcraft.getNotificationPlayer()!=ccraft.getNotificationPlayer()) {
						// craft has been detected				
						foundContact=true;
						String notification="Contact: ";
						notification+=tcraft.getType().getCraftName();
						notification+=" commanded by ";
						notification+=tcraft.getNotificationPlayer().getDisplayName();
						notification+=", size: ";
						notification+=tcraft.getOrigBlockCount();
						notification+=", range: ";
						notification+=(int)Math.sqrt(distsquared);
						notification+=" to the";
						if(Math.abs(diffx) > Math.abs(diffz))
							if(diffx<0)
								notification+=" east.";
							else
								notification+=" west.";
						else
							if(diffz<0)
								notification+=" south.";
							else
								notification+=" north.";
							
						ccraft.getNotificationPlayer().sendMessage(notification);
					}
				}
				if(!foundContact)
					player.sendMessage( String.format( I18nSupport.getInternationalisedString( "No contacts within range" ) ) );
				return true;	
			} else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "You must be piloting a craft" ) ) );
				return true;	
			}
				
		}
		
		if(cmd.getName().equalsIgnoreCase("manOverBoard")) {
			if(CraftManager.getInstance().getCraftByPlayerName(player.getName())!=null) {
				Location telPoint = getCraftTeleportPoint(CraftManager.getInstance().getCraftByPlayerName(player.getName()), CraftManager.getInstance().getCraftByPlayerName(player.getName()).getW());
				player.teleport(telPoint);
			} else {
				for(World w : Bukkit.getWorlds()) {
					if(CraftManager.getInstance().getCraftsInWorld( w )!=null)
						for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld( w )) {
							if(tcraft.getMovedPlayers().containsKey(player)) {
								if(tcraft.getW()!=player.getWorld()) {
									player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Distance to craft is too far" ) ) );
								}
								if((System.currentTimeMillis()-tcraft.getMovedPlayers().get(player))/1000<Settings.ManOverBoardTimeout) {
									Location telPoint = getCraftTeleportPoint(tcraft, w);
									if(telPoint.distance(player.getLocation())>1000) {
										player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Distance to craft is too far" ) ) );										
									} else {
										player.teleport(telPoint);
									}
								}
							}
						}
				}
			}
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("siege")) {
			if(!player.hasPermission( "movecraft.siege" )) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Insufficient Permissions" ) ) );
				return true;
			}
			if(Settings.SiegeName==null) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Siege is not configured on this server" ) ) );
				return true;
			}
			if(Movecraft.getInstance().siegeInProgress==true) {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "A Siege is already taking place" ) ) );
				return true;
			}
			String foundSiegeName=null;
            LocalPlayer lp = (LocalPlayer) Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
            if (regions.size() != 0){
            	for(ProtectedRegion tRegion : regions.getRegions()) {
            		for(String tSiegeName : Settings.SiegeName) {
            			if(tRegion.getId().equalsIgnoreCase(Settings.SiegeRegion.get(tSiegeName)))
            				foundSiegeName=tSiegeName;
            		}
                }
            }
            if(foundSiegeName!=null) {
            	long cost=Settings.SiegeCost.get(foundSiegeName);
            	int numControlledSieges=0;
            	for(String tSiegeName : Settings.SiegeName) {
            		ProtectedRegion tregion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(Settings.SiegeControlRegion.get(tSiegeName) );
            		if(tregion.getOwners().contains(player.getName())) {
            			numControlledSieges++;
            			cost=cost*2;
            		}
            	}

            	if(Movecraft.getInstance().getEconomy().has(player, cost)) {
            		Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("MST"));
            		int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            		int minute = rightNow.get(Calendar.MINUTE);
            		int currMilitaryTime=hour*100+minute;
            		int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
            		if((currMilitaryTime>Settings.SiegeScheduleStart.get(foundSiegeName))&&(currMilitaryTime<Settings.SiegeScheduleEnd.get(foundSiegeName)) && dayOfWeek == Settings.SiegeDayOfTheWeek.get(foundSiegeName)) {
						for (String command : Settings.SiegeCommandsOnStart.get(foundSiegeName)) {
							command.replace("%r", Settings.SiegeRegion.get(foundSiegeName));
							command.replace("%c", Settings.SiegeCost.get(foundSiegeName).toString());
							Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
						}
            			
            			Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
            						, player.getDisplayName(), foundSiegeName, Settings.SiegeDelay.get(foundSiegeName) / 60));
                        for(Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                        }
                        final String taskPlayerDisplayName=player.getDisplayName();
                        final String taskPlayerName=player.getName();
                        final String taskSiegeName=foundSiegeName;
                        BukkitTask warningtask1 = new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
	            						, taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 3));
		                        for(Player p : Bukkit.getOnlinePlayers()) {
		                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
		                        }
							}
						}.runTaskLater( Movecraft.getInstance(), ( 20 * Settings.SiegeDelay.get(taskSiegeName ) / 4 * 1 ));
                        BukkitTask warningtask2 = new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
	            						, taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 2));
		                        for(Player p : Bukkit.getOnlinePlayers()) {
		                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
		                        }
							}
						}.runTaskLater( Movecraft.getInstance(), ( 20 * Settings.SiegeDelay.get(taskSiegeName ) / 4 * 2 ));
                        BukkitTask warningtask3 = new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
	            						, taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 1));
		                        for(Player p : Bukkit.getOnlinePlayers()) {
		                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
		                        }
							}
						}.runTaskLater( Movecraft.getInstance(), ( 20 * Settings.SiegeDelay.get(taskSiegeName ) / 4 * 3 ));
						BukkitTask commencetask = new BukkitRunnable() {
							@Override
							public void run() {
								Bukkit.getServer().broadcastMessage(String.format("The Siege of %s has commenced! The siege leader is %s. Destroy the enemy vessels!"
	            						, taskSiegeName, taskPlayerDisplayName, (Settings.SiegeDuration.get(taskSiegeName) / 60) ));
		                        for(Player p : Bukkit.getOnlinePlayers()) {
		                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
		                        }
								Movecraft.getInstance().currentSiegeName=taskSiegeName;
								Movecraft.getInstance().currentSiegePlayer=taskPlayerName;
								Movecraft.getInstance().currentSiegeStartTime=System.currentTimeMillis();
							}
						}.runTaskLater( Movecraft.getInstance(), ( 20 * Settings.SiegeDelay.get(taskSiegeName ) ));
						Movecraft.getInstance().getLogger().log(Level.INFO, String.format("Siege: %s commenced by %s for a cost of %d",foundSiegeName,player.getName(),cost));	
						Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
						Movecraft.getInstance().siegeInProgress=true;
            		} else {
    					player.sendMessage(String.format( I18nSupport.getInternationalisedString( "The time is not during the Siege schedule" )));
        				return true;
            		}
            	} else {
					player.sendMessage(String.format( "You do not have enough money. You need %d",cost ));
    				return true;
            	}
            } else {
				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "Could not find a siege configuration for the region you are in" ) ) );
				return true;
            }
		}
		
		return false;
	}

}
