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

package net.countercraft.movecraft.async;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.detection.DetectionTaskData;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BlockUtils;
import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.apache.commons.collections.ListUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.mozilla.javascript.JavaScriptException;

import at.pavlov.cannons.cannon.Cannon;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import net.countercraft.movecraft.utils.ItemDropUpdateCommand;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;

public class AsyncManager extends BukkitRunnable {
	private static final AsyncManager instance = new AsyncManager();
	private final HashMap<AsyncTask, Craft> ownershipMap = new HashMap<AsyncTask, Craft>();
	private final HashMap<org.bukkit.entity.TNTPrimed, Double> TNTTracking = new HashMap<org.bukkit.entity.TNTPrimed, Double>();
	private final HashMap<Craft, HashMap<Craft, Long>> recentContactTracking = new HashMap<Craft, HashMap<Craft, Long>>();
	private HashMap<org.bukkit.entity.SmallFireball, Long> FireballTracking = new HashMap <org.bukkit.entity.SmallFireball, Long>();
	private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<AsyncTask>();
	private final HashSet<Craft> clearanceSet = new HashSet<Craft>();
	private long lastTracerUpdate = 0;
	private long lastFireballCheck = 0;
	private long lastTNTContactCheck = 0;
	private long lastFadeCheck = 0;
	private long lastContactCheck = 0;

	public static AsyncManager getInstance() {
		return instance;
	}

	private AsyncManager() {
	}

	public void submitTask( AsyncTask task, Craft c ) {
		if ( c.isNotProcessing() ) {
			c.setProcessing( true );
			ownershipMap.put( task, c );
			task.runTaskAsynchronously( Movecraft.getInstance() );
		}
	}

	public void submitCompletedTask( AsyncTask task ) {
		finishedAlgorithms.add( task );
	}

