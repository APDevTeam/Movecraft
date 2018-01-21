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

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.CraftTranslateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ItemDropUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.TownyWorldHeightLimits;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TranslationTask extends AsyncTask{
    private int dx,dy,dz;
    private boolean failed = false;
    private String failMessage;
    private Collection<UpdateCommand> updates = new HashSet<>();
    private boolean collisionExplosion;
    private final HitBox newHitBox = new HitBox();
    private final HitBox oldHitBox;

    public TranslationTask(Craft c, int dx, int dy, int dz) {
        super(c);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        oldHitBox=c.getHitBox();
    }

    @Override
    public void excecute() {
        //MovecraftLocation[] blocksList = data.getBlockList();

        final int[] fallThroughBlocks = new int[]{0, 8, 9, 10, 11, 31, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 68, 69, 70, 72, 75, 76, 77, 78, 83, 85, 93, 94, 111, 141, 142, 143, 171};

        // blockedByWater=false means an ocean-going vessel
        boolean waterCraft = !craft.getType().blockedByWater();
        boolean hoverCraft = craft.getType().getCanHover();

        boolean airCraft = craft.getType().blockedByWater();

        int hoverLimit = craft.getType().getHoverLimit();

        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(craft);



        // start by finding the crafts borders
        int minY = oldHitBox.getMinY();
        int maxY = oldHitBox.getMaxY();
        int maxX = oldHitBox.getMaxX();
        int maxZ = oldHitBox.getMaxZ();  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
        int minX = oldHitBox.getMinX();
        int minZ = oldHitBox.getMinZ();

        // treat sinking crafts specially
        if (craft.getSinking()) {
            waterCraft = true;
            hoverCraft = false;
        }

        if (craft.getDisabled() && (!craft.getSinking())) {
            fail(I18nSupport.getInternationalisedString("Craft is disabled!"));
        }

        // check the maxheightaboveground limitation, move 1 down if that limit is exceeded
        if (craft.getType().getMaxHeightAboveGround() > 0 && data.getDy() >= 0) {
            int x = oldHitBox.getMaxX() + oldHitBox.getMinX();
            x = x >> 1;
            int y = oldHitBox.getMaxY();
            int z = oldHitBox.getMaxZ() + oldHitBox.getMinZ();
            z = z >> 1;
            int cy = oldHitBox.getMinY();
            boolean done = false;
            while (!done) {
                cy = cy - 1;
                if (craft.getW().getBlockTypeIdAt(x, cy, z) != 0)
                    done = true;
                if (cy <= 1)
                    done = true;
            }
            if (y - cy > craft.getType().getMaxHeightAboveGround()) {
                dy=-1;
            }
        }

        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        if (waterCraft) {
            if (craft.getType().getStaticWaterLevel() != 0) {
                if (waterLine <= maxY + 1) {
                    waterLine = craft.getType().getStaticWaterLevel();
                }
            } else {
                // figure out the water level by examining blocks next to the outer boundaries of the craft
                for (int posY = maxY + 1; (posY >= minY - 1) && (waterLine == 0); posY--) {
                    int numWater = 0;
                    int numAir = 0;
                    int posX;
                    int posZ;
                    posZ = minZ - 1;
                    for (posX = minX - 1; (posX <= maxX + 1) && (waterLine == 0); posX++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posZ = maxZ + 1;
                    for (posX = minX - 1; (posX <= maxX + 1) && (waterLine == 0); posX++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posX = minX - 1;
                    for (posZ = minZ; (posZ <= maxZ) && (waterLine == 0); posZ++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posX = maxX + 1;
                    for (posZ = minZ; (posZ <= maxZ) && (waterLine == 0); posZ++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    if (numWater > numAir) {
                        waterLine = posY;
                    }
                }
            }

            // now add all the air blocks found within the craft's hitbox immediately above the waterline and below to the craft blocks so they will be translated
            HashSet<MovecraftLocation> newHSBlockList = new HashSet<>(Arrays.asList(blocksList));
            int posY = waterLine + 1;
            for (int posX = minX; posX < maxX; posX++) {
                for (int posZ = minZ; posZ < maxZ; posZ++) {
                    int localMaxY=oldHitBox.getLocalMaxY(posX,posZ);
                    if(localMaxY != -1){
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 0 && posY < localMaxY && posY > oldHitBox.getLocalMinY(posX,posZ)) {
                            MovecraftLocation l = new MovecraftLocation(posX, posY, posZ);
                            newHSBlockList.add(l);
                        }
                    }
                }
            }
            // dont check the hitbox for the underwater portion. Otherwise open-hulled ships would flood.
            for (posY = waterLine; posY >= minY; posY--) {
                for (int posX = minX; posX < maxX; posX++) {
                    for (int posZ = minZ; posZ < maxZ; posZ++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 0) {
                            MovecraftLocation l = new MovecraftLocation(posX, posY, posZ);
                            newHSBlockList.add(l);
                        }
                    }
                }
            }

            blocksList = newHSBlockList.toArray(new MovecraftLocation[newHSBlockList.size()]);
        }

        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = craft.getType().getFuelBurnRate();
        // going down doesn't require fuel
        if (dy == -1 && dx == 0 && dz == 0)
            fuelBurnRate = 0.0;

        if (fuelBurnRate != 0.0 && !craft.getSinking()) {
            if (craft.getBurningFuel() < fuelBurnRate) {
                Block fuelHolder = null;
                for (MovecraftLocation bTest : blocksList) {
                    Block b = craft.getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
                    if (b.getTypeId() == 61) {
                        InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                        if (inventoryHolder.getInventory().contains(263) || inventoryHolder.getInventory().contains(173)) {
                            fuelHolder = b;
                        }
                    }
                }
                if (fuelHolder == null) {
                    fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
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
                        craft.setBurningFuel(craft.getBurningFuel() + 7.0);
                    } else {
                        ItemStack iStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(173));
                        int amount = iStack.getAmount();
                        if (amount == 1) {
                            inventoryHolder.getInventory().remove(iStack);
                        } else {
                            iStack.setAmount(amount - 1);
                        }
                        craft.setBurningFuel(craft.getBurningFuel() + 79.0);

                    }
                }
            } else {
                craft.setBurningFuel(craft.getBurningFuel() - fuelBurnRate);
            }
        }

        List<MovecraftLocation> tempBlockList = new ArrayList<>();
        HashSet<MovecraftLocation> existingBlockSet = new HashSet<>(Arrays.asList(blocksList));
        //Set<UpdateCommand> updateSet = new HashSet<>();

        collisionExplosion=false;

        List<Material> harvestBlocks = craft.getType().getHarvestBlocks();
        List<MovecraftLocation> harvestedBlocks = new ArrayList<>();
        List<MovecraftLocation> destroyedBlocks = new ArrayList<>();
        List<Material> harvesterBladeBlocks = craft.getType().getHarvesterBladeBlocks();

        int hoverOver = dy;
        int craftMinY = 0;
        int craftMaxY = 0;
        boolean clearNewData = false;
        boolean hoverUseGravity = craft.getType().getUseGravity();
        boolean checkHover = (dx != 0 || dz != 0);// we want to check only horizontal moves
        boolean canHoverOverWater = craft.getType().getCanHoverOverWater();
        boolean townyEnabled = Movecraft.getInstance().getTownyPlugin() != null;
        boolean explosionBlockedByTowny = false;
        boolean moveBlockedByTowny = false;
        boolean validateTownyExplosion = false;
        String townName = "";

        Set<TownBlock> townBlockSet = new HashSet<>();
        TownyWorld townyWorld = null;
        TownyWorldHeightLimits townyWorldHeightLimits = null;

        if (townyEnabled && Settings.TownyBlockMoveOnSwitchPerm) {
            townyWorld = TownyUtils.getTownyWorld(craft.getW());
            if (townyWorld != null) {
                townyEnabled = townyWorld.isUsingTowny();
                if (townyEnabled) {
                    townyWorldHeightLimits = TownyUtils.getWorldLimits(craft.getW());
                    if (craft.getType().getCollisionExplosion() != 0.0F) {
                        validateTownyExplosion = true;
                    }
                }
            }
        } else {
            townyEnabled = false;
        }


        for (int i = 0; i < blocksList.length; i++) {
            MovecraftLocation oldLoc = blocksList[i];
            MovecraftLocation newLoc = oldLoc.translate(dx, dy, dz);

            if (newLoc.getY() > oldHitBox.getMaxY() && newLoc.getY() > oldLoc.getY()) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
                break;
            } else if (newLoc.getY() < oldHitBox.getMinY() && newLoc.getY() < oldLoc.getY() && !craft.getSinking()) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
                break;
            }

            boolean blockObstructed;
            boolean bladeOK = true;
            Material testMaterial;


            Location plugLoc = new Location(craft.getW(), newLoc.getX(), newLoc.getY(), newLoc.getZ());
            if (craftPilot != null) {
                // See if they are permitted to build in the area, if WorldGuard integration is turned on
                if (Movecraft.getInstance().getWorldGuardPlugin() != null && Settings.WorldGuardBlockMoveOnBuildPerm) {
                    if (!Movecraft.getInstance().getWorldGuardPlugin().canBuild(craftPilot, plugLoc)) {
                        fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Player is not permitted to build in this WorldGuard region") + " @ %d,%d,%d", oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()));
                        break;
                    }
                }
            }
            Player p;
            if (craftPilot == null) {
                p = craft.getNotificationPlayer();
            } else {
                p = craftPilot;
            }
            if (p != null) {
                if (Movecraft.getInstance().getWorldGuardPlugin() != null && Movecraft.getInstance().getWGCustomFlagsPlugin() != null && Settings.WGCustomFlagsUsePilotFlag) {
                    LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(p);
                    WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
                    if (!WGCFU.validateFlag(plugLoc, Movecraft.FLAG_MOVE, lp)) {
                        fail(String.format(I18nSupport.getInternationalisedString("WGCustomFlags - Translation Failed") + " @ %d,%d,%d", oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()));
                        break;
                    }
                }
                if (townyEnabled) {
                    TownBlock townBlock = TownyUtils.getTownBlock(plugLoc);
                    if (townBlock != null && !townBlockSet.contains(townBlock)) {
                        if (validateTownyExplosion) {
                            if (!explosionBlockedByTowny) {
                                if (!TownyUtils.validateExplosion(townBlock)) {
                                    explosionBlockedByTowny = true;
                                }
                            }
                        }
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
                                boolean failed = false;
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
                                                if (WGCFU.validateFlag(plugLoc, Movecraft.FLAG_MOVE, lp)) {
                                                    failed = false;
                                                }
                                            }
                                        }
                                    }
                                    if (failed) {
                                        townName = town.getName();
                                        moveBlockedByTowny = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //check for chests around
            testMaterial = craft.getW().getBlockAt(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()).getType();
            if (testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) {
                if (!checkChests(testMaterial, newLoc, existingBlockSet)) {
                    //prevent chests collision
                    fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLoc.getX(), newLoc.getY(), newLoc.getZ(), craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType().toString()));
                    break;
                }
            }

            if (craft.getSinking()) {
                int testID = craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getTypeId();
                blockObstructed = !(Arrays.binarySearch(fallThroughBlocks, testID) >= 0) && !existingBlockSet.contains(newLoc);
            } else if (!waterCraft) {
                // New block is not air or a piston head and is not part of the existing ship
                testMaterial = craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType();
                blockObstructed = (!testMaterial.equals(Material.AIR)) && !existingBlockSet.contains(newLoc);
            } else {
                // New block is not air or water or a piston head and is not part of the existing ship
                testMaterial = craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType();
                blockObstructed = (!testMaterial.equals(Material.AIR) && !testMaterial.equals(Material.STATIONARY_WATER)
                        && !testMaterial.equals(Material.WATER)) && !existingBlockSet.contains(newLoc);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (craft.getW().getBlockAt(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()).getType().equals(Material.AIR) && blockObstructed) {
                ignoreBlock = true;
//	            	blockObstructed=false;
            }

            testMaterial = craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType();
            if (blockObstructed) {
                if (hoverCraft || harvestBlocks.size() > 0) {
                    // New block is not harvested block
                    if (harvestBlocks.contains(testMaterial) && !existingBlockSet.contains(newLoc)) {
                        Material tmpType = craft.getW().getBlockAt(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()).getType();
                        if (harvesterBladeBlocks.size() > 0) {
                            if (!harvesterBladeBlocks.contains(tmpType)) {
                                bladeOK = false;
                            }
                        }
                        if (bladeOK) {
                            blockObstructed = false;
                            tryPutToDestroyBox(testMaterial, newLoc, harvestedBlocks, destroyedBlocks);
                            harvestedBlocks.add(newLoc);
                        }
                    }
                }
            }

            if (blockObstructed || moveBlockedByTowny) {
                if (hoverCraft && checkHover) {
                    //we check one up ever, if it is hovercraft and one down if it's using gravity
                    if (hoverOver == 0 && newLoc.getY() + 1 <= oldHitBox.getMaxY()) {
                        //first was checked actual level, now check if we can go up
                        hoverOver = 1;
                        dy=1;
                        clearNewData = true;
                    } else if (hoverOver >= 1) {
                        //check other options to go up
                        if (hoverOver < hoverLimit + 1 && newLoc.getY() + 1 <= oldHitBox.getMaxY()) {
                            dy= hoverOver + 1;
                            hoverOver += 1;
                            clearNewData = true;
                        } else {
                            if (hoverUseGravity && newLoc.getY() - hoverOver - 1 >= oldHitBox.getMinY()) {
                                //we are on the maximum of top
                                //if we can't go up so we test bottom side
                                dy=-1;
                                hoverOver = -1;
                            } else {
                                // no way - back to original dY, turn off hovercraft for this move
                                // and get original data again for all explosions
                                dy=0;
                                hoverOver = 0;
                                hoverCraft = false;
                                hoverUseGravity = false;
                            }
                            clearNewData = true;
                        }
                    } else if (hoverOver <= -1) {
                        //we cant go down for 1 block, check more to hoverLimit
                        if (hoverOver > -hoverLimit - 1 && newLoc.getY() - 1 >= oldHitBox.getMinY()) {
                            dy = hoverOver - 1;
                            hoverOver -= 1;
                            clearNewData = true;
                        } else {
                            // no way - back to original dY, turn off hovercraft for this move
                            // and get original data again for all explosions
                            dy = 0;
                            hoverOver = 0;
                            hoverUseGravity = false;
                            clearNewData = true;
                            hoverCraft = false;
                        }
                    } else {
                        // no way - reached MaxHeight during looking new way upstairss
                        if (hoverUseGravity && newLoc.getY() - 1 >= oldHitBox.getMinY()) {
                            //we are on the maximum of top
                            //if we can't go up so we test bottom side
                            dy = -1;
                            hoverOver = -1;
                        } else {
                            // - back to original dY, turn off hovercraft for this move
                            // and get original data again for all explosions
                            dy = 0;
                            hoverOver = 0;
                            hoverUseGravity = false;
                            hoverCraft = false;
                        }
                        clearNewData = true;
                    }
                    // End hovercraft stuff
                } else {
                    // handle sinking ship collisions
                    if (craft.getSinking()) {
                        if (craft.getType().getExplodeOnCrash() != 0.0F && !explosionBlockedByTowny) {
                            float explosionKey = craft.getType().getExplodeOnCrash();
                            if (System.currentTimeMillis() - craft.getOrigPilotTime() > 1000){
                                Location loc = oldLoc.toBukkit(craft.getW());
                                if (!loc.getBlock().getType().equals(Material.AIR)) {
                                    updates.add(new ExplosionUpdateCommand( loc, explosionKey));
                                    collisionExplosion=true;
                                }
                            }
                        } else {
                            // use the explosion code to clean up the craft, but not with enough force to do anything
                            int explosionKey = 1;
                            Location loc = oldLoc.toBukkit(craft.getW());
                            if (!loc.getBlock().getType().equals(Material.AIR)) {
                                updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                                collisionExplosion = true;
                            }
                        }
                    } else {
                        // Explode if the craft is set to have a CollisionExplosion. Also keep moving for spectacular ramming collisions
                        if (craft.getType().getCollisionExplosion() == 0.0F) {
                            if (moveBlockedByTowny) {
                                fail(String.format(I18nSupport.getInternationalisedString("Towny - Translation Failed") + " %s @ %d,%d,%d", townName, oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()));
                            } else {
                                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", oldLoc.getX(), oldLoc.getY(), oldLoc.getZ(), craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType().toString()));
                                if (craft.getNotificationPlayer() != null) {
                                    Location location = craft.getNotificationPlayer().getLocation();
                                }
                            }
                            break;
                        } else if (explosionBlockedByTowny) {
                            int explosionKey = 1;
                            Location loc = oldLoc.toBukkit(craft.getW());
                            if (!loc.getBlock().getType().equals(Material.AIR)) {
                                updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                                collisionExplosion=true;
                            }
                        } else if (System.currentTimeMillis() - craft.getOrigPilotTime() > 1000) {
                            float explosionKey;
                            float explosionForce = craft.getType().getCollisionExplosion();
                            if (craft.getType().getFocusedExplosion()) {
                                explosionForce = explosionForce * oldHitBox.size();
                            }
                            if (oldLoc.getY() < waterLine) { // underwater explosions require more force to do anything
                                explosionForce += 25;//TODO: find the correct amount
                            }
                            explosionKey = explosionForce;
                            Location loc = oldLoc.toBukkit(craft.getW());
                            if (!loc.getBlock().getType().equals(Material.AIR)) {
                                updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                                collisionExplosion=true;
                            }
                            if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                                break;
                            }
                        }
                    }
                }
            } else {
                //block not obstructed
                /*Material oldType = craft.getW().getBlockAt(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()).getType();
                byte oldData = craft.getW().getBlockAt(oldLoc.getX(), oldLoc.getY(), oldLoc.getZ()).getData();
                // remove water from sinking crafts
                if (craft.getSinking()) {
                    if ((oldType == Material.WATER || oldType == Material.STATIONARY_WATER) && oldLoc.getY() > waterLine)
                        oldType = Material.AIR;
                }*/

                if (!ignoreBlock) {
                    //updateSet.add(new BlockTranslateCommand(oldLoc, newLoc, oldType, oldData, craft));
                    tempBlockList.add(newLoc);
                }

                if (i == blocksList.length - 1) {
                    if ((hoverCraft && hoverUseGravity) || (hoverUseGravity && newLoc.getY() > oldHitBox.getMaxY() && hoverOver == 0)) {
                        //hovecraft using gravity or something else using gravity and flying over its limit
                        int iFreeSpace = 0;
                        //canHoverOverWater adds 1 to dY for better check water under craft
                        // best way should be expand selected region to each first blocks under craft
                        if (hoverOver == 0) {
                            //we go directly forward so we check if we can go down
                            for (int ii = -1; ii > -hoverLimit - 2 - (canHoverOverWater ? 0 : 1); ii--) {
                                if (!isFreeSpace(dx, hoverOver + ii, dz, blocksList, existingBlockSet, waterCraft, hoverCraft, harvestBlocks, canHoverOverWater, checkHover)) {
                                    break;
                                }
                                iFreeSpace++;
                            }
                            if (failed) {
                                break;
                            }
                            if (iFreeSpace > hoverLimit - (canHoverOverWater ? 0 : 1)) {
                                dy = -1;
                                hoverOver = -1;
                                clearNewData = true;
                            }
                        } else if (hoverOver == 1 && !airCraft) {
                            //prevent fly heigher than hoverLimit
                            for (int ii = -1; ii > -hoverLimit - 2; ii--) {
                                if (!isFreeSpace(dx, hoverOver + ii, dz, blocksList, existingBlockSet, waterCraft, hoverCraft, harvestBlocks, canHoverOverWater, checkHover)) {
                                    break;
                                }
                                iFreeSpace++;
                            }
                            if (failed) {
                                break;
                            }
                            if (iFreeSpace > hoverLimit) {
                                if (bladeOK) {
                                    fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
                                } else {
                                    fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", oldLoc.getX(), oldLoc.getY(), oldLoc.getZ(), craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType().toString()));

                                }
                                break;
                            }
                        } else if (hoverOver > 1) {
                            //prevent jump thru block
                            for (int ii = 1; ii < hoverOver - 1; ii++) {
                                if (!isFreeSpace(0, ii, 0, blocksList, existingBlockSet, waterCraft, hoverCraft, harvestBlocks, canHoverOverWater, checkHover)) {
                                    break;
                                }
                                iFreeSpace++;
                            }
                            if (failed) {
                                break;
                            }
                            if (iFreeSpace + 2 < hoverOver) {
                                dy=-1;
                                hoverOver = -1;
                                clearNewData = true;
                            }
                        } else if (hoverOver < -1) {
                            //prevent jump thru block
                            for (int ii = -1; ii > hoverOver + 1; ii--) {
                                if (!isFreeSpace(0, ii, 0, blocksList, existingBlockSet, waterCraft, hoverCraft, harvestBlocks, canHoverOverWater, checkHover)) {
                                    break;
                                }
                                iFreeSpace++;
                            }
                            if (failed) {
                                break;
                            }
                            if (iFreeSpace + 2 < -hoverOver) {
                                dy=0;
                                hoverOver = 0;
                                hoverCraft = false;
                                clearNewData = true;
                            }
                        }
                        if (!canHoverOverWater) {
                            if (hoverOver >= 1) {
                                //others hoverOver values we have checked jet
                                for (int ii = hoverOver - 1; ii > hoverOver - hoverLimit - 2; ii--) {
                                    if (!isFreeSpace(0, ii, 0, blocksList, existingBlockSet, waterCraft, hoverCraft, harvestBlocks, canHoverOverWater, checkHover)) {
                                        break;
                                    }
                                    iFreeSpace++;
                                }
                                if (failed) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } //END OF: if (blockObstructed) 

            if (clearNewData) {
                i = -1;
                tempBlockList.clear();
                updates.clear();
                harvestedBlocks.clear();
                collisionExplosion=false;
                clearNewData = false;
                townBlockSet.clear();
                craftMinY = 0;
                craftMaxY = 0;
            }

        } //END OF: for ( int i = 0; i < blocksList.length; i++ ) {

        if (collisionExplosion) {
            // mark the craft to check for sinking, remove the exploding blocks from the blocklist, and submit the explosions for map update
            for (UpdateCommand m : updates) {
                if(!(m instanceof ExplosionUpdateCommand))
                    continue;
                ExplosionUpdateCommand explosionCommand = (ExplosionUpdateCommand)m;
                MovecraftLocation explosionLocation = MathUtils.bukkit2MovecraftLoc(explosionCommand.getLocation());
                if (existingBlockSet.contains(explosionLocation)) {
                    existingBlockSet.remove(explosionLocation);
                    if (Settings.FadeWrecksAfter > 0) {
                        int typeID = explosionCommand.getLocation().getBlock().getTypeId();
                        if (typeID != 0 && typeID != 9) {
                            Movecraft.getInstance().blockFadeTimeMap.put(explosionLocation, System.currentTimeMillis());
                            Movecraft.getInstance().blockFadeTypeMap.put(explosionLocation, typeID);
                            if (explosionCommand.getLocation().getY() <= waterLine) {
                                Movecraft.getInstance().blockFadeWaterMap.put(explosionLocation, true);
                            } else {
                                Movecraft.getInstance().blockFadeWaterMap.put(explosionLocation, false);
                            }
                            Movecraft.getInstance().blockFadeWorldMap.put(explosionLocation, craft.getW());
                        }
                    }
                }

                // if the craft is sinking, remove all solid blocks above the one that hit the ground from the craft for smoothing sinking
                if (craft.getSinking() && (craft.getType().getExplodeOnCrash() == 0.0 || explosionBlockedByTowny)) {
                    MovecraftLocation location = MathUtils.bukkit2MovecraftLoc(explosionCommand.getLocation());
                    int posy = (int) explosionCommand.getLocation().getY() + 1;
                    int testID = craft.getW().getBlockAt(location.getX(), posy, location.getZ()).getTypeId();
                    while (posy <= maxY && !(Arrays.binarySearch(fallThroughBlocks, testID) >= 0)) {
                        MovecraftLocation testLoc = new MovecraftLocation(location.getX(), posy, location.getZ());
                        if (existingBlockSet.contains(testLoc)) {
                            existingBlockSet.remove(testLoc);
                            if (Settings.FadeWrecksAfter > 0) {
                                int typeID = craft.getW().getBlockAt(testLoc.getX(), testLoc.getY(), testLoc.getZ()).getTypeId();
                                if (typeID != 0 && typeID != 9) {
                                    Movecraft.getInstance().blockFadeTimeMap.put(testLoc, System.currentTimeMillis());
                                    Movecraft.getInstance().blockFadeTypeMap.put(testLoc, typeID);
                                    if (testLoc.getY() <= waterLine) {
                                        Movecraft.getInstance().blockFadeWaterMap.put(testLoc, true);
                                    } else {
                                        Movecraft.getInstance().blockFadeWaterMap.put(testLoc, false);
                                    }
                                    Movecraft.getInstance().blockFadeWorldMap.put(testLoc, craft.getW());
                                }
                            }
                        }
                        posy++;
                        testID = craft.getW().getBlockAt(location.getX(), posy, location.getZ()).getTypeId();
                    }
                }
            }


            MovecraftLocation[] newBlockList = existingBlockSet.toArray(new MovecraftLocation[0]);
            data.setBlockList(newBlockList);
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed"));

            if (!craft.getSinking()) {   // FROG changed from ==true, think that was a typo
                if (craft.getType().getSinkPercent() != 0.0) {
                    craft.setLastBlockCheck(0);
                }
                craft.setLastCruisUpdate(System.currentTimeMillis() - 30000);
            }
        }

        if (!failed) {
            updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(data.getDx(), data.getDy(), data.getDz())));
            MovecraftLocation[] newBlockList = tempBlockList.toArray(new MovecraftLocation[0]);
            data.setBlockList(newBlockList);

            //prevents torpedo and rocket pilots :)
            if (craft.getType().getMoveEntities() && !craft.getSinking()) {
                // Move entities within the craft
                List<Entity> eList = null;
                int numTries = 0;
                while ((eList == null) && (numTries < 100)) {
                    try {
                        eList = craft.getW().getEntities();
                    } catch (java.util.ConcurrentModificationException e) {
                        numTries++;
                    }
                }

                for (Entity pTest : eList) {
                    //                                if ( MathUtils.playerIsWithinBoundingPolygon( oldHitBox, craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {
                    if (MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(pTest.getLocation()))) {
                        if (pTest.getType() == EntityType.PLAYER) {
                            Player player = (Player) pTest;
                            craft.getMovedPlayers().put(player, System.currentTimeMillis());
                            Location tempLoc = pTest.getLocation();

//                                        Direct control no longer locks the player in place
//                                       if(craft.getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(craft)) {
//                                            tempLoc.setX(craft.getPilotLockedX());
//                                            tempLoc.setY(craft.getPilotLockedY());
//                                            tempLoc.setZ(craft.getPilotLockedZ());
//                                        } 
                            tempLoc = tempLoc.add(dx, dy, dz);
                            Location newPLoc = new Location(craft.getW(), tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
                            newPLoc.setPitch(pTest.getLocation().getPitch());
                            newPLoc.setYaw(pTest.getLocation().getYaw());

                            EntityUpdateCommand eUp = new EntityUpdateCommand( newPLoc, pTest);
                            updates.add(eUp);
//                                        if(craft.getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(craft)) {
//                                            craft.setPilotLockedX(tempLoc.getX());
//                                            craft.setPilotLockedY(tempLoc.getY());
//                                            craft.setPilotLockedZ(tempLoc.getZ());
//                                        }
                        }
                        if (pTest.getType() == EntityType.PRIMED_TNT) {
                            Location tempLoc = pTest.getLocation();
                            tempLoc = tempLoc.add(dx,dy,dz);
                            EntityUpdateCommand eUp = new EntityUpdateCommand(tempLoc, pTest);
                            updates.add(eUp);
                        }

                    }
                }
            } else {
                //add releaseTask without playermove to manager
                if (!craft.getType().getCruiseOnPilot() && !craft.getSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                    CraftManager.getInstance().addReleaseTask(craft);
            }

            // remove water near sinking crafts
            if (craft.getSinking()) {
                int posX;
                int posY = maxY;
                int posZ;
                if (posY > waterLine) {
                    for (posX = minX - 1; posX <= maxX + 1; posX++) {
                        for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                            if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                                MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                                updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                            }
                        }
                    }
                }
                for (posY = maxY + 1; (posY >= minY - 1) && (posY > waterLine); posY--) {
                    posZ = minZ - 1;
                    for (posX = minX - 1; posX <= maxX + 1; posX++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                            MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                            updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                        }
                    }
                    posZ = maxZ + 1;
                    for (posX = minX - 1; posX <= maxX + 1; posX++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                            MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);

                            updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                        }
                    }
                    posX = minX - 1;
                    for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                            MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                            updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                        }
                    }
                    posX = maxX + 1;
                    for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                            MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                            updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                        }
                    }
                }
            }

            //Set blocks that are no longer craft to air

