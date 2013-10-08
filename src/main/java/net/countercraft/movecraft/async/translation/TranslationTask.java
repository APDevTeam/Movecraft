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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BoundingBoxUtils;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;

import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class TranslationTask extends AsyncTask {
	private TranslationTaskData data;

	public TranslationTask( Craft c, TranslationTaskData data ) {
		super( c );
		this.data = data;
	}

	@Override
	public void excecute() {
		MovecraftLocation[] blocksList = data.getBlockList();

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
			int minX=getCraft().getMinX();
			int minZ=getCraft().getMinZ();
			
			// next figure out the water level by examining blocks next to the outer boundaries of the craft
			for(int posY=maxY; (posY>=minY)&&(waterLine==0); posY--) {
				int posX;
				int posZ;
				posZ=minZ-1;
				for(posX=minX-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
					if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posZ=maxZ+1;
				for(posX=minX-1; (posX <= maxX+1)&&(waterLine==0); posX++ ) {
					if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posX=minX-1;
				for(posZ=minZ; (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
					if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
				posX=maxX+1;
				for(posZ=minZ; (posZ <= maxZ)&&(waterLine==0); posZ++ ) {
					if(getCraft().getW().getBlockAt(posX, posY, posZ).getTypeId()==9) {
						waterLine=posY;
					}
				}
			}
			
			// now add all the air blocks found within the craft's hitbox immediately above the waterline and below to the craft blocks so they will be translated
			HashSet<MovecraftLocation> newHSBlockList=new HashSet<MovecraftLocation>(Arrays.asList(blocksList));
			for(int posY=waterLine+1; posY>=minY; posY--) {
				for(int posX=minX; posX<maxX; posX++) {
					for(int posZ=minZ; posZ<maxZ; posZ++) {
						if(hb[posX-minX]!=null) {
							if(hb[posX-minX][posZ-minZ]!=null) {
								if(getCraft().getW().getBlockAt(posX,posY,posZ).getTypeId()==0 && posY>hb[posX-minX][posZ-minZ][0] && posY<hb[posX-minX][posZ-minZ][1]) {
									MovecraftLocation l=new MovecraftLocation(posX,posY,posZ);
									newHSBlockList.add(l);
								}
							}
						}
					}
				}
			}
				
			blocksList=newHSBlockList.toArray(new MovecraftLocation[newHSBlockList.size()]);
		}
		
		MovecraftLocation[] newBlockList = new MovecraftLocation[blocksList.length];
		HashSet<MovecraftLocation> existingBlockSet = new HashSet<MovecraftLocation>( Arrays.asList( blocksList ) );
		Set<MapUpdateCommand> updateSet = new HashSet<MapUpdateCommand>();
		
		for ( int i = 0; i < blocksList.length; i++ ) {
			MovecraftLocation oldLoc = blocksList[i];
			MovecraftLocation newLoc = oldLoc.translate( data.getDx(), data.getDy(), data.getDz() );
			newBlockList[i] = newLoc;

			if ( newLoc.getY() >= data.getMaxHeight() && newLoc.getY() != oldLoc.getY() ) {
				fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit height limit" ) ) );
				break;
			} else if ( newLoc.getY() <= data.getMinHeight()  && newLoc.getY() != oldLoc.getY() ) {
				fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft hit minimum height limit" ) ) );
				break;
			}

			int testID = getCraft().getW().getBlockTypeIdAt( newLoc.getX(), newLoc.getY(), newLoc.getZ() );

			if(!waterCraft) { 
				if ( testID != 0 && !existingBlockSet.contains( newLoc ) ) {
					// New block is not air and is not part of the existing ship
					fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft is obstructed" ) ) );
					break;
				} else {
					int oldID = getCraft().getW().getBlockTypeIdAt( oldLoc.getX(), oldLoc.getY(), oldLoc.getZ() );

					updateSet.add( new MapUpdateCommand( blocksList[i], newBlockList[i], oldID ) );
				}
			} else {
			// let watercraft move through water
				if ( (testID != 0 && testID != 9) && !existingBlockSet.contains( newLoc ) ) {
					// New block is not air or water and is not part of the existing ship
					fail( String.format( I18nSupport.getInternationalisedString( "Translation - Failed Craft is obstructed" ) ) );
					break;
				} else {
					int oldID = getCraft().getW().getBlockTypeIdAt( oldLoc.getX(), oldLoc.getY(), oldLoc.getZ() );

					updateSet.add( new MapUpdateCommand( blocksList[i], newBlockList[i], oldID ) );
				}
			}

		}

		if ( !data.failed() ) {
			data.setBlockList( newBlockList );


			//Set blocks that are no longer craft to air
			List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( blocksList ), Arrays.asList( newBlockList ) );

			for ( MovecraftLocation l1 : airLocation ) {
				// for watercraft, fill blocks below the waterline with water
				if(!waterCraft) {
					updateSet.add( new MapUpdateCommand( l1, 0 ) );
				} else {
					if(l1.getY()<=waterLine) {
						updateSet.add( new MapUpdateCommand( l1, 9 ) );
					} else {
						updateSet.add( new MapUpdateCommand( l1, 0 ) );
					}
				}
			}

			data.setUpdates( updateSet.toArray( new MapUpdateCommand[1] ) );

			if ( data.getDy() != 0 ) {
				data.setHitbox( BoundingBoxUtils.translateBoundingBoxVertically( data.getHitbox(), data.getDy() ) );
			}

			data.setMinX( data.getMinX() + data.getDx() );
			data.setMinZ( data.getMinZ() + data.getDz() );
		}
	}

	private void fail( String message ) {
		data.setFailed( true );
		data.setFailMessage( message );
	}

	public TranslationTaskData getData() {
		return data;
	}
}
