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
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class AsyncManager extends BukkitRunnable {
	private static AsyncManager instance = new AsyncManager();
	private HashMap<AsyncTask, Craft> ownershipMap = new HashMap<AsyncTask, Craft>();
	private BlockingQueue<AsyncTask> finishedAlgorithms = new LinkedBlockingQueue<AsyncTask>();

	public static AsyncManager getInstance() {
		return instance;
	}

	private AsyncManager() {
	}

	public void submitTask( AsyncTask task, Craft c ) {
		if ( !c.isProcessing() ) {
			c.setProcessing( true );
			ownershipMap.put( task, c );
			task.runTaskAsynchronously( Movecraft.getInstance() );
		}
	}

	public void submitCompletedTask( AsyncTask task ) {
		finishedAlgorithms.add( task );
	}

	public void processAlgorithmQueue() {
		int runLength = 10;
		int queueLength = finishedAlgorithms.size();

		runLength = Math.min( runLength, queueLength );

		for ( int i = 0; i < runLength; i++ ) {
			AsyncTask poll = finishedAlgorithms.poll();
			Craft c = ownershipMap.get( poll );

			if ( poll instanceof DetectionTask ) {
				// Process detection task

				DetectionTask task = ( DetectionTask ) poll;

				Player p = Movecraft.getInstance().getServer().getPlayer( task.getPlayername() );
				Craft pCraft = CraftManager.getInstance().getCraftByPlayer( p );

				if ( pCraft != null ) {
					//Player is already controlling a craft
					p.sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed - Already commanding a craft" ) ) );
				} else {
					if ( task.isFailed() ) {
						Movecraft.getInstance().getServer().getPlayer( task.getPlayername() ).sendMessage( task.getFailMessage() );
					} else {
						Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld( c.getW() );
						boolean failed = false;

						if ( craftsInWorld != null ) {
							for ( Craft craft : craftsInWorld ) {

								if ( BlockUtils.arrayContainsOverlap( craft.getBlockList(), task.getBlockListFinal() ) ) {
									Movecraft.getInstance().getServer().getPlayer( task.getPlayername() ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Failed Craft is already being controlled" ) ) );
									failed = true;
								}

							}
						}
						if ( !failed ) {
							c.setBlockList( task.getBlockListFinal() );
							c.setHitBox( task.getHitBox() );
							c.setMinX( task.getMinX() );
							c.setMinZ( task.getMinZ() );

							Movecraft.getInstance().getServer().getPlayer( task.getPlayername() ).sendMessage( String.format( I18nSupport.getInternationalisedString( "Detection - Successfully piloted craft" ) ) );
							Movecraft.getInstance().getLogger().log( Level.INFO, String.format( I18nSupport.getInternationalisedString( "Detection - Success - Log Output" ), p.getDisplayName(), c.getType().getCraftName(), c.getBlockList().length, c.getMinX(), c.getMinZ() ) );
							CraftManager.getInstance().addCraft( c, Movecraft.getInstance().getServer().getPlayer( task.getPlayername() ) );
						}
					}
				}


			} else if ( poll instanceof TranslationTask ) {
				//Process translation task

				TranslationTask task = ( TranslationTask ) poll;
				Player p = CraftManager.getInstance().getPlayerFromCraft( c );

				// Check that the craft hasn't been sneakily unpiloted
				if ( p != null ) {

					if ( task.isFailed() ) {
						//The craft translation failed
						p.sendMessage( task.getFailMessage() );
					} else {
						//The craft is clear to move, perform the block updates

						MapUpdateCommand[] updates = task.getUpdates();
						boolean failed = MapUpdateManager.getInstance().addWorldUpdate( c.getW(), updates );

						if ( !failed ) {

							c.setBlockList( task.getNewBlockList() );

							// Move entities
							for ( Entity pTest : c.getW().getEntities() ) {

								if ( MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {

									// Player is onboard this craft

									// Smoother method, still work in progress
									if ( c.getType().isTryNudge() ) {
										pTest.setVelocity( pTest.getVelocity().add( new Vector( task.getDx(), task.getDy(), task.getDz() ).normalize().multiply( 0.5 ) ) );
									} else {
										Vector velocity = pTest.getVelocity().clone();
										pTest.teleport( pTest.getLocation().add( task.getDx(), task.getDy(), task.getDz() ) );
										pTest.setVelocity( velocity );
									}
								}

							}
							c.setMinX( task.getMinX() );
							c.setMinZ( task.getMinZ() );
							c.setHitBox( task.getHitbox() );


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

							// Move entities
							for ( Entity pTest : c.getW().getEntities() ) {

								if ( MathUtils.playerIsWithinBoundingPolygon( c.getHitBox(), c.getMinX(), c.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {

									// Player is onboard this craft
									MovecraftLocation originPoint = task.getOriginPoint();
									MovecraftLocation playerLoc = MathUtils.bukkit2MovecraftLoc( pTest.getLocation() );
									MovecraftLocation adjustedPLoc = playerLoc.subtract( originPoint );

									MovecraftLocation newPLoc = MathUtils.rotateVec( task.getRotation(), adjustedPLoc ).add( originPoint );

									Vector velocity = pTest.getVelocity().clone();
									pTest.teleport( new Location( pTest.getWorld(), newPLoc.getX(), newPLoc.getY(), newPLoc.getZ() ) );
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
			c.setProcessing( false );

		}
	}

	public void run() {
		processAlgorithmQueue();
	}
}
