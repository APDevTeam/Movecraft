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
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.CraftRotateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

import static net.countercraft.movecraft.utils.MathUtils.withinWorldBorder;

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

    private final BitmapHitBox oldHitBox;
    private final BitmapHitBox newHitBox;
    private final BitmapHitBox oldFluidList;
    private final BitmapHitBox newFluidList;

    public RotationTask(Craft c, MovecraftLocation originPoint, Rotation rotation, World w, boolean isSubCraft) {
        super(c);
        this.originPoint = originPoint;
        this.rotation = rotation;
        this.w = w;
        this.isSubCraft = isSubCraft;
        this.newHitBox = new BitmapHitBox();
        this.oldHitBox = new BitmapHitBox(c.getHitBox());
        this.oldFluidList = new BitmapHitBox(c.getFluidLocations());
        this.newFluidList = new BitmapHitBox(c.getFluidLocations());
    }

    public RotationTask(Craft c, MovecraftLocation originPoint, Rotation rotation, World w) {
        this(c,originPoint,rotation,w,false);
    }

    @Override
    protected void execute() {

        if(oldHitBox.isEmpty())
            return;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(getCraft());

        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            failed = true;
            failMessage = I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled");
        }

        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = getCraft().getType().getFuelBurnRate(getCraft().getW());
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
        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
        Craft parentCraft = getCraft();
        for (Craft craft : craftsInWorld) {
            if ( craft != getCraft() && !craft.getHitBox().intersection(oldHitBox).isEmpty()) {
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
            if (!withinWorldBorder(craft.getW(), newLocation)) {
                failMessage = I18nSupport.getInternationalisedString("Rotation - Failed Craft cannot pass world border") + String.format(" @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                failed = true;
                return;
            }

            Material newMaterial = newLocation.toBukkit(w).getBlock().getType();
            if ((newMaterial == Material.AIR) || (newMaterial == Material.PISTON_EXTENSION) || craft.getType().getPassthroughBlocks().contains(newMaterial)) {
                //getCraft().getPhaseBlocks().put(newLocation.toBukkit(w), newMaterial);
                continue;
            }

            if (!oldHitBox.contains(newLocation)) {
                failed = true;
                failMessage = String.format(I18nSupport.getInternationalisedString("Rotation - Craft is obstructed") + " @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                break;
            }
        }

        if (!oldFluidList.isEmpty()) {
            for (MovecraftLocation fluidLoc : oldFluidList) {
                newFluidList.add(MathUtils.rotateVec(rotation, fluidLoc.subtract(originPoint)).add(originPoint));
            }
        }

        if (failed) {
            if (this.isSubCraft && parentCraft != getCraft()) {
                parentCraft.setProcessing(false);
            }
            return;
        }
        //call event
        CraftRotateEvent event = new CraftRotateEvent(craft, rotation, originPoint, oldHitBox, newHitBox);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            failed = true;
            failMessage = event.getFailMessage();
            return;
        }
        if (parentCraft != craft) {
            parentCraft.getFluidLocations().removeAll(oldFluidList);
            parentCraft.getFluidLocations().addAll(newFluidList);
        }


        updates.add(new CraftRotateCommand(getCraft(),originPoint, rotation));
        //rotate entities in the craft
        Location tOP = new Location(getCraft().getW(), originPoint.getX(), originPoint.getY(), originPoint.getZ());
        tOP.setX(tOP.getBlockX() + 0.5);
        tOP.setZ(tOP.getBlockZ() + 0.5);

        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            Location midpoint = new Location(
                    craft.getW(),
                    (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                    (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                    (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
            for(Entity entity : craft.getW().getNearbyEntities(midpoint, oldHitBox.getXLength()/2.0 + 1, oldHitBox.getYLength()/2.0 + 2, oldHitBox.getZLength()/2.0 + 1)){
                if ((entity.getType() == EntityType.PLAYER && !craft.getSinking()) || !craft.getType().getOnlyMovePlayers()) {
                    // Player is onboard this craft

                    Location adjustedPLoc = entity.getLocation().subtract(tOP);

                    double[] rotatedCoords = MathUtils.rotateVecNoRound(rotation, adjustedPLoc.getX(), adjustedPLoc.getZ());
                    float newYaw = rotation == Rotation.CLOCKWISE ? 90F : -90F;
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, rotatedCoords[0] + tOP.getX() - entity.getLocation().getX(), 0, rotatedCoords[1] + tOP.getZ() - entity.getLocation().getZ(), newYaw, 0);
                    updates.add(eUp);
                }
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
            String faceMessage = I18nSupport.getInternationalisedString("Rotation - Farthest Extent Facing");
            faceMessage += " ";
            if (Math.abs(farthestX) > Math.abs(farthestZ)) {
                if (farthestX > 0) {
                    if (getCraft().getNotificationPlayer() != null)
                        faceMessage += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - East");
                } else {
                    if (getCraft().getNotificationPlayer() != null)
                        faceMessage += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - West");
                }
            } else {
                if (farthestZ > 0) {
                    if (getCraft().getNotificationPlayer() != null)
                        faceMessage += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - South");
                } else {
                    if (getCraft().getNotificationPlayer() != null)
                        faceMessage += I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - North");
                }
            }
            getCraft().getNotificationPlayer().sendMessage(faceMessage);

            craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
            for (Craft craft : craftsInWorld) {
                if (!newHitBox.intersection(craft.getHitBox()).isEmpty() && craft != getCraft()) {
                    //newHitBox.addAll(CollectionUtils.filter(craft.getHitBox(),newHitBox));
                    //craft.setHitBox(newHitBox);
                    if (Settings.Debug) {
                        Bukkit.broadcastMessage(String.format("Size of %s hitbox: %d, Size of %s hitbox: %d", this.craft.getType().getCraftName(), newHitBox.size(), craft.getType().getCraftName(), craft.getHitBox().size()));
                    }
                    craft.getHitBox().removeAll(oldHitBox);
                    craft.getHitBox().addAll(newHitBox);
                    if (Settings.Debug){
                        Bukkit.broadcastMessage(String.format("Hitbox of craft %s intersects hitbox of craft %s", this.craft.getType().getCraftName(), craft.getType().getCraftName()));
                        Bukkit.broadcastMessage(String.format("Size of %s hitbox: %d, Size of %s hitbox: %d", this.craft.getType().getCraftName(), newHitBox.size(), craft.getType().getCraftName(), craft.getHitBox().size()));
                    }
                    break;
                }
            }
        }


    }

    private static HitBox rotateHitBox(HitBox hitBox, MovecraftLocation originPoint, Rotation rotation){
        MutableHitBox output = new HashHitBox();
        for(MovecraftLocation location : hitBox){
            output.add(MathUtils.rotateVec(rotation,originPoint.subtract(originPoint)).add(originPoint));
        }
        return output;
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



    public BitmapHitBox getNewHitBox() {
        return newHitBox;
    }

    public BitmapHitBox getNewFluidList() {
        return newFluidList;
    }
}
