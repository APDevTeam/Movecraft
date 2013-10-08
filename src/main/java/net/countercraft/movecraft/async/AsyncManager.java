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
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BlockUtils;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateManager;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class AsyncManager extends BukkitRunnable {
	private static final AsyncManager instance = new AsyncManager();
	private final HashMap<AsyncTask, Craft> ownershipMap = new HashMap<AsyncTask, Craft>();
	private final BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<AsyncTask>();
	private final HashSet<Craft> clearanceSet = new HashSet<Craft>();

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
			AsyncTask poll = finishedAlgorithms.poll();
			Craft c = ownershipMap.get( poll );

			if ( poll instanceof DetectionTask ) {
				// Process detection task

				DetectionTask task = ( DetectionTask ) poll;
				DetectionTaskData data = task.getData();

				Player p = Movecraft.getInstance().getServer().getPlayer( data.getPlayername() );
				Craft pCraft = CraftManager.getInstance().getCraftByPlayer( p );

				if ( pCraft != null ) {
					//Player is already controlling a craft
					p.sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed - Already commanding a craft" ) ) );
				} else {
					if ( data.failed() ) {
						Movecraft.getInstance().getServer().getPlayer( data.getPlayername() ).sendMessage( data.getFailMessage() );
					} else {
						Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld( c.getW() );
						boolean failed = false;

						if ( craftsInWorld != null ) {
							for ( Craft craft : craftsInWorld ) {

								if ( BlockUtils.arrayContainsOverlap( craft.getBlockList(), data.getBlockList() ) ) {
									Movecraft.getInstance().getServer().getPlayer( data.getPlayername() ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed Craft is already being controlled" ) ) );
									failed = true;
								}

							}
						}
						if ( !failed ) {
							c.setBlockList( data.getBlockList() );
							c.setHitBox( data.getHitBox() );
							c.setMinX( data.getMinX() );
							c.setMinZ( data.getMinZ() );

							Movecraft.getInstance().getServer().getPlayer( data.getPlayername() ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Successfully piloted craft" ) ) );
							Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Detection - Success - Log Output" ), p.getName(), c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
							CraftManager.getInstance().addCraft( c, Movecraft.getInstance().getServer().getPlayer( data.getPlayername() ) );
						}
					}
				}


			} else if ( poll instanceof TranslationTask ) {
				//Process translation task

				TranslationTask task = ( TranslationTask ) poll;
				Player p = CraftManager.getInstance().getPlayerFromCraft( c );

				// Check that the craft hasn't been sneakily unpiloted
				if ( p != null ) {

					if ( task.getData().failed() ) {
						//The craft translation failed
						p.sendMessage( task.getData().getFailMessage() );
					} else {
						//The craft is clear to move, perform the block updates

						MapUpdateCommand[] updates = task.getData().getUpdates();
						boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates );

						if ( !failed ) {

							c.setBlockList( task.getData().getBlockList() );

							// Move entities
							for ( Entity pTest : c.getW().getEntities() ) {

								if ( MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {

									// Player is onboard this craft
		
									
									// If its an item, its probably garbage from the translation. Remove it  - todo: only remove broken block items, not other drops
									if(pTest.getType()==org.bukkit.entity.EntityType.DROPPED_ITEM) {
										pTest.remove();
									} else {
									// Smoother method, still work in progress
										if ( c.getType().isTryNudge() ) {
											pTest.setVelocity( pTest.getVelocity().add( new Vector( task.getData().getDx(), task.getData().getDy(), task.getData().getDz() ).normalize().multiply( 0.5 ) ) );
										} else {
											Vector velocity = pTest.getVelocity().clone();
											pTest.teleport( pTest.getLocation().add( task.getData().getDx(), task.getData().getDy(), task.getData().getDz() ) );
											pTest.setVelocity( velocity );
										}
									}
								}

							}
							c.setMinX( task.getData().getMinX() );
							c.setMinZ( task.getData().getMinZ() );
							c.setHitBox( task.getData().getHitbox() );


						} else {

							Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Translation - Craft collision" ) ) );

						}
					}

				}


			} else if ( poll instanceof RotationTask ) {
				// Process rotation task
				RotationTask task = ( RotationTask ) poll;
				Player p = CraftManager.getInstance().getPlayerFromCraft( c );

				// Check that the craft hasn't been sneakily unpiloted
				if ( p != null ) {

					if ( task.isFailed() ) {
						//The craft translation failed
						p.sendMessage( task.getFailMessage() );
					} else {
						MapUpdateCommand[] updates = task.getUpdates();
						boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates );

						if ( !failed ) {

							c.setBlockList( task.getBlockList() );

							Location originPoint = new Location( c.getW(), task.getOriginPoint().getX(), task.getOriginPoint().getY(), task.getOriginPoint().getZ() );
							// Move entities
							for ( Entity pTest : c.getW().getEntities() ) {

								if ( MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {

									// Player is onboard this craft
									Location playerLoc = pTest.getLocation();
									Location adjustedPLoc = playerLoc.subtract( originPoint );

									double[] rotatedCoords = MathUtils.rotateVec( task.getRotation(), adjustedPLoc.getX(), adjustedPLoc.getY() );
									Location rotatedPloc = new Location( c.getW(), rotatedCoords[0], playerLoc.getY(), rotatedCoords[1] );
									Location newPLoc = rotatedPloc.add( originPoint );

									Vector velocity = pTest.getVelocity().clone();
									pTest.teleport( newPLoc );
									pTest.setVelocity( velocity );
								}

							}

							c.setMinX( task.getMinX() );
							c.setMinZ( task.getMinZ() );
							c.setHitBox( task.getHitbox() );

						} else {

							Movecraft.getInstance().getLogger().log( Level.SEVERE, String.format( I18nSupport.getInternationalisedString( "Rotation - Craft Collision" ) ) );

						}
					}
				}
			}

			ownershipMap.remove( poll );
			clear( c );
		}
	}

	public void run() {
		clearAll();
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
