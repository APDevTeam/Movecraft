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

package net.countercraft.movecraft.async.rotation;

import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.apache.commons.collections.ListUtils;
import org.bukkit.World;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RotationTask extends AsyncTask {
	private MovecraftLocation originPoint;
	private boolean failed = false;
	private String failMessage;
	private MovecraftLocation[] blockList;
	private MapUpdateCommand[] updates;
	private int[][][] hitbox;
	private Integer minX, minZ;
	private Rotation rotation;
	private World w;

	public RotationTask( Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w ) {
		super( c );
		this.originPoint = originPoint;
		this.blockList = blockList;
		this.rotation = rotation;
		this.w = w;
	}

	@Override
	public void excecute() {
		// Rotate the block set
		MovecraftLocation[] centeredBlockList = new MovecraftLocation[blockList.length];
		MovecraftLocation[] originalBlockList = blockList.clone();
		HashSet<MovecraftLocation> existingBlockSet = new HashSet<MovecraftLocation>( Arrays.asList( originalBlockList ) );
		Set<MapUpdateCommand> mapUpdates = new HashSet<MapUpdateCommand>();

		for ( int i = 0; i < blockList.length; i++ ) {
			centeredBlockList[i] = blockList[i].subtract( originPoint );
		}

		for ( int i = 0; i < blockList.length; i++ ) {

			blockList[i] = MathUtils.rotateVec( rotation, centeredBlockList[i] ).add( originPoint );

			if ( w.getBlockTypeIdAt( blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ) != 0 && !existingBlockSet.contains( blockList[i] ) ) {
				failed = true;
				failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" ) );
				break;
			} else {
				int id = w.getBlockTypeIdAt( originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ() );
				mapUpdates.add( new MapUpdateCommand( originalBlockList[i], blockList[i], id, rotation ) );


			}

		}

		if ( !failed ) {
			// Calculate air changes
			List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( originalBlockList ), Arrays.asList( blockList ) );

			for ( MovecraftLocation l1 : airLocation ) {
				mapUpdates.add( new MapUpdateCommand( l1, 0 ) );
			}

			this.updates = mapUpdates.toArray( new MapUpdateCommand[1] );

			Integer maxX = null, maxZ = null;
			minX = null;
			minZ = null;

			for ( MovecraftLocation l : blockList ) {
				if ( maxX == null || l.getX() > maxX ) {
					maxX = l.getX();
				}
				if ( maxZ == null || l.getZ() > maxZ ) {
					maxZ = l.getZ();
				}
				if ( minX == null || l.getX() < minX ) {
					minX = l.getX();
				}
				if ( minZ == null || l.getZ() < minZ ) {
					minZ = l.getZ();
				}
			}

			// Rerun the polygonal bounding formula for the newly formed craft
			int sizeX, sizeZ;
			sizeX = ( maxX - minX ) + 1;
			sizeZ = ( maxZ - minZ ) + 1;


			int[][][] polygonalBox = new int[sizeX][][];


			for ( MovecraftLocation l : blockList ) {
				if ( polygonalBox[l.getX() - minX] == null ) {
					polygonalBox[l.getX() - minX] = new int[sizeZ][];
				}

				int minY = 0, maxY = 0;

				if ( polygonalBox[l.getX() - minX][l.getZ() - minZ] == null ) {

					polygonalBox[l.getX() - minX][l.getZ() - minZ] = new int[2];
					polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
					polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();

				} else {
					minY = polygonalBox[l.getX() - minX][l.getZ() - minZ][0];
					maxY = polygonalBox[l.getX() - minX][l.getZ() - minZ][1];

					if ( l.getY() < minY ) {
						polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
					}
					if ( l.getY() > maxY ) {
						polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();
					}

				}


			}


			this.hitbox = polygonalBox;
		}
	}

	public MovecraftLocation getOriginPoint() {
		return originPoint;
	}

	public boolean isFailed() {
		return failed;
	}

	public String getFailMessage() {
		return failMessage;
	}

	public MovecraftLocation[] getBlockList() {
		return blockList;
	}

	public MapUpdateCommand[] getUpdates() {
		return updates;
	}

	public int[][][] getHitbox() {
		return hitbox;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinZ() {
		return minZ;
	}

	public Rotation getRotation() {
		return rotation;
	}

}
