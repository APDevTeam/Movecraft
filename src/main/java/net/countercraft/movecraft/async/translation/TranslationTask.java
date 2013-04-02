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

package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.apache.commons.collections.ListUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TranslationTask extends AsyncTask {
	private int dx, dy, dz;
	private boolean failed = false;
	private String failMessage;
	private MovecraftLocation[] newBlockList;
	private MapUpdateCommand[] updates;
	private int[][][] hitbox;
	private int minX, minZ, heightLimit, minHeightLimit;

	public TranslationTask( Craft c, int dx, int dy, int dz, int[][][] hitBox, int minX, int minZ, int heightLimit, int minHeightLimit ) {
		super( c );
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.hitbox = hitBox;
		this.minX = minX;
		this.minZ = minZ;
		this.heightLimit = heightLimit;
                this.minHeightLimit=minHeightLimit;
	}

	@Override
	public void excecute() {
		MovecraftLocation[] blocksList = getCraft().getBlockList();
		MovecraftLocation[] newBlockList = new MovecraftLocation[blocksList.length];
		HashSet<MovecraftLocation> existingBlockSet = new HashSet<MovecraftLocation>( Arrays.asList( blocksList ) );
		Set<MapUpdateCommand> updateSet = new HashSet<MapUpdateCommand>();

		for ( int i = 0; i < blocksList.length; i++ ) {
			MovecraftLocation oldLoc = blocksList[i];
			MovecraftLocation newLoc = oldLoc.translate( dx, dy, dz );
			newBlockList[i] = newLoc;

			if ( newLoc.getY() >= heightLimit ) {
				failed = true;
				failMessage = String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit height limit" ) );
				break;        
			} else if ( newLoc.getY() <= minHeightLimit ) {   
				failed = true;
                                failMessage = String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit minimum height limit" ) );
				break;
			}

			int testID = getCraft().getW().getBlockTypeIdAt( newLoc.getX(), newLoc.getY(), newLoc.getZ() );


			if ( testID != 0 && !existingBlockSet.contains( newLoc ) ) {
				// New block is not air and is not part of the existing ship
				failed = true;
				failMessage = String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft is obstructed" ) );
				break;
			} else {
				int oldID = getCraft().getW().getBlockTypeIdAt( oldLoc.getX(), oldLoc.getY(), oldLoc.getZ() );

				updateSet.add( new MapUpdateCommand( blocksList[i], newBlockList[i], oldID ) );


			}

		}

		if ( !failed ) {
			this.newBlockList = newBlockList;


			//Set blocks that are no longer craft to air
			List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( blocksList ), Arrays.asList( newBlockList ) );

			for ( MovecraftLocation l1 : airLocation ) {
				updateSet.add( new MapUpdateCommand( l1, 0 ) );
			}

			this.updates = updateSet.toArray( new MapUpdateCommand[1] );

			if ( dy != 0 ) {

				int[][][] newHitbox = new int[hitbox.length][][];

				for ( int x = 0; x < hitbox.length; x++ ) {
					newHitbox[x] = new int[hitbox[x].length][];

					for ( int z = 0; z < hitbox[x].length; z++ ) {
						try {

							newHitbox[x][z] = new int[2];
							newHitbox[x][z][0] = hitbox[x][z][0] + dy;
							newHitbox[x][z][1] = hitbox[x][z][1] + dy;

						} catch ( NullPointerException e ) {
							continue;
						}

					}

				}

				this.hitbox = newHitbox;

			}

			this.minX = minX + dx;
			this.minZ = minZ + dz;
		}
	}

	public MovecraftLocation[] getNewBlockList() {
		return newBlockList;
	}

	public MapUpdateCommand[] getUpdates() {
		return updates;
	}

	public String getFailMessage() {
		return failMessage;
	}

	public boolean isFailed() {
		return failed;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinZ() {
		return minZ;
	}

	public int[][][] getHitbox() {
		return hitbox;
	}

	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

	public int getDz() {
		return dz;
	}
}
