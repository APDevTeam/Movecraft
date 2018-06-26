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
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.*;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.CraftRotateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
    //private final MovecraftLocation[] blockList;    // used to be final, not sure why. Changed by Mark / Loraxe42
    private Set<UpdateCommand> updates = new HashSet<>();
    //private int[][][] hitbox;
    //private Integer minX, minZ;

    private boolean townyEnabled;
    private Set<TownBlock> townBlockSet;
    private TownyWorld townyWorld;
    private TownyWorldHeightLimits townyWorldHeightLimits;

    private final HashHitBox oldHitBox;
    private final HashHitBox newHitBox;

    public RotationTask(Craft c, MovecraftLocation originPoint, Rotation rotation, World w, boolean isSubCraft) {
        super(c);
        this.originPoint = originPoint;
        this.rotation = rotation;
        this.w = w;
        this.isSubCraft = isSubCraft;
        this.newHitBox = new HashHitBox();
        this.oldHitBox = new HashHitBox(c.getHitBox());
    }

    public RotationTask(Craft c, MovecraftLocation originPoint, Rotation rotation, World w) {
        this(c,originPoint,rotation,w,false);
    }

    @Override
    protected void excecute() {

        if(oldHitBox.isEmpty())
            return;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(getCraft());

        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            failed = true;
            failMessage = I18nSupport.getInternationalisedString("Craft is disabled!");
        }

        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = getCraft().getType().getFuelBurnRate();
        if (fuelBurnRate != 0.0 && !getCraft().getSinking()) {
            if (getCraft().getBurningFuel() < fuelBurnRate) {
                Block fuelHolder = null;
                for (MovecraftLocation bTest : oldHitBox) {
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
        // if a subcraft, find the parent craft. If not a subcraft, it is it's own parent
        Craft[] craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
        Craft parentCraft = getCraft();
        for (Craft craft : craftsInWorld) {
            if ( craft != getCraft() && craft.getHitBox().intersects(oldHitBox)) {
                parentCraft = craft;
                break;
            }
        }

        for(MovecraftLocation originalLocation : oldHitBox){
            MovecraftLocation newLocation = MathUtils.rotateVec(rotation,originalLocation.subtract(originPoint)).add(originPoint);
            newHitBox.add(newLocation);

            Material oldMaterial = originalLocation.toBukkit(w).getBlock().getType();
            //prevent chests collision
            if ((oldMaterial.equals(Material.CHEST) || oldMaterial.equals(Material.TRAPPED_CHEST)) &&
                    !checkChests(oldMaterial, newLocation)) {
                failed = true;
                failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                break;
            }

            // See if they are permitted to build in the area, if WorldGuard integration is turned on
            Location plugLoc = newLocation.toBukkit(w);
            if (craftPilot != null &&
                    Movecraft.getInstance().getWorldGuardPlugin() != null &&
                    Settings.WorldGuardBlockMoveOnBuildPerm &&
                    !Movecraft.getInstance().getWorldGuardPlugin().canBuild(craftPilot, plugLoc)) {
                failed = true;
                failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Player is not permitted to build in this WorldGuard region") + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                break;
            }

            //TODO: ADD TOWNY

            //isTownyBlock(plugLoc,craftPilot);
            Material newMaterial = newLocation.toBukkit(w).getBlock().getType();
            if ((newMaterial == Material.AIR) || (newMaterial == Material.PISTON_EXTENSION) || craft.getType().getPassthroughBlocks().contains(newMaterial)) {
                //getCraft().getPhaseBlocks().put(newLocation, newMaterial);
                continue;
            }

            if (!oldHitBox.contains(newLocation)) {
                failed = true;
                failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                break;
            }
        }
        if (failed) {
            if (this.isSubCraft && parentCraft != getCraft()) {
                parentCraft.setProcessing(false);
            }
            return;
        }
        updates.add(new CraftRotateCommand(getCraft(),originPoint, rotation));
        //rotate entities in the craft
        Location tOP = new Location(getCraft().getW(), originPoint.getX(), originPoint.getY(), originPoint.getZ());

        List<Entity> eList = null;
        int numTries = 0;

        //TODO: Check if needed
        while ((eList == null) && (numTries < 100)) {
            try {
                eList = getCraft().getW().getEntities();
            } catch (java.util.ConcurrentModificationException e) {
                numTries++;
            }
        }
        for (Entity pTest : getCraft().getW().getEntities()) {
            if (MathUtils.locIsNearCraftFast(getCraft(), MathUtils.bukkit2MovecraftLoc(pTest.getLocation())) &&
                    pTest.getType() != EntityType.DROPPED_ITEM) {
                // Player is onboard this craft
                tOP.setX(tOP.getBlockX() + 0.5);
                tOP.setZ(tOP.getBlockZ() + 0.5);
                Location playerLoc = pTest.getLocation();
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
                EntityUpdateCommand eUp = new EntityUpdateCommand(newPLoc, pTest);
                updates.add(eUp);
            }

        }

        if (getCraft().getCruising()) {
            if (rotation == Rotation.ANTICLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case 0x5:
                        getCraft().setCruiseDirection((byte) 0x2);
                        break;
                    // ship faces east
                    case 0x4:
                        getCraft().setCruiseDirection((byte) 0x3);
                        break;
                    // ship faces north
                    case 0x2:
                        getCraft().setCruiseDirection((byte) 0x4);
                        break;
                    // ship faces south
                    case 0x3:
                        getCraft().setCruiseDirection((byte) 0x5);
                        break;
                }
            } else if (rotation == Rotation.CLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case 0x5:
                        getCraft().setCruiseDirection((byte) 0x3);
                        break;
                    // ship faces east
                    case 0x4:
                        getCraft().setCruiseDirection((byte) 0x2);
                        break;
                    // ship faces north
                    case 0x2:
                        getCraft().setCruiseDirection((byte) 0x5);
                        break;
                    // ship faces south
                    case 0x3:
                        getCraft().setCruiseDirection((byte) 0x4);
                        break;
                }
            }
        }

        // if you rotated a subcraft, update the parent with the new blocks
        if (this.isSubCraft) {
            // also find the furthest extent from center and notify the player of the new direction
            int farthestX = 0;
            int farthestZ = 0;
            for (MovecraftLocation loc : newHitBox) {
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
                if (newHitBox.intersects(craft.getHitBox()) && craft != getCraft()) {
                    //newHitBox.addAll(CollectionUtils.filter(craft.getHitBox(),newHitBox));
                    //craft.setHitBox(newHitBox);
                    craft.getHitBox().removeAll(oldHitBox);
                    craft.getHitBox().addAll(newHitBox);
                    break;
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

    public Set<UpdateCommand> getUpdates() {
        return updates;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean getIsSubCraft() {
        return isSubCraft;
    }

    private void isTownyBlock(Location plugLoc, Player craftPilot){
        //towny
        Player p = craftPilot == null ? getCraft().getNotificationPlayer() : craftPilot;
        if (p == null) {
            return;
        }
        if (Movecraft.getInstance().getWorldGuardPlugin() != null && Movecraft.getInstance().getWGCustomFlagsPlugin() != null && Settings.WGCustomFlagsUsePilotFlag) {
            LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(p);
            WGCustomFlagsUtils WGCFU = new WGCustomFlagsUtils();
            if (!WGCFU.validateFlag(plugLoc, Movecraft.FLAG_ROTATE, lp)) {
                failed = true;
                failMessage = String.format(I18nSupport.getInternationalisedString("WGCustomFlags - Rotation Failed") + " @ %d,%d,%d", plugLoc.getX(), plugLoc.getY(), plugLoc.getZ());
                return;
            }
        }

        if (!townyEnabled) {
            return;
        }
        TownBlock townBlock = TownyUtils.getTownBlock(plugLoc);
        if (townBlock == null || townBlockSet.contains(townBlock)) {
            return;
        }
        if (TownyUtils.validateCraftMoveEvent(p, plugLoc, townyWorld)) {
            townBlockSet.add(townBlock);
            return;
        }
        Town town = TownyUtils.getTown(townBlock);
        if (town == null) {
            return;
        }
        Location locSpawn = TownyUtils.getTownSpawn(townBlock);
        if (locSpawn == null || !townyWorldHeightLimits.validate(newHitBox.getMaxY(), locSpawn.getBlockY())) {
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
            failMessage = String.format(I18nSupport.getInternationalisedString("Towny - Rotation Failed") + " %s @ %d,%d,%d", town.getName(), plugLoc.getX(), plugLoc.getY(), plugLoc.getZ());
        }
    }


    private boolean checkChests(Material mBlock, MovecraftLocation newLoc) {
        Material testMaterial;
        MovecraftLocation aroundNewLoc;

        aroundNewLoc = newLoc.translate(1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(-1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, 1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, -1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        return !testMaterial.equals(mBlock) || oldHitBox.contains(aroundNewLoc);
    }

    public HashHitBox getNewHitBox() {
        return newHitBox;
    }
}