//                        /**********************************************************************************************************
//                        *   I had problems with ListUtils (I tryied commons-collections 3.2.1. and 4.0 without success) 
//                        *   so I replaced Lists with Sets
//                        * 
//                        *   Caused by: java.lang.NoClassDefFoundError: org/apache/commons/collections/ListUtils
//                        *   at net.countercraft.movecraft.async.translation.TranslationTask.excecute(TranslationTask.java:716)
//                        *                                                                                       mwkaicz 24-02-2015
//                        ***********************************************************************************************************/
//                        Set<MovecraftLocation> setA = new HashSet(Arrays.asList(blocksList));
//                        Set<MovecraftLocation> setB = new HashSet(Arrays.asList(newBlockList));
//                        setA.removeAll(setB);
//                        MovecraftLocation[] arrA = new MovecraftLocation[0];
//                        arrA = setA.toArray(arrA);
//                        List<MovecraftLocation> airLocation = Arrays.asList(arrA);
            //Removed because high runtime
            /*List<MovecraftLocation> airLocation = ArrayUtils.subtractAsList(blocksList, newBlockList);

            for (MovecraftLocation l1 : airLocation) {
                // for watercraft, fill blocks below the waterline with water
                if (!waterCraft) {
                    //updateSet.add(new BlockCreateCommand(l1, Material.AIR, (byte) 0, craft));
                    if (craft.getSinking()) {
                        updateSet.add(new ParticleUpdateCommand(l1.toBukkit(craft.getW()), craft.getType().getSmokeOnSink()));
                    }
                }
            }*/

            //add destroyed parts of growed
            /*for (MovecraftLocation destroyedLocation : destroyedBlocks) {
                updateSet.add(new BlockCreateCommand(destroyedLocation, Material.AIR, (byte) 0, craft));
            }*/