	void processAlgorithmQueue() {
		int runLength = 10;
		int queueLength = finishedAlgorithms.size();

		runLength = Math.min( runLength, queueLength );

		for ( int i = 0; i < runLength; i++ ) {
			boolean sentMapUpdate=false;
			AsyncTask poll = finishedAlgorithms.poll();
			Craft c = ownershipMap.get( poll );

			if ( poll instanceof DetectionTask ) {
				// Process detection task

				DetectionTask task = ( DetectionTask ) poll;
				DetectionTaskData data = task.getData();

				Player p = data.getPlayer();
				Player notifyP = data.getNotificationPlayer();
				Craft pCraft = CraftManager.getInstance().getCraftByPlayer( p );

				if ( pCraft != null && p != null ) {
					//Player is already controlling a craft
					notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed - Already commanding a craft" ) ) );
				} else {
					if ( data.failed() ) {
						if(notifyP!=null)
							notifyP.sendMessage( data.getFailMessage() );
						else
							Movecraft.getInstance().getLogger().log( Level.INFO,"NULL Player Craft Detection failed:"+data.getFailMessage());

					} else {
						Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld( c.getW() );
						boolean failed = false;

						if ( craftsInWorld != null ) {
							for ( Craft craft : craftsInWorld ) {

								if ( BlockUtils.arrayContainsOverlap( craft.getBlockList(), data.getBlockList() ) && (c.getType().getCruiseOnPilot() || p!=null)) {  // changed from p!=null
									if(craft.getType()==c.getType() || craft.getBlockList().length<=data.getBlockList().length) {
										notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed Craft is already being controlled" ) ) );
										failed = true;
									} else { // if this is a different type than the overlapping craft, and is smaller, this must be a child craft, like a fighter on a carrier
										if(craft.isNotProcessing()==false) {
											failed=true;
											notifyP.sendMessage(String.format( I18nSupport.getInternationalisedString( "Parent Craft is busy" )));
										}
										
										// remove the new craft from the parent craft
										List<MovecraftLocation> parentBlockList=ListUtils.subtract(Arrays.asList(craft.getBlockList()), Arrays.asList(data.getBlockList()));
										craft.setBlockList(parentBlockList.toArray( new MovecraftLocation[1] ));
										craft.setOrigBlockCount(craft.getOrigBlockCount()-data.getBlockList().length);

										// Rerun the polygonal bounding formula for the parent craft
										Integer parentMaxX = null;
										Integer parentMaxZ = null;
										Integer parentMinX = null;
										Integer parentMinZ = null;
										for ( MovecraftLocation l : parentBlockList ) {
											if ( parentMaxX == null || l.getX() > parentMaxX ) {
												parentMaxX = l.getX();
											}
											if ( parentMaxZ == null || l.getZ() > parentMaxZ ) {
												parentMaxZ = l.getZ();
											}
											if ( parentMinX == null || l.getX() < parentMinX ) {
												parentMinX = l.getX();
											}
											if ( parentMinZ == null || l.getZ() < parentMinZ ) {
												parentMinZ = l.getZ();
											}
										}
										int parentSizeX, parentSizeZ;
										parentSizeX = ( parentMaxX - parentMinX ) + 1;
										parentSizeZ = ( parentMaxZ - parentMinZ ) + 1;
										int[][][] parentPolygonalBox = new int[parentSizeX][][];
										for ( MovecraftLocation l : parentBlockList ) {
											if ( parentPolygonalBox[l.getX() - parentMinX] == null ) {
												parentPolygonalBox[l.getX() - parentMinX] = new int[parentSizeZ][];
											}
											if ( parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ] == null ) {
												parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ] = new int[2];
												parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0] = l.getY();
												parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1] = l.getY();
											} else {
												int parentMinY = parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0];
												int parentMaxY = parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1];

												if ( l.getY() < parentMinY ) {
													parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0] = l.getY();
												}
												if ( l.getY() > parentMaxY ) {
													parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1] = l.getY();
												}
											}
										}
										craft.setHitBox(parentPolygonalBox);										
									}
								}

							}
						}
						if ( !failed ) {
							c.setBlockList( data.getBlockList() );
							c.setOrigBlockCount(data.getBlockList().length);
							c.setHitBox( data.getHitBox() );
							c.setMinX( data.getMinX() );
							c.setMinZ( data.getMinZ() );
							c.setNotificationPlayer(notifyP);

							if(notifyP!=null) {
								notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Successfully piloted craft" ) ) +" Size: "+c.getBlockList().length);
								Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Detection - Success - Log Output" ), notifyP.getName(), c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
							} else {
								Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Detection - Success - Log Output" ), "NULL PLAYER", c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );								
							}
							CraftManager.getInstance().addCraft( c, p );
						}
					}
				}


			} else if ( poll instanceof TranslationTask ) {
				//Process translation task

				TranslationTask task = ( TranslationTask ) poll;
				Player p = CraftManager.getInstance().getPlayerFromCraft( c );
				Player notifyP = c.getNotificationPlayer();

				// Check that the craft hasn't been sneakily unpiloted
		//		if ( p != null ) {     cruiseOnPilot crafts don't have player pilots

					if ( task.getData().failed() ) {
						//The craft translation failed
						if( notifyP != null && !c.getSinking())
							notifyP.sendMessage( task.getData().getFailMessage() );
							
						if(task.getData().collisionExplosion()) {
							MapUpdateCommand[] updates = task.getData().getUpdates();
							c.setBlockList( task.getData().getBlockList() );
							boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates, null,null);

							if ( failed ) {
								Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Translation - Craft collision" ) ) );
							} else {
								sentMapUpdate=true;
							}
						}
					} else {
						//The craft is clear to move, perform the block updates

						MapUpdateCommand[] updates = task.getData().getUpdates();
						EntityUpdateCommand[] eUpdates=task.getData().getEntityUpdates();
                                                ItemDropUpdateCommand[] iUpdates = task.getData().getItemDropUpdateCommands();
						//get list of cannons before sending map updates, to avoid conflicts
						HashSet<Cannon> shipCannons=null;
						if( Movecraft.getInstance().getCannonsPlugin()!=null && c.getNotificationPlayer()!=null) {
							// convert blocklist to location list
							List<Location> shipLocations=new ArrayList<Location>();
							for(MovecraftLocation loc : c.getBlockList()) {
								Location tloc=new Location(c.getW(),loc.getX(),loc.getY(),loc.getZ());
								shipLocations.add(tloc);
							}
							shipCannons=Movecraft.getInstance().getCannonsPlugin().getCannonsAPI().getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
						}
						boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates, eUpdates, iUpdates);
						
						
						if ( !failed ) {
							sentMapUpdate=true;
							c.setBlockList( task.getData().getBlockList() );
							c.setMinX( task.getData().getMinX() );
							c.setMinZ( task.getData().getMinZ() );
							c.setHitBox( task.getData().getHitbox() );
							
							// move any cannons that were present
							if( Movecraft.getInstance().getCannonsPlugin()!=null && shipCannons!=null) {
								for(Cannon can : shipCannons) {
									can.move(new Vector(task.getData().getDx(),task.getData().getDy(),task.getData().getDz()));
								}
							}

						} else {

							Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Translation - Craft collision" ) ) );

						}
					}

				


			} else if ( poll instanceof RotationTask ) {
				// Process rotation task
				RotationTask task = ( RotationTask ) poll;
				Player p = CraftManager.getInstance().getPlayerFromCraft( c );
				Player notifyP = c.getNotificationPlayer();

				// Check that the craft hasn't been sneakily unpiloted
				if ( notifyP != null || task.getIsSubCraft()) {

					if ( task.isFailed() ) {
						//The craft translation failed, don't try to notify them if there is no pilot
						if(notifyP!=null)
							notifyP.sendMessage( task.getFailMessage() );
						else
							Movecraft.getInstance().getLogger().log( Level.INFO,"NULL Player Rotation Failed: "+task.getFailMessage());
					} else {
						MapUpdateCommand[] updates = task.getUpdates();
						EntityUpdateCommand[] eUpdates=task.getEntityUpdates();

						//get list of cannons before sending map updates, to avoid conflicts
						HashSet<Cannon> shipCannons=null;
						if( Movecraft.getInstance().getCannonsPlugin()!=null && c.getNotificationPlayer()!=null) {
							// convert blocklist to location list
							List<Location> shipLocations=new ArrayList<Location>();
							for(MovecraftLocation loc : c.getBlockList()) {
								Location tloc=new Location(c.getW(),loc.getX(),loc.getY(),loc.getZ());
								shipLocations.add(tloc);
							}
							shipCannons=Movecraft.getInstance().getCannonsPlugin().getCannonsAPI().getCannons(shipLocations, c.getNotificationPlayer().getUniqueId(), true);
						}	
						
						boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates, eUpdates,null);
 
						if ( !failed ) {
							sentMapUpdate=true;

							c.setBlockList( task.getBlockList() );
							c.setMinX( task.getMinX() );
							c.setMinZ( task.getMinZ() );
							c.setHitBox( task.getHitbox() );

							// rotate any cannons that were present
							if( Movecraft.getInstance().getCannonsPlugin()!=null && shipCannons!=null) {
								Location tloc=new Location(task.getCraft().getW(),task.getOriginPoint().getX(),task.getOriginPoint().getY(),task.getOriginPoint().getZ());
								for(Cannon can : shipCannons) {
									if(task.getRotation()==net.countercraft.movecraft.utils.Rotation.CLOCKWISE)
										can.rotateRight(tloc.toVector());
									if(task.getRotation()==net.countercraft.movecraft.utils.Rotation.ANTICLOCKWISE)
										can.rotateLeft(tloc.toVector());
								}
							}
						} else {

							Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Rotation - Craft Collision" ) ) );

						}
					}
				}
			}

			ownershipMap.remove( poll );
			
			// only mark the craft as having finished updating if you didn't send any updates to the map updater. Otherwise the map updater will mark the crafts once it is done with them.
			if(!sentMapUpdate) {
				clear( c ); 
			}
		}
	}
	
	public void processCruise() {
		for( World w : Bukkit.getWorlds()) {
			if(w!=null && CraftManager.getInstance().getCraftsInWorld(w)!=null) {
				for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
					if((pcraft!=null)&&pcraft.isNotProcessing()) {
						if(pcraft.getCruising()) {
							long ticksElapsed = ( System.currentTimeMillis() - pcraft.getLastCruiseUpdate() ) / 50;
							
							// if the craft should go slower underwater, make time pass more slowly there
							if(pcraft.getType().getHalfSpeedUnderwater() && pcraft.getMinY()<w.getSeaLevel())
								ticksElapsed=ticksElapsed>>1;
		
							if ( Math.abs( ticksElapsed ) >= pcraft.getType().getCruiseTickCooldown() ) {
								int dx=0;
								int dz=0;
								int dy=0;

								// ascend
								if(pcraft.getCruiseDirection()==0x42) {
									dy=0+1+pcraft.getType().getVertCruiseSkipBlocks();
								}
								// descend
								if(pcraft.getCruiseDirection()==0x43) {
									dy=0-1-pcraft.getType().getVertCruiseSkipBlocks();
									if(pcraft.getMinY()<=w.getSeaLevel())
										dy=-1;
								}
								// ship faces west
								if(pcraft.getCruiseDirection()==0x5) {
									dx=0-1-pcraft.getType().getCruiseSkipBlocks();
								}
								// ship faces east
								if(pcraft.getCruiseDirection()==0x4) {
									dx=1+pcraft.getType().getCruiseSkipBlocks();
								}
								// ship faces north
								if(pcraft.getCruiseDirection()==0x2) {
									dz=1+pcraft.getType().getCruiseSkipBlocks();
								}
								// ship faces south
								if(pcraft.getCruiseDirection()==0x3) {
									dz=0-1-pcraft.getType().getCruiseSkipBlocks();
								}
								if(pcraft.getType().getCruiseOnPilot())
									dy=pcraft.getType().getCruiseOnPilotVertMove();
								pcraft.translate(dx, dy, dz);
								pcraft.setLastDX(dx);
								pcraft.setLastDZ(dz);
								if(pcraft.getLastCruiseUpdate()!=-1) {
									pcraft.setLastCruisUpdate(System.currentTimeMillis());
								} else {
									pcraft.setLastCruisUpdate(0);									
								}
							}
							
						} else {
							if(pcraft.getKeepMoving()) {
								long rcticksElapsed = ( System.currentTimeMillis() - pcraft.getLastRightClick() ) / 50;

								// if the craft should go slower underwater, make time pass more slowly there
								if(pcraft.getType().getHalfSpeedUnderwater() && pcraft.getMinY()<w.getSeaLevel())
									rcticksElapsed=rcticksElapsed>>1;
			
								rcticksElapsed=Math.abs(rcticksElapsed);
								// if they are holding the button down, keep moving
								if (rcticksElapsed <= 10 ) {
									long ticksElapsed = ( System.currentTimeMillis() - pcraft.getLastCruiseUpdate() ) / 50;
									if ( Math.abs( ticksElapsed ) >= pcraft.getType().getTickCooldown() ) {
										pcraft.translate(pcraft.getLastDX(), pcraft.getLastDY(), pcraft.getLastDZ());
										pcraft.setLastCruisUpdate(System.currentTimeMillis());
									}
								}
							}
							if(pcraft.getPilotLocked()==true && pcraft.isNotProcessing()) {
								
								Player p = CraftManager.getInstance().getPlayerFromCraft( pcraft );
								if(p!=null) 
									if ( MathUtils.playerIsWithinBoundingPolygon( pcraft.getHitBox(), pcraft.getMinX(), pcraft.getMinZ(), MathUtils.bukkit2MovecraftLoc( p.getLocation() ) ) ) {
										double movedX=p.getLocation().getX()-pcraft.getPilotLockedX();
										double movedZ=p.getLocation().getZ()-pcraft.getPilotLockedZ();
										int dX=0;
										int dZ=0;
										if(movedX>0.15)
											dX=1;
										else if(movedX<-0.15)
											dX=-1;
										if(movedZ>0.15)
											dZ=1;
										else if(movedZ<-0.15)
											dZ=-1;
										if(dX!=0 || dZ!=0) {
											long timeSinceLastMoveCommand=System.currentTimeMillis()-pcraft.getLastRightClick();
											// wait before accepting new move commands to help with bouncing. Also ignore extreme movement
											if(Math.abs(movedX)<0.2 && Math.abs(movedZ)<0.2 && timeSinceLastMoveCommand>300) { 
	
												pcraft.setLastRightClick(System.currentTimeMillis());
												long ticksElapsed = ( System.currentTimeMillis() - pcraft.getLastCruiseUpdate() ) / 50;

												// if the craft should go slower underwater, make time pass more slowly there
												if(pcraft.getType().getHalfSpeedUnderwater() && pcraft.getMinY()<w.getSeaLevel())
													ticksElapsed=ticksElapsed>>1;
							
												if ( Math.abs( ticksElapsed ) >= pcraft.getType().getTickCooldown() ) {
													pcraft.translate(dX, 0, dZ);
													pcraft.setLastCruisUpdate(System.currentTimeMillis());
												}
												pcraft.setLastDX(dX);
												pcraft.setLastDY(0);
												pcraft.setLastDZ(dZ);
												pcraft.setKeepMoving(true);
											} else {
												Location loc=p.getLocation();
												loc.setX(pcraft.getPilotLockedX());
												loc.setY(pcraft.getPilotLockedY());
												loc.setZ(pcraft.getPilotLockedZ());
												Vector pVel=new Vector(0.0,0.0,0.0);
												p.teleport(loc);
												p.setVelocity(pVel);
											}
										} 
									}
							}
						}
					}
				}
			}
		}

	}
	
	private boolean isRegionBlockedPVP(MovecraftLocation loc,World w) {
		if(Movecraft.getInstance().getWorldGuardPlugin()==null)
			return false;
		if(Settings.WorldGuardBlockSinkOnPVPPerm==false)
			return false;
		 
		Location nativeLoc=new Location(w, loc.getX(), loc.getY(), loc.getZ());
		ApplicableRegionSet set = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(w).getApplicableRegions(nativeLoc);
		if(set.allows(DefaultFlag.PVP)==false) {
			return true;
		}
		return false;
	}
	
        private boolean isRegionFlagSinkAllowed(MovecraftLocation loc,World w) {
            if(Movecraft.getInstance().getWorldGuardPlugin()!=null && Movecraft.getInstance().getWGCustomFlagsPlugin()!= null && Settings.WGCustomFlagsUseSinkFlag){
                Location nativeLoc=new Location(w, loc.getX(), loc.getY(), loc.getZ());
                WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                return WGCFU.validateFlag(nativeLoc,Movecraft.FLAG_SINK);
            }else{
                return true;
            }
	}
        
        private Location isTownyPlotPVPEnabled(MovecraftLocation loc,World w, Set<TownBlock> townBlockSet){
            Location plugLoc=new Location(w, loc.getX(), loc.getY(), loc.getZ());
            TownBlock townBlock = TownyUtils.getTownBlock(plugLoc);
            if (townBlock != null && !townBlockSet.contains(townBlock)){
                if (TownyUtils.validatePVP(townBlock)){
                    townBlockSet.add(townBlock);
                    return null;
                }else{
                    return plugLoc;
                }
            }else{
                return null;
            }
        }
        
	public void processSinking() {
                for( World w : Bukkit.getWorlds()) {
			if(w!=null && CraftManager.getInstance().getCraftsInWorld(w)!=null) {
                                TownyWorld townyWorld = null;
                                boolean townyEnabled = false;
                                if (Movecraft.getInstance().getTownyPlugin() != null && Settings.TownyBlockSinkOnNoPVP){
                                    townyWorld = TownyUtils.getTownyWorld(w);
                                    if (townyWorld != null){
                                        townyEnabled = townyWorld.isUsingTowny();
                                    }
                                }
				// check every few seconds for every craft to see if it should be sinking
				for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                                        Set<TownBlock> townBlockSet = new HashSet<TownBlock>();                        
					if(pcraft!=null && pcraft.getSinking()==false) {
						if( pcraft.getType().getSinkPercent()!=0.0 && pcraft.isNotProcessing()) {
							long ticksElapsed = ( System.currentTimeMillis() - pcraft.getLastBlockCheck() ) / 50;
						
							if(ticksElapsed>Settings.SinkCheckTicks) {
								int totalNonAirBlocks=0;
								int totalNonAirWaterBlocks=0;
								HashMap<ArrayList<Integer>, Integer> foundFlyBlocks = new HashMap<ArrayList<Integer>, Integer>();
								boolean regionPVPBlocked=false;
                                                                boolean sinkingForbiddenByFlag = false;
                                                                boolean sinkingForbiddenByTowny = false;
								// go through each block in the blocklist, and if its in the FlyBlocks, total up the number of them
                                                                Location townyLoc = null;
								for(MovecraftLocation l : pcraft.getBlockList()) {
									if(isRegionBlockedPVP(l,w))
                                                                            regionPVPBlocked=true;
                                                                        if (!isRegionFlagSinkAllowed(l, w))
                                                                            sinkingForbiddenByFlag = true;
                                                                        if (townyLoc == null && townyEnabled && Settings.TownyBlockSinkOnNoPVP){
                                                                            townyLoc = isTownyPlotPVPEnabled(l, w, townBlockSet);
                                                                            if (townyLoc != null){
                                                                                sinkingForbiddenByTowny = true;
                                                                            }
                                                                        } 
									Integer blockID=w.getBlockAt(l.getX(), l.getY(), l.getZ()).getTypeId();
									Integer dataID=(int)w.getBlockAt(l.getX(), l.getY(), l.getZ()).getData();
									Integer shiftedID=(blockID<<4)+dataID+10000;
									for(ArrayList<Integer> flyBlockDef : pcraft.getType().getFlyBlocks().keySet()) {
										if(flyBlockDef.contains(blockID) || flyBlockDef.contains(shiftedID)) {
											Integer count=foundFlyBlocks.get(flyBlockDef);
											if(count==null) {
												foundFlyBlocks.put(flyBlockDef, 1);
											} else {
												foundFlyBlocks.put(flyBlockDef, count+1);
											}
										}
									}
									
									if(blockID!=0) {  
										totalNonAirBlocks++;
									}
									if( blockID!=0 && blockID!=8 && blockID!=9 ) {
										totalNonAirWaterBlocks++;
									}
								}
								
								// now see if any of the resulting percentages are below the threshold specified in SinkPercent
								boolean isSinking=false;
								for(ArrayList<Integer> i : pcraft.getType().getFlyBlocks().keySet()) {
									int numfound=0;
									if(foundFlyBlocks.get(i)!=null) {
										numfound=foundFlyBlocks.get(i);
									}
									double percent=((double)numfound/(double)totalNonAirBlocks)*100.0;
									double flyPercent=pcraft.getType().getFlyBlocks().get(i).get(0);
									double sinkPercent=flyPercent*pcraft.getType().getSinkPercent()/100.0;
									if(percent<sinkPercent) {
										isSinking=true;
									}
									
								}
								
								// And check the overallsinkpercent
								if(pcraft.getType().getOverallSinkPercent()!=0.0) {
									double percent=(double)totalNonAirWaterBlocks/(double)pcraft.getOrigBlockCount();
									if(percent*100.0<pcraft.getType().getOverallSinkPercent()) {
										isSinking=true;
									}
								}
								
								if(totalNonAirBlocks==0) {
									isSinking=true;
								}
								
								if(isSinking && (regionPVPBlocked || sinkingForbiddenByFlag || sinkingForbiddenByTowny) && pcraft.isNotProcessing()) {
									Player p = CraftManager.getInstance().getPlayerFromCraft( pcraft );
									Player notifyP = pcraft.getNotificationPlayer();
									if(notifyP!=null)
                                                                                if (regionPVPBlocked){
                                                                                    notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft should sink but PVP is not allowed in this WorldGuard region" ) ) );
                                                                                }else if (sinkingForbiddenByFlag){
                                                                                    notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "WGCustomFlags - Sinking a craft is not allowed in this WorldGuard region" ) ) );
                                                                                }else{
                                                                                    if (townyLoc != null){
                                                                                        notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Towny - Sinking a craft is not allowed in this town plot" ) +" @ %d,%d,%d", townyLoc.getBlockX(), townyLoc.getBlockY(),townyLoc.getBlockZ() ) );
                                                                                    }else{
                                                                                        notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Towny - Sinking a craft is not allowed in this town plot" ) ) );
                                                                                    }
                                                                                    
                                                                                }
									pcraft.setCruising(false);
									pcraft.setKeepMoving(false);
									CraftManager.getInstance().removeCraft(pcraft);
								} else {
									// if the craft is sinking, let the player know and release the craft. Otherwise update the time for the next check
									if(isSinking && pcraft.isNotProcessing()) {
										Player p = CraftManager.getInstance().getPlayerFromCraft( pcraft );
										Player notifyP=pcraft.getNotificationPlayer();
										if(notifyP!=null)
											notifyP.sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft is sinking" ) ) );
										pcraft.setCruising(false);
										pcraft.setKeepMoving(false);
										pcraft.setSinking(true);
										CraftManager.getInstance().removePlayerFromCraft(pcraft);
										final Craft releaseCraft=pcraft;
										BukkitTask releaseTask = new BukkitRunnable() {
											@Override
											public void run() {
												CraftManager.getInstance().removeCraft(releaseCraft);
											}
										}.runTaskLater( Movecraft.getInstance(), ( 20 * 600 ) );
									} else {
										pcraft.setLastBlockCheck(System.currentTimeMillis());
									}
								}
							}
						}
					}
				}

				// sink all the sinking ships
				if(CraftManager.getInstance().getCraftsInWorld(w)!=null)
					for (Craft pcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
						if(pcraft!=null && pcraft.getSinking()==true) {
							if(pcraft.getBlockList().length==0) {
								CraftManager.getInstance().removeCraft( pcraft );
							}
							if(pcraft.getMinY()<-1) {
								CraftManager.getInstance().removeCraft( pcraft );							
							}
							long ticksElapsed = ( System.currentTimeMillis() - pcraft.getLastCruiseUpdate() ) / 50;
							if ( Math.abs( ticksElapsed ) >= pcraft.getType().getSinkRateTicks() ) {
								int dx=0;
								int dz=0;
								if(pcraft.getType().getKeepMovingOnSink()) {
									dx=pcraft.getLastDX();
									dz=pcraft.getLastDZ();								
								}
								pcraft.translate(dx, -1, dz);
								if(pcraft.getLastCruiseUpdate()!=-1) {
									pcraft.setLastCruisUpdate(System.currentTimeMillis());
								} else {
									pcraft.setLastCruisUpdate(0);
								}
							}
						}
					}
			}
		}
	}
	
	public void processTracers() {
		if(Settings.TracerRateTicks==0)
			return;
		long ticksElapsed = ( System.currentTimeMillis() - lastTracerUpdate ) / 50;
		if(ticksElapsed>Settings.TracerRateTicks) {
			for( World w : Bukkit.getWorlds()) {
				if(w!=null) {
					for(org.bukkit.entity.TNTPrimed tnt : w.getEntitiesByClass(org.bukkit.entity.TNTPrimed.class)) {
						if(tnt.getVelocity().lengthSquared()>0.25) {
							for (Player p : w.getPlayers()) {
								// is the TNT within the view distance (rendered world) of the player?
								long maxDistSquared=Bukkit.getServer().getViewDistance()*16;
								maxDistSquared=maxDistSquared-16;
								maxDistSquared=maxDistSquared*maxDistSquared;

								if(p.getLocation().distanceSquared(tnt.getLocation())<maxDistSquared) {  // we use squared because its faster
									final Location loc=tnt.getLocation();
									final Player fp=p;
									final World fw=w;
									// then make a cobweb to look like smoke, place it a little later so it isn't right in the middle of the volley
									BukkitTask placeCobweb = new BukkitRunnable() {
										@Override
										public void run() {
											fp.sendBlockChange(loc, 30, (byte) 0);
										}
									}.runTaskLater( Movecraft.getInstance(), 5 );
									// then remove it
									BukkitTask removeCobweb = new BukkitRunnable() {
										@Override
										public void run() {
//											fp.sendBlockChange(loc, fw.getBlockAt(loc).getType(), fw.getBlockAt(loc).getData());
											fp.sendBlockChange(loc, 0, (byte) 0);
										}
									}.runTaskLater( Movecraft.getInstance(), 160 );
								}
							}
						}
					}
				}
			}
		lastTracerUpdate=System.currentTimeMillis();
		}
	}
	
	public void processFireballs() {
		long ticksElapsed = ( System.currentTimeMillis() - lastFireballCheck ) / 50;
		if(ticksElapsed>4) {
			for( World w : Bukkit.getWorlds()) {
				if(w!=null) {
					for(org.bukkit.entity.SmallFireball fireball : w.getEntitiesByClass(org.bukkit.entity.SmallFireball.class)) {
						if(!(fireball.getShooter() instanceof org.bukkit.entity.LivingEntity)) { // means it was launched by a dispenser
							if(w.getPlayers().size()>0) {
								Player p=w.getPlayers().get(0);
								double closest=1000000000.0;
								for(Player pi : w.getPlayers()) {
									if(pi.getLocation().distanceSquared(fireball.getLocation())<closest) {
										closest=pi.getLocation().distanceSquared(fireball.getLocation());
										p=pi;
									}
								}
								// give it a living shooter, then set the fireball to be deleted
								fireball.setShooter(p);
								final org.bukkit.entity.SmallFireball ffb=fireball;
								if(!FireballTracking.containsKey(fireball)) {
									FireballTracking.put(fireball, System.currentTimeMillis());
								}
/*								BukkitTask deleteFireballTask = new BukkitRunnable() {
									@Override
									public void run() {
										ffb.remove();
									}
								}.runTaskLater( Movecraft.getInstance(), ( 20 * Settings.FireballLifespan ) );*/
							}
						}
					}
				}
			}
			
			int timelimit= 20 * Settings.FireballLifespan * 50;
			//then, removed any exploded TNT from tracking
			Iterator<org.bukkit.entity.SmallFireball> fireballI=FireballTracking.keySet().iterator();
			while(fireballI.hasNext()) {
				org.bukkit.entity.SmallFireball fireball=fireballI.next();
				if(fireball!=null)
					if(System.currentTimeMillis()-FireballTracking.get(fireball) > timelimit) {
						fireball.remove();
						fireballI.remove();
					}
				}
			
			lastFireballCheck=System.currentTimeMillis();
		}
	}

	public void processTNTContactExplosives() {
		long ticksElapsed = ( System.currentTimeMillis() - lastTNTContactCheck ) / 50;
		if(ticksElapsed>4) {
			// see if there is any new rapid moving TNT in the worlds
			for( World w : Bukkit.getWorlds()) {
				if(w!=null) {
					for(org.bukkit.entity.TNTPrimed tnt : w.getEntitiesByClass(org.bukkit.entity.TNTPrimed.class)) {
						if(tnt.getVelocity().lengthSquared()>0.35) {
							if(!TNTTracking.containsKey(tnt)) {
								TNTTracking.put(tnt, tnt.getVelocity().lengthSquared());
							}
						}
					}
				}
			}
			
			//then, removed any exploded TNT from tracking
			Iterator<org.bukkit.entity.TNTPrimed> tntI=TNTTracking.keySet().iterator();
			while(tntI.hasNext()) {
				org.bukkit.entity.TNTPrimed tnt=tntI.next();
				if(tnt.getFuseTicks()<=0) {
					tntI.remove();
				}
			}

			//now check to see if any has abruptly changed velocity, and should explode
			for(org.bukkit.entity.TNTPrimed tnt : TNTTracking.keySet()) {
				double vel=tnt.getVelocity().lengthSquared();
				if(vel<TNTTracking.get(tnt)/10.0) {
					tnt.setFuseTicks(0);
				} else {
					// update the tracking with the new velocity so gradual changes do not make TNT explode
					TNTTracking.put(tnt, vel);
				}
			}
			
			lastTNTContactCheck=System.currentTimeMillis();
		}
	}

	public void processFadingBlocks() {
		if(Settings.FadeWrecksAfter==0)
			return;
		long ticksElapsed = ( System.currentTimeMillis() - lastFadeCheck ) / 50;
		if(ticksElapsed>20) {
			for( World w : Bukkit.getWorlds()) {
				if(w!=null) {
					ArrayList <MapUpdateCommand> updateCommands=new ArrayList <MapUpdateCommand>();
					CopyOnWriteArrayList <MovecraftLocation> locations=null;
					
					// I know this is horrible, but I honestly don't see another way to do this...
					int numTries=0;
					while((locations==null)&&(numTries<100)) {
						try {
							locations=new CopyOnWriteArrayList <MovecraftLocation>(Movecraft.getInstance().blockFadeTimeMap.keySet());
						}
						catch(java.util.ConcurrentModificationException e)
						{
							numTries++;
						}
						catch(java.lang.NegativeArraySizeException e) 
						{
							Movecraft.getInstance().blockFadeTimeMap = new HashMap<MovecraftLocation, Long>();
							Movecraft.getInstance().blockFadeTypeMap = new HashMap<MovecraftLocation, Integer>();
							Movecraft.getInstance().blockFadeWaterMap = new HashMap<MovecraftLocation, Boolean>();
							Movecraft.getInstance().blockFadeWorldMap = new HashMap<MovecraftLocation, World>();
							locations=new CopyOnWriteArrayList <MovecraftLocation>(Movecraft.getInstance().blockFadeTimeMap.keySet());
						}
					}
					
					for(MovecraftLocation loc : locations) {
						if(Movecraft.getInstance().blockFadeWorldMap.get(loc)==w) {
							Long time=Movecraft.getInstance().blockFadeTimeMap.get(loc);
							Integer type=Movecraft.getInstance().blockFadeTypeMap.get(loc);
							Boolean water=Movecraft.getInstance().blockFadeWaterMap.get(loc);
							if(time!=null && type!=null && water!=null) {
								long secsElapsed=(System.currentTimeMillis() - Movecraft.getInstance().blockFadeTimeMap.get(loc))/1000;
								// has enough time passed to fade the block?
								if(secsElapsed>Settings.FadeWrecksAfter) {
									// load the chunk if it hasn't been already
									int cx=loc.getX()>>4;
									int cz=loc.getZ()>>4;
									if(w.isChunkLoaded(cx, cz) == false) {
										w.loadChunk(cx, cz);
									}
									// check to see if the block type has changed, if so don't fade it
									if(w.getBlockTypeIdAt( loc.getX(), loc.getY(), loc.getZ())==Movecraft.getInstance().blockFadeTypeMap.get(loc)) {
										// should it become water? if not, then air
										if(Movecraft.getInstance().blockFadeWaterMap.get(loc)==true) {
											MapUpdateCommand updateCom=new MapUpdateCommand(loc,9,(byte)0,null);
											updateCommands.add(updateCom);
										} else {
											MapUpdateCommand updateCom=new MapUpdateCommand(loc,0,(byte)0,null);
											updateCommands.add(updateCom);									
										}
									}
									Movecraft.getInstance().blockFadeTimeMap.remove(loc);
									Movecraft.getInstance().blockFadeTypeMap.remove(loc);
									Movecraft.getInstance().blockFadeWorldMap.remove(loc);
									Movecraft.getInstance().blockFadeWaterMap.remove(loc);
								}
							}
						}
					}
					if(updateCommands.size()>0) {
						MapUpdateManager.getInstance().addWorldUpdate( w, updateCommands.toArray(new MapUpdateCommand[1]), null,null);
					}
				}
			}

		lastFadeCheck=System.currentTimeMillis();
		}
	}
	
	public void processDetection() {
		long ticksElapsed = ( System.currentTimeMillis() - lastContactCheck ) / 50;
		if(ticksElapsed>21) {
			for( World w : Bukkit.getWorlds()) {
				if(w!=null && CraftManager.getInstance().getCraftsInWorld(w)!=null) {
					for (Craft ccraft : CraftManager.getInstance().getCraftsInWorld(w)) {
						if(CraftManager.getInstance().getPlayerFromCraft(ccraft)!=null ) {
							if(!recentContactTracking.containsKey(ccraft)) {
								recentContactTracking.put(ccraft, new HashMap<Craft, Long>());
							}
							for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
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
								if(tposy>65) {
									detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getDetectionMultiplier());
								} else {
									detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getUnderwaterDetectionMultiplier());
								}
								if(distsquared<detectionRange*detectionRange && tcraft.getNotificationPlayer()!=ccraft.getNotificationPlayer()) {
									// craft has been detected
									
									// has the craft not been seen in the last minute, or is completely new?
									if(recentContactTracking.get(ccraft).get(tcraft)==null || System.currentTimeMillis()-recentContactTracking.get(ccraft).get(tcraft)>60000) {
										String notification="New contact: ";
										notification+=tcraft.getType().getCraftName();
										notification+=" commanded by ";
										if(tcraft.getNotificationPlayer()!=null) {
											notification+=tcraft.getNotificationPlayer().getDisplayName();
										} else {
											notification+="NULL";
										}
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
										w.playSound(ccraft.getNotificationPlayer().getLocation(), Sound.ANVIL_LAND, 1.0f, 2.0f);
										final World sw=w;
										final Player sp=ccraft.getNotificationPlayer();
										BukkitTask replaysound = new BukkitRunnable() {
											@Override
											public void run() {
												sw.playSound(sp.getLocation(), Sound.ANVIL_LAND, 10.0f, 2.0f);
											}
										}.runTaskLater( Movecraft.getInstance(), ( 5 ) );

									}
									
									long timestamp=System.currentTimeMillis();
									recentContactTracking.get(ccraft).put(tcraft, timestamp);
								}
							}
						}
					}
				}
			}
			
			
			lastContactCheck=System.currentTimeMillis();
		}
	}

	public void run() {
		clearAll();
		processCruise();
		processSinking();
		processTracers();
		processFireballs();
		processTNTContactExplosives();
		processFadingBlocks();
		processDetection();
		processAlgorithmQueue();
	}

	private void clear( Craft c ) {
		clearanceSet.add( c );
	}

	private void clearAll() {
		for ( Craft c : clearanceSet ) {
			c.setProcessing( false );
		}

		clearanceSet.clear();
	}
}
