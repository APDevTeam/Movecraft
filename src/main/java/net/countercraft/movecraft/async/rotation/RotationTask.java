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
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RotationTask extends AsyncTask {
	private final MovecraftLocation originPoint;
	private boolean failed = false;
	private String failMessage;
	private MovecraftLocation[] blockList;    // used to be final, not sure why. Changed by Mark / Loraxe42
	private MapUpdateCommand[] updates;
	private int[][][] hitbox;
	private Integer minX, minZ;
	private final Rotation rotation;
	private final World w;

	public RotationTask( Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w ) {
		super( c );
		this.originPoint = originPoint;
		this.blockList = blockList;
		this.rotation = rotation;
		this.w = w;
	}

	@Override
	public void excecute() {
		
		// canfly=false means an ocean-going vessel
		boolean waterCraft=!getCraft().getType().canFly();
		int waterLine=0;
		
		if (waterCraft) {
			int [][][] hb=getCraft().getHitBox();
			
			// start by finding the minimum and maximum y coord
			int minY=65535;
			int maxY=-65535;
			for (int [][] i1 : hb) {
				for (int [] i2 : i1) {
					if(i2!=null) {
						if(i2[0]<minY) {
							minY=i2[0];
						}
						if(i2[1]>maxY) {
							maxY=i2[1];
						}
					}
				}
			}
			int maxX=getCraft().getMinX()+hb.length;
			int maxZ=getCraft().getMinZ()+hb[0].length;  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
			
			// next figure out the water level by examining blocks next to the outer boundaries of the craft
			for(int posY=maxY; (posY>=minY)&&(waterLine==0); posY--) {
				int posX;
				int posZ;
				posZ=getCraft().getMinZ()-1;
				for(posX=getCraft().getMinX()-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
					if(w.getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posZ=maxZ+1;
				for(posX=getCraft().getMinX()-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
					if(w.getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posX=getCraft().getMinX()-1;
				for(posZ=getCraft().getMinZ(); (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
					if(w.getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posX=maxX+1;
				for(posZ=getCraft().getMinZ(); (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
					if(w.getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
			}
			
			// now add all the air blocks found within the crafts borders below the waterline to the craft blocks so they will be rotated
			HashSet<MovecraftLocation> newHSBlockList=new HashSet<MovecraftLocation>(Arrays.asList(blockList));
			for(int posY=waterLine; posY>=minY; posY--) {
				for(int posX=getCraft().getMinX(); posX<=maxX; posX++) {
					for(int posZ=getCraft().getMinZ(); posZ<=maxZ; posZ++) {
						if(w.getBlockAt(posX,posY,posZ).getTypeId()==0) {
							MovecraftLocation l=new MovecraftLocation(posX,posY,posZ);
							newHSBlockList.add(l);
						}
					}
				}
			}
			blockList=newHSBlockList.toArray(new MovecraftLocation[newHSBlockList.size()]);
		}
		
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

			if (!waterCraft) {
				if ( w.getBlockTypeIdAt( blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ) != 0 && !existingBlockSet.contains( blockList[i] ) ) {
					failed = true;
					failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" ) );
					break;
				} else {
					int id = w.getBlockTypeIdAt( originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ() );
					mapUpdates.add( new MapUpdateCommand( originalBlockList[i], blockList[i], id, rotation ) );
				} 
			} else {
				// allow watercraft to rotate through water
				if ( (w.getBlockTypeIdAt( blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ) != 0 && w.getBlockTypeIdAt( blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ) != 9) && !existingBlockSet.contains( blockList[i] ) ) {
					failed = true;
					failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" ) );
					break;
				} else {
					int id = w.getBlockTypeIdAt( originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ() );
					mapUpdates.add( new MapUpdateCommand( originalBlockList[i], blockList[i], id, rotation ) );
				} 
			}

		}

		if ( !failed ) {
			// Calculate air changes
			List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( originalBlockList ), Arrays.asList( blockList ) );
			
			for ( MovecraftLocation l1 : airLocation ) {
				if(waterCraft) {
						// if its below the waterline, fill in with water. Otherwise fill in with air.
					if(l1.getY()<=waterLine) {
						mapUpdates.add( new MapUpdateCommand( l1, 9 ) );
					} else {
						mapUpdates.add( new MapUpdateCommand( l1, 0 ) );
					}
				} else {
					mapUpdates.add( new MapUpdateCommand( l1, 0 ) );
				}
			}
			

			this.updates = mapUpdates.toArray( new MapUpdateCommand[1] );

			Integer maxX = null;
			Integer maxZ = null;
			minX = null;
			minZ = null;
			
			int maxY, minY;

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
