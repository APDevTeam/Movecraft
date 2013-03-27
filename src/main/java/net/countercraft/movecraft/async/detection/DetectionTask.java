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

package net.countercraft.movecraft.async.detection;

import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.L18nSupport;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Stack;

public class DetectionTask extends AsyncTask {
	private MovecraftLocation startLocation;
	private int minSize, maxSize, maxX, maxY, maxZ, minX, minY, minZ;
	private Integer[] allowedBlocks, forbiddenBlocks;
	private World w;
	private Stack<MovecraftLocation> blockStack = new Stack<MovecraftLocation>();
	private HashSet<MovecraftLocation> blockList = new HashSet<MovecraftLocation>();
	private HashSet<MovecraftLocation> visited = new HashSet<MovecraftLocation>();
	private boolean failed;
	private String failMessage;
	private MovecraftLocation[] blockListFinal;
	private String playername;
	int[][][] hitBox;

	public DetectionTask( Craft c, MovecraftLocation startLocation, int minSize, int maxSize, Integer[] allowedBlocks, Integer[] forbiddenBlocks, String player, World w ) {
		super( c );
		this.startLocation = startLocation;
		this.minSize = minSize;
		this.maxSize = maxSize;
		this.allowedBlocks = allowedBlocks;
		this.forbiddenBlocks = forbiddenBlocks;
		playername = player;
		this.w = w;
	}

	@Override
	public void excecute() {
		//Recursive detection
		blockStack.push( startLocation );

		//detect all connected blocks
		do{
			detectBlock( blockStack.pop() );
		}
		while( !blockStack.isEmpty() );
		//Add each valid location to a HashSet

		//If exceeded max block limit return false

		if( blockList.size() < minSize ) {
			// Craft is too small
			failMessage = String.format( L18nSupport.getInternationalisedString( "Detection - Craft too small" ), minSize) ;
			failed = true;
		} else if ( blockList.size() > maxSize ) {
			// Craft is too big
			failMessage = String.format( L18nSupport.getInternationalisedString( "Detection - Craft too large" ), maxSize);
			failed = true;
		} else {

			blockListFinal = blockList.toArray( new MovecraftLocation[1] );

			if ( !failed ) {
				// Check if craft contains forbidden block
				for ( MovecraftLocation l : blockList ) {
					if ( isForbiddenBlock( w.getBlockTypeIdAt( l.getX(), l.getY(), l.getZ() ) ) ) {
						failMessage = String.format( L18nSupport.getInternationalisedString( "Detection - Forbidden block found" ) );
						failed = true;
					}
				}

				if ( !failed ) {
					// Form Polygonal bounding box
					int sizeX, sizeZ;
					sizeX = maxX - minX + 1;
					sizeZ = maxZ - minZ + 1;

					int[][][] polygonalBox = new int[sizeX][][];
					for ( int i = minX; i <= maxX; i++ ) {
						polygonalBox[i - minX] = new int[sizeZ][];

						for ( int j = minZ; j <= maxZ; j++ ) {
							int yMin = -1, yMax = -1;

							for ( MovecraftLocation l : blockList ) {
								if ( l.getX() == i && l.getZ() == j ) {
									if( yMax == -1 || l.getY() > yMax ){
										yMax = l.getY();
									}
									if( yMin == -1 || l.getY() < yMin ){
										yMin = l.getY();
									}

								}
							}

							polygonalBox[i - minX][j - minZ] = new int[2];
							polygonalBox[i - minX][j - minZ][0] = yMin;
							polygonalBox[i - minX][j - minZ][1] = yMax;
						}
					}

					hitBox = polygonalBox;
				}
			}
		}

	}

	private void detectBlock(MovecraftLocation location){
		int x = location.getX();
		int y = location.getY();
		int z = location.getZ();

		detectBlock(x + 1, y, z);
		detectBlock(x - 1, y, z);
		detectBlock(x, y + 1, z);
		detectBlock(x, y - 1, z);
		detectBlock(x, y, z + 1);
		detectBlock(x, y, z - 1);
		detectBlock(x + 1, y - 1, z);
		detectBlock(x - 1, y - 1, z);
		detectBlock(x, y - 1, z + 1);
		detectBlock(x, y - 1, z - 1);
		detectBlock(x + 1, y + 1, z);
		detectBlock(x - 1, y + 1, z);
		detectBlock(x, y + 1, z + 1);
		detectBlock(x, y + 1, z - 1);

	}

	private void detectBlock( int x, int y, int z ){
		MovecraftLocation workingLocation = new MovecraftLocation( x, y, z );
		if(!visited.contains( workingLocation )){
			visited.add( workingLocation );

			if(isAllowedBlock( w.getBlockTypeIdAt( x, y, z ) )){
				blockList.add(workingLocation);

				if(blockList.size() <= maxSize){

					blockStack.push( workingLocation );

					if ( maxX == 0 || workingLocation.getX() > maxX ){
						maxX = workingLocation.getX();
					}
					if( maxY == 0 || workingLocation.getY() > maxY ){
						maxY = workingLocation.getY();
					}
					if( maxZ == 0 || workingLocation.getZ() > maxZ ){
						maxZ = workingLocation.getZ();
					}
					if( minX == 0 || workingLocation.getX() < minX ){
						minX = workingLocation.getX();
					}
					if( minY == 0 || workingLocation.getY() < minY ){
						minY = workingLocation.getY();
					}
					if( minZ == 0 || workingLocation.getZ() < minZ ){
						minZ = workingLocation.getZ();
					}
				}else{
					return;
				}
			}
		}


	}

	private boolean isAllowedBlock( int test ){

		for( int i : allowedBlocks ){
			if( i == test ){
				return true;
			}
		}

		return test != 0;
	}

	private boolean isForbiddenBlock( int test ){

		for( int i : forbiddenBlocks ){
			if( i == test ){
				return true;
			}
		}

		return false;
	}

	public World getW() {
		return w;
	}

	public boolean isFailed() {
		return failed;
	}

	public String getFailMessage() {
		return failMessage;
	}

	public MovecraftLocation[] getBlockListFinal() {
		return blockListFinal;
	}

	public String getPlayername() {
		return playername;
	}

	public int[][][] getHitBox() {
		return hitBox;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinZ() {
		return minZ;
	}
}
