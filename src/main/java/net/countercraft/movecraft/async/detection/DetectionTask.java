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
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BoundingBoxUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class DetectionTask extends AsyncTask {
	private final MovecraftLocation startLocation;
	private final Integer minSize;
	private final Integer maxSize;
	private Integer maxX;
	private Integer maxY;
	private Integer maxZ;
	private Integer minY;
	private final Stack<MovecraftLocation> blockStack = new Stack<MovecraftLocation>();
	private final HashSet<MovecraftLocation> blockList = new HashSet<MovecraftLocation>();
	private final HashSet<MovecraftLocation> visited = new HashSet<MovecraftLocation>();
	private final HashMap<Integer, Integer> blockTypeCount = new HashMap<Integer, Integer>();
	private final DetectionTaskData data;

	public DetectionTask( Craft c, MovecraftLocation startLocation, int minSize, int maxSize, Integer[] allowedBlocks, Integer[] forbiddenBlocks, Player player, World w ) {
		super( c );
		this.startLocation = startLocation;
		this.minSize = minSize;
		this.maxSize = maxSize;
		data = new DetectionTaskData( w, player, allowedBlocks, forbiddenBlocks );
	}

	@Override
	public void excecute() {

		blockStack.push( startLocation );

		do {
			detectSurrounding( blockStack.pop() );
		}
		while ( !blockStack.isEmpty() );

		if ( data.failed() ) {
			return;
		}

		if ( isWithinLimit( blockList.size(), minSize, maxSize ) ) {

			data.setBlockList( finaliseBlockList( blockList ) );

			HashMap<Integer, ArrayList<Double>> flyBlocks = ( HashMap<Integer, ArrayList<Double>> ) getCraft().getType().getFlyBlocks().clone();

			if ( confirmStructureRequirements( flyBlocks, blockTypeCount ) ) {

				data.setHitBox( BoundingBoxUtils.formBoundingBox( data.getBlockList(), data.getMinX(), maxX, data.getMinZ(), maxZ ) );

			}

		}

	}

	private void detectBlock( int x, int y, int z ) {

		MovecraftLocation workingLocation = new MovecraftLocation( x, y, z );

		if ( notVisited( workingLocation, visited ) ) {

			int testID = data.getWorld().getBlockTypeIdAt( x, y, z );

			if ( isForbiddenBlock( testID ) ) {

				fail( String.format( I18nSupport.getInternationalisedString( "Detection - Forbidden block found" ) ) );

			} else if ( isAllowedBlock( testID ) ) {
				//check for double chests
				if (testID==54) {
					boolean foundDoubleChest=false;
					if(data.getWorld().getBlockTypeIdAt( x-1, y, z )==54) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x+1, y, z )==54) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x, y, z-1 )==54) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x, y, z+1 )==54) {
						foundDoubleChest=true;
					}
                    if(foundDoubleChest) {
						fail( String.format( I18nSupport.getInternationalisedString( "Detection - ERROR: Double chest found" ) ) );						
					}
				}
                //check for double trapped chests
				if (testID==146) {
					boolean foundDoubleChest=false;
					if(data.getWorld().getBlockTypeIdAt( x-1, y, z )==146) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x+1, y, z )==146) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x, y, z-1 )==146) {
						foundDoubleChest=true;
					}
					if(data.getWorld().getBlockTypeIdAt( x, y, z+1 )==146) {
						foundDoubleChest=true;
					}
                    if(foundDoubleChest) {
						fail( String.format( I18nSupport.getInternationalisedString( "Detection - ERROR: Double chest found" ) ) );						
					}
				}
				addToBlockList( workingLocation );
				addToBlockCount( testID );

				if ( isWithinLimit( blockList.size(), 0, maxSize ) ) {

					addToDetectionStack( workingLocation );

					calculateBounds( workingLocation );

				}

			}
		}


	}

	private boolean isAllowedBlock( int test ) {

		for ( int i : data.getAllowedBlocks() ) {
			if ( i == test ) {
				return true;
			}
		}

		return false;
	}

	private boolean isForbiddenBlock( int test ) {

		for ( int i : data.getForbiddenBlocks() ) {
			if ( i == test ) {
				return true;
			}
		}

		return false;
	}

	public DetectionTaskData getData() {
		return data;
	}

	private boolean notVisited( MovecraftLocation l, HashSet<MovecraftLocation> locations ) {
		if ( locations.contains( l ) ) {
			return false;
		} else {
			locations.add( l );
			return true;
		}
	}

	private void addToBlockList( MovecraftLocation l ) {
		blockList.add( l );
	}

	private void addToDetectionStack( MovecraftLocation l ) {
		blockStack.push( l );
	}

	private void addToBlockCount( int id ) {
		Integer count = blockTypeCount.get( id );

		if ( count == null ) {
			count = 0;
		}

		blockTypeCount.put( id, count + 1 );
	}

	private void detectSurrounding( MovecraftLocation l ) {
		int x = l.getX();
		int y = l.getY();
		int z = l.getZ();

		for ( int xMod = -1; xMod < 2; xMod += 2 ) {

			for ( int yMod = -1; yMod < 2; yMod++ ) {

				detectBlock( x + xMod, y + yMod, z );

			}

		}

		for ( int zMod = -1; zMod < 2; zMod += 2 ) {

			for ( int yMod = -1; yMod < 2; yMod++ ) {

				detectBlock( x, y + yMod, z + zMod );

			}

		}

		for ( int yMod = -1; yMod < 2; yMod += 2 ) {

			detectBlock( x, y + yMod, z );

		}

	}

	private void calculateBounds( MovecraftLocation l ) {
		if ( maxX == null || l.getX() > maxX ) {
			maxX = l.getX();
		}
		if ( maxY == null || l.getY() > maxY ) {
			maxY = l.getY();
		}
		if ( maxZ == null || l.getZ() > maxZ ) {
			maxZ = l.getZ();
		}
		if ( data.getMinX() == null || l.getX() < data.getMinX() ) {
			data.setMinX( l.getX() );
		}
		if ( minY == null || l.getY() < minY ) {
			minY = l.getY();
		}
		if ( data.getMinZ() == null || l.getZ() < data.getMinZ() ) {
			data.setMinZ( l.getZ() );
		}
	}

	private boolean isWithinLimit( int size, int min, int max ) {
		if ( size < min ) {
			fail( String.format( I18nSupport.getInternationalisedString( "Detection - Craft too small" ), min ) );
			return false;
		} else if ( size > max ) {
			fail( String.format( I18nSupport.getInternationalisedString( "Detection - Craft too large" ), max ) );
			return false;
		} else {
			return true;
		}

	}

	private MovecraftLocation[] finaliseBlockList( HashSet<MovecraftLocation> blockSet ) {
		return blockSet.toArray( new MovecraftLocation[1] );
	}

	private boolean confirmStructureRequirements( HashMap<Integer, ArrayList<Double>> flyBlocks, HashMap<Integer, Integer> countData ) {
		for ( Integer i : flyBlocks.keySet() ) {
			Integer numberOfBlocks = countData.get( i );

			if ( numberOfBlocks == null ) {
				numberOfBlocks = 0;
			}

			float blockPercentage = ( ( ( float ) numberOfBlocks / data.getBlockList().length ) * 100 );
			Double minPercentage = flyBlocks.get( i ).get( 0 );
			Double maxPercentage = flyBlocks.get( i ).get( 1 );

			if ( blockPercentage < minPercentage ) {

				fail( String.format( I18nSupport.getInternationalisedString( "Detection - Failed - Not enough flyblock" ), i, minPercentage, blockPercentage ) );
				return false;

			} else if ( blockPercentage > maxPercentage ) {

				fail( String.format( I18nSupport.getInternationalisedString( "Detection - Failed - Too much flyblock" ), i, maxPercentage, blockPercentage ) );
				return false;

			}
		}

		return true;
	}

	private void fail( String message ) {
		data.setFailed( true );
		data.setFailMessage( message );
	}
}
