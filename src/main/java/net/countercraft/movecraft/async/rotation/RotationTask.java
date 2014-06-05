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

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BlockUtils;
import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;

import org.apache.commons.collections.ListUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;

public class RotationTask extends AsyncTask {
	private final MovecraftLocation originPoint;
	private boolean failed = false;
	private String failMessage;
	private MovecraftLocation[] blockList;    // used to be final, not sure why. Changed by Mark / Loraxe42
	private MapUpdateCommand[] updates;
	private EntityUpdateCommand[] entityUpdates;
	private int[][][] hitbox;
	private Integer minX, minZ;
	private final Rotation rotation;
	private final World w;
	private final boolean isSubCraft;

	public RotationTask( Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w ) {
		super( c );
		this.originPoint = originPoint;
		this.blockList = blockList;
		this.rotation = rotation;
		this.w = w;
		this.isSubCraft = false;
	}

	public RotationTask( Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w, boolean isSubCraft ) {
		super( c );
		this.originPoint = originPoint;
		this.blockList = blockList;
		this.rotation = rotation;
		this.w = w;
		this.isSubCraft = isSubCraft;
	}

	@Override
	public void excecute() {
		
		int waterLine=0;
		
		int [][][] hb=getCraft().getHitBox();
		
		// Determine craft borders
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
		Integer maxX=getCraft().getMinX()+hb.length;
		Integer maxZ=getCraft().getMinZ()+hb[0].length;  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
		minX=getCraft().getMinX();
		minZ=getCraft().getMinZ();

		int distX=maxX-minX;
		int distZ=maxZ-minZ; 
		
		// Load any chunks that you could possibly rotate into that are not loaded 
		for (int posX=minX-distZ;posX<=maxX+distZ;posX++) {
			for (int posZ=minZ-distX;posZ<=maxZ+distX;posZ++) {
				Chunk chunk=getCraft().getW().getBlockAt(posX,minY,posZ).getChunk();
				if (!chunk.isLoaded()) {
					chunk.load();
				}
			}
		}
		
		// blockedByWater=false means an ocean-going vessel
		boolean waterCraft=!getCraft().getType().blockedByWater();

		if (waterCraft) {
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
		HashSet<EntityUpdateCommand> entityUpdateSet = new HashSet<EntityUpdateCommand>();

		// make the centered block list, and check for a cruise control sign to reset to off
		for ( int i = 0; i < blockList.length; i++ ) {
			centeredBlockList[i] = blockList[i].subtract( originPoint );
			if(getCraft().getCruising()) {
				int blockID=w.getBlockAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ).getTypeId();
				if(blockID==63 || blockID==68) {
					Sign s=(Sign) w.getBlockAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() ).getState();
					if ( s.getLine( 0 ).equals( "Cruise: ON")) {
						s.setLine(0, "Cruise: OFF");
						s.update(true);
					}
				}
			}
		}
		getCraft().setCruising(false);

		for ( int i = 0; i < blockList.length; i++ ) {

			blockList[i] = MathUtils.rotateVec( rotation, centeredBlockList[i] ).add( originPoint );
			int typeID = w.getBlockTypeIdAt( blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() );
            
            Material testMaterial = w.getBlockAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ()).getType();
                    
            if (testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)){
               if (!checkChests(testMaterial, blockList[i], existingBlockSet)){
                    //prevent chests collision
                    failed = true;
					failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" )+" @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() );
                    break;
                }
            }

			if (!waterCraft) {
				if ( (typeID != 0 && typeID!=34) && !existingBlockSet.contains( blockList[i] ) ) {
					failed = true;
					failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" )+" @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() );
					break;
				} else {
					int id = w.getBlockTypeIdAt( originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ() );
					mapUpdates.add( new MapUpdateCommand( originalBlockList[i], blockList[i], id, rotation, getCraft() ) );
				} 
			} else {
				// allow watercraft to rotate through water
				if ( (typeID != 0 && typeID != 9 && typeID!=34) && !existingBlockSet.contains( blockList[i] ) ) {
					failed = true;
					failMessage = String.format( I18nSupport.getInternationalisedString( "Rotation - Craft is obstructed" )+" @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ() );
					break;
				} else {
					int id = w.getBlockTypeIdAt( originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ() );
					mapUpdates.add( new MapUpdateCommand( originalBlockList[i], blockList[i], id, rotation, getCraft() ) );
				} 
			}

		}

		if ( !failed ) {
			//rotate entities in the craft
			Location tOP = new Location( getCraft().getW(), originPoint.getX(), originPoint.getY(), originPoint.getZ() );
			
			List<Entity> eList=null;
			int numTries=0;
			
			while((eList==null)&&(numTries<100)) {
				try {
					eList=getCraft().getW().getEntities();
				}
				catch(java.util.ConcurrentModificationException e)
				{
					numTries++;
				}
			}
			Iterator<Entity> i=getCraft().getW().getEntities().iterator();
			while (i.hasNext()) {
				Entity pTest=i.next();
				if ( MathUtils.playerIsWithinBoundingPolygon( getCraft().getHitBox(), getCraft().getMinX(), getCraft().getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {
					if(pTest.getType()!=org.bukkit.entity.EntityType.DROPPED_ITEM ) {
						// Player is onboard this craft
						tOP.setX(tOP.getBlockX()+0.5);
						tOP.setZ(tOP.getBlockZ()+0.5);
						Location playerLoc = pTest.getLocation();
						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
							playerLoc.setX(getCraft().getPilotLockedX());
							playerLoc.setY(getCraft().getPilotLockedY());
							playerLoc.setZ(getCraft().getPilotLockedZ());
							}
						Location adjustedPLoc = playerLoc.subtract( tOP ); 

						double[] rotatedCoords = MathUtils.rotateVecNoRound( rotation, adjustedPLoc.getX(), adjustedPLoc.getZ() );
						Location rotatedPloc = new Location( getCraft().getW(), rotatedCoords[0], playerLoc.getY(), rotatedCoords[1] );
						Location newPLoc = rotatedPloc.add( tOP );

						newPLoc.setPitch(playerLoc.getPitch());
						float newYaw=playerLoc.getYaw();
						if(rotation==Rotation.CLOCKWISE) {
							newYaw=newYaw+90.0F;
							if(newYaw>=360.0F) {
								newYaw=newYaw-360.0F;
							}
						}
						if(rotation==Rotation.ANTICLOCKWISE) {
							newYaw=newYaw-90;
							if(newYaw<0.0F) {
								newYaw=newYaw+360.0F;
							}
						}
						newPLoc.setYaw(newYaw);

						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
							getCraft().setPilotLockedX(newPLoc.getX());
							getCraft().setPilotLockedY(newPLoc.getY());
							getCraft().setPilotLockedZ(newPLoc.getZ());
							}
						EntityUpdateCommand eUp=new EntityUpdateCommand(pTest.getLocation().clone(),newPLoc,pTest);
						entityUpdateSet.add(eUp);
						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
							getCraft().setPilotLockedX(newPLoc.getX());
							getCraft().setPilotLockedY(newPLoc.getY());
							getCraft().setPilotLockedZ(newPLoc.getZ());
						}
					} else {
					//	pTest.remove();   removed to test cleaner fragile item removal
					}
				}

			}
			
