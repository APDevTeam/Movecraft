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

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.BlockUtils;
import net.countercraft.movecraft.utils.EntityUpdateCommand;
import net.countercraft.movecraft.utils.MapUpdateCommand;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.TownyWorldHeightLimits;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import org.apache.commons.collections.ListUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RotationTask extends AsyncTask {
    private final MovecraftLocation originPoint;
    private final Rotation rotation;
    private final World w;
    private final boolean isSubCraft;
    private boolean failed = false;
    private String failMessage;
    private MovecraftLocation[] blockList;    // used to be final, not sure why. Changed by Mark / Loraxe42
    private MapUpdateCommand[] updates;
    private EntityUpdateCommand[] entityUpdates;
    private int[][][] hitbox;
    private Integer minX, minZ;
    private HashMap<MapUpdateCommand, Long> scheduledBlockChanges;

    public RotationTask(Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w) {
        super(c);
        this.originPoint = originPoint;
        this.blockList = blockList;
        this.rotation = rotation;
        this.w = w;
        this.isSubCraft = false;
    }

    public RotationTask(Craft c, MovecraftLocation originPoint, MovecraftLocation[] blockList, Rotation rotation, World w, boolean isSubCraft) {
        super(c);
        this.originPoint = originPoint;
        this.blockList = blockList;
        this.rotation = rotation;
        this.w = w;
        this.isSubCraft = isSubCraft;
    }

    @Override
    public void excecute() {

        int waterLine = 0;

        int[][][] hb = getCraft().getHitBox();
        if (hb == null)
            return;

        // Determine craft borders
        int minY = 65535;
        int maxY = -65535;
        for (int[][] i1 : hb) {
            for (int[] i2 : i1) {
                if (i2 != null) {
                    if (i2[0] < minY) {
                        minY = i2[0];
                    }
                    if (i2[1] > maxY) {
                        maxY = i2[1];
                    }
                }
            }
        }
        Integer maxX = getCraft().getMinX() + hb.length;
        Integer maxZ = getCraft().getMinZ() + hb[0].length;  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
        minX = getCraft().getMinX();
        minZ = getCraft().getMinZ();

        int distX = maxX - minX;
        int distZ = maxZ - minZ;

        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(getCraft());

        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            failed = true;
            failMessage = I18nSupport.getInternationalisedString("Craft is disabled!");
        }

        // blockedByWater=false means an ocean-going vessel
        boolean waterCraft = !getCraft().getType().blockedByWater();

        if (waterCraft) {
            // next figure out the water level by examining blocks next to the outer boundaries of the craft
            for (int posY = maxY; (posY >= minY) && (waterLine == 0); posY--) {
                int posX;
                int posZ;
                posZ = getCraft().getMinZ() - 1;
                for (posX = getCraft().getMinX() - 1; (posX <= maxX + 1) && (waterLine == 0); posX++) {
                    if (w.getBlockAt(posX, posY, posZ).getTypeId() == 9) {
                        waterLine = posY;
                    }
                }
                posZ = maxZ + 1;
                for (posX = getCraft().getMinX() - 1; (posX <= maxX + 1) && (waterLine == 0); posX++) {
                    if (w.getBlockAt(posX, posY, posZ).getTypeId() == 9) {
                        waterLine = posY;
                    }
                }
                posX = getCraft().getMinX() - 1;
                for (posZ = getCraft().getMinZ(); (posZ <= maxZ) && (waterLine == 0); posZ++) {
                    if (w.getBlockAt(posX, posY, posZ).getTypeId() == 9) {
                        waterLine = posY;
                    }
                }
                posX = maxX + 1;
                for (posZ = getCraft().getMinZ(); (posZ <= maxZ) && (waterLine == 0); posZ++) {
                    if (w.getBlockAt(posX, posY, posZ).getTypeId() == 9) {
                        waterLine = posY;
                    }
                }
            }

            // now add all the air blocks found within the crafts borders below the waterline to the craft blocks so they will be rotated
            HashSet<MovecraftLocation> newHSBlockList = new HashSet<>(Arrays.asList(blockList));
            for (int posY = waterLine; posY >= minY; posY--) {
                for (int posX = getCraft().getMinX(); posX <= maxX; posX++) {
                    for (int posZ = getCraft().getMinZ(); posZ <= maxZ; posZ++) {
                        if (w.getBlockAt(posX, posY, posZ).getTypeId() == 0) {
                            MovecraftLocation l = new MovecraftLocation(posX, posY, posZ);
                            newHSBlockList.add(l);
                        }
                    }
                }
            }
            blockList = newHSBlockList.toArray(new MovecraftLocation[newHSBlockList.size()]);
        }

        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = getCraft().getType().getFuelBurnRate();
        if (fuelBurnRate != 0.0 && !getCraft().getSinking()) {
            if (getCraft().getBurningFuel() < fuelBurnRate) {
                Block fuelHolder = null;
                for (MovecraftLocation bTest : blockList) {
                    Block b = getCraft().getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
                    if (b.getTypeId() == 61) {
                        InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                        if (inventoryHolder.getInventory().contains(263) || inventoryHolder.getInventory().contains(173)) {
                            fuelHolder = b;
                        }
                    }
                }
                if (fuelHolder == null) {
                    failed = true;
                    failMessage = I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel");
                } else {
                    InventoryHolder inventoryHolder = (InventoryHolder) fuelHolder.getState();
                    if (inventoryHolder.getInventory().contains(263)) {
                        ItemStack iStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(263));
                        int amount = iStack.getAmount();
                        if (amount == 1) {
                            inventoryHolder.getInventory().remove(iStack);
                        } else {
                            iStack.setAmount(amount - 1);
                        }
                        getCraft().setBurningFuel(getCraft().getBurningFuel() + 7.0);
                    } else {
                        ItemStack iStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(173));
                        int amount = iStack.getAmount();
                        if (amount == 1) {
                            inventoryHolder.getInventory().remove(iStack);
                        } else {
                            iStack.setAmount(amount - 1);
                        }
                        getCraft().setBurningFuel(getCraft().getBurningFuel() + 79.0);

                    }
                }
            } else {
                getCraft().setBurningFuel(getCraft().getBurningFuel() - fuelBurnRate);
            }
        }


        // Rotate the block set
        MovecraftLocation[] centeredBlockList = new MovecraftLocation[blockList.length];
        MovecraftLocation[] originalBlockList = blockList.clone();
        HashSet<MovecraftLocation> existingBlockSet = new HashSet<>(Arrays.asList(originalBlockList));
        Set<MapUpdateCommand> mapUpdates = new HashSet<>();
        HashSet<EntityUpdateCommand> entityUpdateSet = new HashSet<>();

        boolean townyEnabled = Movecraft.getInstance().getTownyPlugin() != null;
        Set<TownBlock> townBlockSet = new HashSet<>();
        TownyWorld townyWorld = null;
        TownyWorldHeightLimits townyWorldHeightLimits = null;

        if (townyEnabled && Settings.TownyBlockMoveOnSwitchPerm) {
            townyWorld = TownyUtils.getTownyWorld(getCraft().getW());
            if (townyWorld != null) {
                townyEnabled = townyWorld.isUsingTowny();
                if (townyEnabled) townyWorldHeightLimits = TownyUtils.getWorldLimits(getCraft().getW());
            }
        } else {
            townyEnabled = false;
        }

        // if a subcraft, find the parent craft. If not a subcraft, it is it's own parent
        Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
        Craft parentCraft = getCraft();
        for (Craft craft : craftsInWorld) {
            if (BlockUtils.arrayContainsOverlap(craft.getBlockList(), originalBlockList) && craft != getCraft()) {
                // found a parent craft
//				if(craft.isNotProcessing()==false) {
//					failed=true;
//					failMessage = String.format( I18nSupport.getInternationalisedString( "Parent Craft is busy" ));
//					return;
//				} else {
//					craft.setProcessing(true); // prevent the parent craft from moving or updating until the subcraft is done
                parentCraft = craft;
//				}
            }
        }


        int craftMinY = 0;
        int craftMaxY = 0;
        // make the centered block list
        for (int i = 0; i < blockList.length; i++) {
            centeredBlockList[i] = blockList[i].subtract(originPoint);

        }

        for (int i = 0; i < blockList.length; i++) {

            blockList[i] = MathUtils.rotateVec(rotation, centeredBlockList[i]).add(originPoint);
            int typeID = w.getBlockTypeIdAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());

            Material testMaterial = w.getBlockAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ()).getType();

            if (testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) {
                if (!checkChests(testMaterial, blockList[i], existingBlockSet)) {
                    //prevent chests collision
                    failed = true;
                    failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                    break;
                }
            }
            Location plugLoc = new Location(w, blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
            if (craftPilot != null) {
                // See if they are permitted to build in the area, if WorldGuard integration is turned on
                if (Movecraft.getInstance().getWorldGuardPlugin() != null && Settings.WorldGuardBlockMoveOnBuildPerm) {
                    if (!Movecraft.getInstance().getWorldGuardPlugin().canBuild(craftPilot, plugLoc)) {
                        failed = true;
                        failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Player is not permitted to build in this WorldGuard region") + " @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                        break;
                    }
                }
            }

            Player p;
            if (craftPilot == null) {
                p = getCraft().getNotificationPlayer();
            } else {
                p = craftPilot;
            }
            if (p != null) {
                if (Movecraft.getInstance().getWorldGuardPlugin() != null && Movecraft.getInstance().getWGCustomFlagsPlugin() != null && Settings.WGCustomFlagsUsePilotFlag) {
                    LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(p);
                    WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                    if (!WGCFU.validateFlag(plugLoc, Movecraft.FLAG_ROTATE, lp)) {
                        failed = true;
                        failMessage = String.format(I18nSupport.getInternationalisedString("WGCustomFlags - Rotation Failed") + " @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                        break;
                    }
                }

                if (townyEnabled) {
                    TownBlock townBlock = TownyUtils.getTownBlock(plugLoc);
                    if (townBlock != null && !townBlockSet.contains(townBlock)) {
                        if (TownyUtils.validateCraftMoveEvent(p, plugLoc, townyWorld)) {
                            townBlockSet.add(townBlock);
                        } else {
                            int y = plugLoc.getBlockY();
                            boolean oChange = false;
                            if (craftMinY > y) {
                                craftMinY = y;
                                oChange = true;
                            }
                            if (craftMaxY < y) {
                                craftMaxY = y;
                                oChange = true;
                            }
                            if (oChange) {
                                Town town = TownyUtils.getTown(townBlock);
                                if (town != null) {
                                    Location locSpawn = TownyUtils.getTownSpawn(townBlock);
                                    if (locSpawn != null) {
                                        if (!townyWorldHeightLimits.validate(y, locSpawn.getBlockY())) {
                                            failed = true;
                                        }
                                    } else {
                                        failed = true;
                                    }
                                    if (failed) {
                                        if (Movecraft.getInstance().getWorldGuardPlugin() != null && Movecraft.getInstance().getWGCustomFlagsPlugin() != null && Settings.WGCustomFlagsUsePilotFlag) {
                                            LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(p);
                                            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(plugLoc.getWorld()).getApplicableRegions(plugLoc);
                                            if (regions.size() != 0) {
                                                WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                                                if (WGCFU.validateFlag(plugLoc, Movecraft.FLAG_ROTATE, lp)) {
                                                    failed = false;
                                                }
                                            }
                                        }
                                    }
                                    if (failed) {
                                        failMessage = String.format(I18nSupport.getInternationalisedString("Towny - Rotation Failed") + " %s @ %d,%d,%d", town.getName(), blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!waterCraft) {
                if ((typeID != 0 && typeID != 34) && !existingBlockSet.contains(blockList[i])) {
                    failed = true;
                    failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                    break;
                } else {
                    int id = w.getBlockTypeIdAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ());
                    byte data = w.getBlockAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ()).getData();
                    int currentID = w.getBlockTypeIdAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                    byte currentData = w.getBlockAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ()).getData();
                    if (BlockUtils.blockRequiresRotation(id)) {
                        data = BlockUtils.rotate(data, id, rotation);
                    }
                    mapUpdates.add(new MapUpdateCommand(originalBlockList[i], currentID, currentData, blockList[i], id, data, rotation, parentCraft));
                }
            } else {
                // allow watercraft to rotate through water
                if ((typeID != 0 && typeID != 9 && typeID != 34) && !existingBlockSet.contains(blockList[i])) {
                    failed = true;
                    failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                    break;
                } else {
                    int id = w.getBlockTypeIdAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ());
                    byte data = w.getBlockAt(originalBlockList[i].getX(), originalBlockList[i].getY(), originalBlockList[i].getZ()).getData();
                    int currentID = w.getBlockTypeIdAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ());
                    byte currentData = w.getBlockAt(blockList[i].getX(), blockList[i].getY(), blockList[i].getZ()).getData();
                    if (BlockUtils.blockRequiresRotation(id)) {
                        data = BlockUtils.rotate(data, id, rotation);
                    }
                    mapUpdates.add(new MapUpdateCommand(originalBlockList[i], currentID, currentData, blockList[i], id, data, rotation, parentCraft));
                }
            }

        }

        if (!failed) {
            //rotate entities in the craft
            Location tOP = new Location(getCraft().getW(), originPoint.getX(), originPoint.getY(), originPoint.getZ());

            List<Entity> eList = null;
            int numTries = 0;

            while ((eList == null) && (numTries < 100)) {
                try {
                    eList = getCraft().getW().getEntities();
                } catch (java.util.ConcurrentModificationException e) {
                    numTries++;
                }
            }
            for (Entity pTest : getCraft().getW().getEntities()) {
                //				if ( MathUtils.playerIsWithinBoundingPolygon( getCraft().getHitBox(), getCraft().getMinX(), getCraft().getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {
                if (MathUtils.locIsNearCraftFast(getCraft(), MathUtils.bukkit2MovecraftLoc(pTest.getLocation()))) {
                    if (pTest.getType() != EntityType.DROPPED_ITEM) {
                        // Player is onboard this craft
                        tOP.setX(tOP.getBlockX() + 0.5);
                        tOP.setZ(tOP.getBlockZ() + 0.5);
                        Location playerLoc = pTest.getLocation();
// direct control no longer locks the player in place						
//						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
//							playerLoc.setX(getCraft().getPilotLockedX());
//							playerLoc.setY(getCraft().getPilotLockedY());
//							playerLoc.setZ(getCraft().getPilotLockedZ());
//							}
                        Location adjustedPLoc = playerLoc.subtract(tOP);

                        double[] rotatedCoords = MathUtils.rotateVecNoRound(rotation, adjustedPLoc.getX(), adjustedPLoc.getZ());
                        Location rotatedPloc = new Location(getCraft().getW(), rotatedCoords[0], playerLoc.getY(), rotatedCoords[1]);
                        Location newPLoc = rotatedPloc.add(tOP);

                        newPLoc.setPitch(playerLoc.getPitch());
                        float newYaw = playerLoc.getYaw();
                        if (rotation == Rotation.CLOCKWISE) {
                            newYaw = newYaw + 90.0F;
                            if (newYaw >= 360.0F) {
                                newYaw = newYaw - 360.0F;
                            }
                        }
                        if (rotation == Rotation.ANTICLOCKWISE) {
                            newYaw = newYaw - 90;
                            if (newYaw < 0.0F) {
                                newYaw = newYaw + 360.0F;
                            }
                        }
                        newPLoc.setYaw(newYaw);

//						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
//							getCraft().setPilotLockedX(newPLoc.getX());
//							getCraft().setPilotLockedY(newPLoc.getY());
//							getCraft().setPilotLockedZ(newPLoc.getZ());
//							}
                        EntityUpdateCommand eUp = new EntityUpdateCommand(pTest.getLocation().clone(), newPLoc, pTest);
                        entityUpdateSet.add(eUp);
//						if(getCraft().getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(getCraft())) {
//							getCraft().setPilotLockedX(newPLoc.getX());
//							getCraft().setPilotLockedY(newPLoc.getY());
//							getCraft().setPilotLockedZ(newPLoc.getZ());
//						}
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
            List<MovecraftLocation> airLocation = ListUtils.subtract(Arrays.asList(originalBlockList), Arrays.asList(blockList));

            for (MovecraftLocation l1 : airLocation) {
                if (waterCraft) {
                    // if its below the waterline, fill in with water. Otherwise fill in with air.
                    if (l1.getY() <= waterLine) {
                        mapUpdates.add(new MapUpdateCommand(l1, 9, (byte) 0, parentCraft));
                    } else {
                        mapUpdates.add(new MapUpdateCommand(l1, 0, (byte) 0, parentCraft));
                    }
                } else {
                    mapUpdates.add(new MapUpdateCommand(l1, 0, (byte) 0, parentCraft));
                }
            }

            // rotate scheduled block changes
            HashMap<MapUpdateCommand, Long> newScheduledBlockChanges = new HashMap<>();
            HashMap<MapUpdateCommand, Long> oldScheduledBlockChanges = getCraft().getScheduledBlockChanges();
            for (MapUpdateCommand muc : oldScheduledBlockChanges.keySet()) {
                MovecraftLocation newLoc = muc.getNewBlockLocation();
                newLoc = newLoc.subtract(originPoint);
                newLoc = MathUtils.rotateVec(rotation, newLoc).add(originPoint);
                Long newTime = System.currentTimeMillis() + 5000;
                MapUpdateCommand newMuc = new MapUpdateCommand(newLoc, muc.getTypeID(), muc.getDataID(), parentCraft);
                newScheduledBlockChanges.put(newMuc, newTime);
            }
            this.scheduledBlockChanges = newScheduledBlockChanges;

            MapUpdateCommand[] updateArray = mapUpdates.toArray(new MapUpdateCommand[1]);
//            MapUpdateManager.getInstance().sortUpdates(updateArray);
            this.updates = updateArray;
            this.entityUpdates = entityUpdateSet.toArray(new EntityUpdateCommand[1]);

            maxX = null;
            maxZ = null;
            minX = null;
            minZ = null;


            for (MovecraftLocation l : blockList) {
                if (maxX == null || l.getX() > maxX) {
                    maxX = l.getX();
                }
                if (maxZ == null || l.getZ() > maxZ) {
                    maxZ = l.getZ();
                }
                if (minX == null || l.getX() < minX) {
                    minX = l.getX();
                }
                if (minZ == null || l.getZ() < minZ) {
                    minZ = l.getZ();
                }
            }

            // Rerun the polygonal bounding formula for the newly formed craft
            int sizeX, sizeZ;
            sizeX = (maxX - minX) + 1;
            sizeZ = (maxZ - minZ) + 1;


            int[][][] polygonalBox = new int[sizeX][][];


            for (MovecraftLocation l : blockList) {
                if (polygonalBox[l.getX() - minX] == null) {
                    polygonalBox[l.getX() - minX] = new int[sizeZ][];
                }


                if (polygonalBox[l.getX() - minX][l.getZ() - minZ] == null) {

                    polygonalBox[l.getX() - minX][l.getZ() - minZ] = new int[2];
                    polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
                    polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();

                } else {
                    minY = polygonalBox[l.getX() - minX][l.getZ() - minZ][0];
                    maxY = polygonalBox[l.getX() - minX][l.getZ() - minZ][1];

                    if (l.getY() < minY) {
                        polygonalBox[l.getX() - minX][l.getZ() - minZ][0] = l.getY();
                    }
                    if (l.getY() > maxY) {
                        polygonalBox[l.getX() - minX][l.getZ() - minZ][1] = l.getY();
                    }

                }


            }

            this.hitbox = polygonalBox;
            if (getCraft().getCruising()) {
                if (rotation == Rotation.ANTICLOCKWISE) {
                    // ship faces west
                    if (getCraft().getCruiseDirection() == 0x5) {
                        getCraft().setCruiseDirection((byte) 0x2);
                    } else
                        // ship faces east
                        if (getCraft().getCruiseDirection() == 0x4) {
                            getCraft().setCruiseDirection((byte) 0x3);
                        } else
                            // ship faces north
                            if (getCraft().getCruiseDirection() == 0x2) {
                                getCraft().setCruiseDirection((byte) 0x4);
                            } else
                                // ship faces south
                                if (getCraft().getCruiseDirection() == 0x3) {
                                    getCraft().setCruiseDirection((byte) 0x5);
                                }
                } else if (rotation == Rotation.CLOCKWISE) {
                    // ship faces west
                    if (getCraft().getCruiseDirection() == 0x5) {
                        getCraft().setCruiseDirection((byte) 0x3);
                    } else
                        // ship faces east
                        if (getCraft().getCruiseDirection() == 0x4) {
                            getCraft().setCruiseDirection((byte) 0x2);
                        } else
                            // ship faces north
                            if (getCraft().getCruiseDirection() == 0x2) {
                                getCraft().setCruiseDirection((byte) 0x5);
                            } else
                                // ship faces south
                                if (getCraft().getCruiseDirection() == 0x3) {
                                    getCraft().setCruiseDirection((byte) 0x4);
                                }
                }
            }

            // if you rotated a subcraft, update the parent with the new blocks
            if (this.isSubCraft) {
                // also find the furthest extent from center and notify the player of the new direction
                int farthestX = 0;
                int farthestZ = 0;
                for (MovecraftLocation loc : blockList) {
                    if (Math.abs(loc.getX() - originPoint.getX()) > Math.abs(farthestX))
                        farthestX = loc.getX() - originPoint.getX();
                    if (Math.abs(loc.getZ() - originPoint.getZ()) > Math.abs(farthestZ))
                        farthestZ = loc.getZ() - originPoint.getZ();
                }
                if (Math.abs(farthestX) > Math.abs(farthestZ)) {
                    if (farthestX > 0) {
                        if (getCraft().getNotificationPlayer() != null)
                            getCraft().getNotificationPlayer().sendMessage("The farthest extent now faces East");
                    } else {
                        if (getCraft().getNotificationPlayer() != null)
                            getCraft().getNotificationPlayer().sendMessage("The farthest extent now faces West");
                    }
                } else {
                    if (farthestZ > 0) {
                        if (getCraft().getNotificationPlayer() != null)
                            getCraft().getNotificationPlayer().sendMessage("The farthest extent now faces South");
                    } else {
                        if (getCraft().getNotificationPlayer() != null)
                            getCraft().getNotificationPlayer().sendMessage("The farthest extent now faces North");
                    }
                }

                craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
                for (Craft craft : craftsInWorld) {
                    if (BlockUtils.arrayContainsOverlap(craft.getBlockList(), originalBlockList) && craft != getCraft()) {
                        List<MovecraftLocation> parentBlockList = ListUtils.subtract(Arrays.asList(craft.getBlockList()), Arrays.asList(originalBlockList));
                        parentBlockList.addAll(Arrays.asList(blockList));
                        craft.setBlockList(parentBlockList.toArray(new MovecraftLocation[1]));

                        // Rerun the polygonal bounding formula for the parent craft
                        Integer parentMaxX = null;
                        Integer parentMaxZ = null;
                        Integer parentMinX = null;
                        Integer parentMinZ = null;
                        for (MovecraftLocation l : parentBlockList) {
                            if (parentMaxX == null || l.getX() > parentMaxX) {
                                parentMaxX = l.getX();
                            }
                            if (parentMaxZ == null || l.getZ() > parentMaxZ) {
                                parentMaxZ = l.getZ();
                            }
                            if (parentMinX == null || l.getX() < parentMinX) {
                                parentMinX = l.getX();
                            }
                            if (parentMinZ == null || l.getZ() < parentMinZ) {
                                parentMinZ = l.getZ();
                            }
                        }
                        int parentSizeX, parentSizeZ;
                        parentSizeX = (parentMaxX - parentMinX) + 1;
                        parentSizeZ = (parentMaxZ - parentMinZ) + 1;
                        int[][][] parentPolygonalBox = new int[parentSizeX][][];
                        for (MovecraftLocation l : parentBlockList) {
                            if (parentPolygonalBox[l.getX() - parentMinX] == null) {
                                parentPolygonalBox[l.getX() - parentMinX] = new int[parentSizeZ][];
                            }
                            if (parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ] == null) {
                                parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ] = new int[2];
                                parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0] = l.getY();
                                parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1] = l.getY();
                            } else {
                                int parentMinY = parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0];
                                int parentMaxY = parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1];

                                if (l.getY() < parentMinY) {
                                    parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][0] = l.getY();
                                }
                                if (l.getY() > parentMaxY) {
                                    parentPolygonalBox[l.getX() - parentMinX][l.getZ() - parentMinZ][1] = l.getY();
                                }
                            }
                        }
                        craft.setMinX(parentMinX);
                        craft.setMinZ(parentMinZ);
                        craft.setHitBox(parentPolygonalBox);
                    }
                }
            }


        } else { // this else is for "if(!failed)"
            if (this.isSubCraft) {
                if (parentCraft != getCraft()) {
                    parentCraft.setProcessing(false);
                }
            }
        }
    }

    public HashMap<MapUpdateCommand, Long> getScheduledBlockChanges() {
        return scheduledBlockChanges;
    }

    public void setScheduledBlockChanges(HashMap<MapUpdateCommand, Long> scheduledBlockChanges) {
        this.scheduledBlockChanges = scheduledBlockChanges;
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

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc, HashSet<MovecraftLocation> existingBlockSet) {
        Material testMaterial;
        MovecraftLocation aroundNewLoc;

        aroundNewLoc = newLoc.translate(1, 0, 0);
        testMaterial = getCraft().getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(-1, 0, 0);
        testMaterial = getCraft().getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, 1);
        testMaterial = getCraft().getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, -1);
        testMaterial = getCraft().getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }
        return true;
    }
}