//            MapUpdateManager.getInstance().sortUpdates(updateArray);
            //data.setUpdates(updateSet);

            /*if (data.getDy() != 0) {
                data.setHitbox(BoundingBoxUtils.translateBoundingBoxVertically(data.getHitbox(), data.getDy()));
            }*/

            //data.setMinX(data.getMinX() + data.getDx());
            //data.setMinZ(data.getMinZ() + data.getDz());
        }

        captureYield(blocksList, harvestedBlocks);
    }

    private void fail(String message) {
        failed=true;
        failMessage=message;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
        if (craftPilot != null) {
            Location location = craftPilot.getLocation();
            if (!craft.getDisabled()) {
                craft.getW().playSound(location, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.25f);
                //craft.setCurTickCooldown(craft.getType().getCruiseTickCooldown());
            } else {
                craft.getW().playSound(location, Sound.ENTITY_IRONGOLEM_DEATH, 5.0f, 5.0f);
                //craft.setCurTickCooldown(craft.getType().getCruiseTickCooldown());
            }
        }

    }

    private boolean isFreeSpace(int x, int y, int z, MovecraftLocation[] blocksList, HashSet<MovecraftLocation> existingBlockSet, boolean waterCraft, boolean hoverCraft, List<Material> harvestBlocks, boolean canHoverOverWater, boolean checkHover) {
        boolean isFree = true;
        // this checking for hovercrafts should be faster with separating horizontal layers and checking only realy necesseries,
        // or more better: remember what checked in each translation, but it's beyond my current abilities, I will try to solve it in future
        for (MovecraftLocation oldLoc : blocksList) {
            MovecraftLocation newLoc = oldLoc.translate(x, y, z);

            Material testMaterial = craft.getW().getBlockAt(newLoc.getX(), newLoc.getY(), newLoc.getZ()).getType();
            if (!canHoverOverWater) {
                if (testMaterial.equals(Material.STATIONARY_WATER) || testMaterial.equals(Material.WATER)) {
                    fail(I18nSupport.getInternationalisedString("Translation - Failed Craft over water"));
                }
            }

            if (newLoc.getY() >= oldHitBox.getMaxY() && newLoc.getY() > oldLoc.getY() && !checkHover) {
                //if ( newLoc.getY() >= data.getMaxHeight() && newLoc.getY() > oldLoc.getY()) {
                isFree = false;
                break;
            } else if (newLoc.getY() <= oldHitBox.getMinY() && newLoc.getY() < oldLoc.getY()) {
                isFree = false;
                break;
            }

            boolean blockObstructed;
            if (!waterCraft) {
                // New block is not air or a piston head and is not part of the existing ship
                blockObstructed = !testMaterial.equals(Material.AIR) && !existingBlockSet.contains(newLoc);
            } else {
                // New block is not air or water or a piston head and is not part of the existing ship
                blockObstructed = !testMaterial.equals(Material.AIR) && !testMaterial.equals(Material.STATIONARY_WATER)
                        && !testMaterial.equals(Material.WATER) && !existingBlockSet.contains(newLoc);
            }
            if (blockObstructed && hoverCraft) {
                // New block is not harvested block and is not part of the existing craft
                blockObstructed = !(harvestBlocks.contains(testMaterial) && !existingBlockSet.contains(newLoc));
            }

            if (blockObstructed) {
                isFree = false;
                break;
            }
        }
        return isFree;
    }

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc, HashSet<MovecraftLocation> existingBlockSet) {
        Material testMaterial;
        MovecraftLocation aroundNewLoc;

        aroundNewLoc = newLoc.translate(1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(-1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, 1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!existingBlockSet.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, -1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        return !testMaterial.equals(mBlock) || existingBlockSet.contains(aroundNewLoc);
    }

    private void captureYield(MovecraftLocation[] blocksList, List<MovecraftLocation> harvestedBlocks) {
        if (harvestedBlocks.isEmpty()) {
            return;
        }
        ArrayList<Inventory> chests = new ArrayList<>();
        HashSet<ItemDropUpdateCommand> itemDropUpdateSet = new HashSet<>();
        //find chests
        for (MovecraftLocation loc : oldHitBox) {
            Block block = craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                chests.add(((InventoryHolder) (block.getState())).getInventory());
        }

        for (MovecraftLocation harvestedBlock : harvestedBlocks) {
            Block block = craft.getW().getBlockAt(harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ());
            ItemStack[] drops = block.getDrops().toArray(new ItemStack[block.getDrops().size()]);
            //generate seed drops
            if (block.getType() == Material.CROPS) {
                Random rand = new Random();
                int amount = rand.nextInt(4);
                if (amount > 0) {
                    ItemStack seeds = new ItemStack(Material.SEEDS, amount);
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList(drops));
                    d.add(seeds);
                    drops = d.toArray(new ItemStack[d.size()]);
                }
            }
            //get contents of inventories before deposting
            if (block.getState() instanceof InventoryHolder) {
                if (block.getState() instanceof Chest) {
                    //Inventory inv = ((DoubleChest) block.getState()).getRightSide().getInventory().getLocation().equals(block.getLocation()) ?((DoubleChest) block.getState()).getRightSide().getInventory(): ((DoubleChest) block.getState()).getLeftSide().getInventory();
                    //HashSet<ItemStack> d = new HashSet<ItemStack>(Arrays.asList(inv.getContents()));
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList(((Chest) block.getState()).getBlockInventory().getContents()));
                    d.addAll(block.getDrops());
                    drops = d.toArray(new ItemStack[d.size()]);
                } else {
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList((((InventoryHolder) block.getState()).getInventory().getContents())));
                    d.addAll(block.getDrops());
                    drops = d.toArray(new ItemStack[d.size()]);
                }
            }
            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    itemDropUpdateSet.add(new ItemDropUpdateCommand(new Location(craft.getW(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
            }
        }
    }

    private ItemStack putInToChests(ItemStack stack, ArrayList<Inventory> inventories) {
        if (stack == null)
            return null;
        if (inventories == null || inventories.isEmpty())
            return stack;
        for (Inventory inv : inventories) {
            int capacity = 0;
            for (ItemStack itemStack : inv) {
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    capacity += stack.getMaxStackSize();
                } else if (itemStack.isSimilar(stack)) {
                    capacity += stack.getMaxStackSize() - itemStack.getAmount();
                }
            }
            if (stack.getAmount() > capacity) {
                ItemStack tempItem = stack.clone();
                tempItem.setAmount(capacity);
                stack.setAmount(stack.getAmount() - capacity);
                inv.addItem(tempItem);
            } else {
                inv.addItem(stack);
                return null;
            }

        }
        return stack;
    }

    private void tryPutToDestroyBox(Material mat, MovecraftLocation loc, List<MovecraftLocation> harvestedBlocks, List<MovecraftLocation> destroyedBlocks) {
        if (mat.equals(Material.DOUBLE_PLANT) || mat.equals(Material.WOODEN_DOOR) || mat.equals(Material.IRON_DOOR_BLOCK)) {
            if (craft.getW().getBlockAt(loc.getX(), loc.getY() + 1, loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 1, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX(), loc.getY() - 1, loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, -1, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
        } else if (mat.equals(Material.CACTUS) || mat.equals(Material.SUGAR_CANE_BLOCK)) {
            MovecraftLocation tmpLoc = loc.translate(0, 1, 0);
            Material tmpType = craft.getW().getBlockAt(tmpLoc.getX(), tmpLoc.getY(), tmpLoc.getZ()).getType();
            while (tmpType.equals(mat)) {
                if (!harvestedBlocks.contains(tmpLoc)) {
                    harvestedBlocks.add(tmpLoc);
                    destroyedBlocks.add(tmpLoc);
                }
                tmpLoc = tmpLoc.translate(0, 1, 0);
                tmpType = craft.getW().getBlockAt(tmpLoc.getX(), tmpLoc.getY(), tmpLoc.getZ()).getType();
            }
        } else if (mat.equals(Material.BED_BLOCK)) {
            if (craft.getW().getBlockAt(loc.getX() + 1, loc.getY(), loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(1, 0, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX() - 1, loc.getY(), loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(-1, 0, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
            if (craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ() + 1).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 0, 1);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ() - 1).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 0, -1);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
        }
        //clear from previous because now it is in harvest
        if (destroyedBlocks.contains(loc)) {
            destroyedBlocks.remove(loc);
        }
    }

    public boolean failed(){
        return failed;
    }

    public boolean collisionExplosion() {
        return collisionExplosion;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public Collection<UpdateCommand> getUpdates() {
        return updates;
    }

    public int getDx(){
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDz() {
        return dz;
    }
}