/*			//update player spawn locations if they spawned where the ship used to be
			for(Player p : Movecraft.getInstance().getServer().getOnlinePlayers()) {
				if(p.getBedSpawnLocation()!=null) {
					if( MathUtils.playerIsWithinBoundingPolygon( getCraft().getHitBox(), getCraft().getMinX(), getCraft().getMinZ(), MathUtils.bukkit2MovecraftLoc( p.getBedSpawnLocation() ) ) ) {
						Location spawnLoc = p.getBedSpawnLocation();
						Location adjustedPLoc = spawnLoc.subtract( tOP ); 

						double[] rotatedCoords = MathUtils.rotateVecNoRound( rotation, adjustedPLoc.getX(), adjustedPLoc.getZ() );
						Location rotatedPloc = new Location( getCraft().getW(), rotatedCoords[0], spawnLoc.getY(), rotatedCoords[1] );
						Location newBedSpawn = rotatedPloc.add( tOP );

						p.setBedSpawnLocation(newBedSpawn, true);
					}
				}
			}*/
			
			// Calculate air changes
			List<MovecraftLocation> airLocation = ListUtils.subtract( Arrays.asList( originalBlockList ), Arrays.asList( blockList ) );
			
			for ( MovecraftLocation l1 : airLocation ) {
				if(waterCraft) {
						// if its below the waterline, fill in with water. Otherwise fill in with air.
					if(l1.getY()<=waterLine) {
						mapUpdates.add( new MapUpdateCommand( l1, 9,null ) );
					} else {
						mapUpdates.add( new MapUpdateCommand( l1, 0,null ) );
					}
				} else {
					mapUpdates.add( new MapUpdateCommand( l1, 0,null ) );
				}
			}
			

			this.updates = mapUpdates.toArray( new MapUpdateCommand[1] );
			this.entityUpdates = entityUpdateSet.toArray( new EntityUpdateCommand[1] );

			maxX = null;
			maxZ = null;
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
			
			// if you rotated a subcraft, update the parent with the new blocks
			if(this.isSubCraft) {
				Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld( getCraft().getW() );
				for ( Craft craft : craftsInWorld ) {
					if ( BlockUtils.arrayContainsOverlap( craft.getBlockList(), originalBlockList ) && craft!=getCraft() ) {
						// found a parent craft
						List<MovecraftLocation> parentBlockList=ListUtils.subtract(Arrays.asList(craft.getBlockList()), Arrays.asList(originalBlockList));
						parentBlockList.addAll(Arrays.asList(blockList));
						craft.setBlockList(parentBlockList.toArray( new MovecraftLocation[1] ));

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

	public EntityUpdateCommand[] getEntityUpdates() {
		return entityUpdates;
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
    
	public boolean getIsSubCraft() {
		return isSubCraft;
	}

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc, HashSet<MovecraftLocation> existingBlockSet){
        Material testMaterial;
        MovecraftLocation aroundNewLoc;
        
        aroundNewLoc = newLoc.translate( 1, 0, 0);
        testMaterial = getCraft().getW().getBlockAt( aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)){
            if (!existingBlockSet.contains(aroundNewLoc)){
                return false;
            }
        }
        
        aroundNewLoc = newLoc.translate( -1, 0, 0);
        testMaterial = getCraft().getW().getBlockAt( aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)){
            if (!existingBlockSet.contains(aroundNewLoc)){
                return false;
            }
        }
        
        aroundNewLoc = newLoc.translate( 0, 0, 1);
        testMaterial = getCraft().getW().getBlockAt( aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)){
            if (!existingBlockSet.contains(aroundNewLoc)){
                return false;
            }
        }
        
        aroundNewLoc = newLoc.translate( 0, 0, -1);
        testMaterial = getCraft().getW().getBlockAt( aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)){
            if (!existingBlockSet.contains(aroundNewLoc)){
                return false;
            }
        }
        return true; 
    }
}
