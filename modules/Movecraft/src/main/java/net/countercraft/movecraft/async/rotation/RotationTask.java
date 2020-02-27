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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
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
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
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
    protected void execute() {

        if(oldHitBox.isEmpty())
            return;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(getCraft());


        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            failed = true;
            failMessage = I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled");
        }
        if (craft.getType().getFuelBurnRate() > 0.0) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
                    double fuelBurnRate = craft.getType().getFuelBurnRate();
                    // going down doesn't require fuel
                    if (fuelBurnRate == 0.0 || craft.getSinking()) {
                        return;
                    }
                    if (craft.getBurningFuel() >= fuelBurnRate) {
                        craft.setBurningFuel(craft.getBurningFuel() - fuelBurnRate);
                        return;
                    }
                    Block fuelHolder = null;
                    for (MovecraftLocation bTest : oldHitBox) {
                        Block b = craft.getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
                        //Get all fuel holders
                        if (b.getType() == Material.FURNACE) {
                            InventoryHolder holder = (InventoryHolder) b.getState();
                            for (Material fuel : Settings.FuelTypes.keySet()) {
                                if (holder.getInventory().contains(fuel)) {
                                    fuelHolder = b;
                                    break;
                                }
                            }
                        }
                        if (fuelHolder != null) break;

                    }
                    if (fuelHolder == null) {
                        failMessage = I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel");
                        failed = true;
                        return;
                    }
                    Furnace furnace = (Furnace) fuelHolder.getState();
                    for (Material fuel : Settings.FuelTypes.keySet()){
                        if (furnace.getInventory().contains(fuel)){
                            ItemStack item = furnace.getInventory().getItem(furnace.getInventory().first(fuel));
                            int amount = item.getAmount();
                            if (amount == 1) {
                                furnace.getInventory().remove(item);
                            } else {
                                item.setAmount(amount - 1);
                            }
                            craft.setBurningFuel(craft.getBurningFuel() + Settings.FuelTypes.get(item.getType()));
                        }
                    }
                }
            }.runTask(Movecraft.getInstance());

        }
        // if a subcraft, find the parent craft. If not a subcraft, it is it's own parent
        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getW());
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
            WorldBorder border = craft.getW().getWorldBorder();
            boolean inside = Settings.IsPre1_9 ? plugLoc.subtract(border.getCenter()).getBlockX() <= border.getSize() &&
                    plugLoc.subtract(border.getCenter()).getBlockZ() <= border.getSize() :
                    craft.getW().getWorldBorder().isInside(plugLoc);

            if (!inside) {
                failMessage = I18nSupport.getInternationalisedString("Rotation - Failed Craft cannot pass world border");
                failed = true;
                return;
            }

            //TODO: ADD TOWNY
            //TODO: ADD FACTIONS
            //isTownyBlock(plugLoc,craftPilot);
            Material newMaterial = newLocation.toBukkit(w).getBlock().getType();
            if ((newMaterial == Material.AIR) || (newMaterial == LegacyUtils.PISTON_EXTENSION) || craft.getType().getPassthroughBlocks().contains(newMaterial)) {
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
        //call event
        CraftRotateEvent event = new CraftRotateEvent(craft, rotation, originPoint, oldHitBox, newHitBox);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            failed = true;
            failMessage = event.getFailMessage();
            return;
        }


        updates.add(new CraftRotateCommand(getCraft(),originPoint, rotation));


        if (getCraft().getCruising()) {
            if (rotation == Rotation.ANTICLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case WEST:
                        getCraft().setCruiseDirection(BlockFace.SOUTH);
                        break;
                    // ship faces east
                    case EAST:
                        getCraft().setCruiseDirection(BlockFace.NORTH);
                        break;
                    // ship faces north
                    case NORTH:
                        getCraft().setCruiseDirection(BlockFace.WEST);
                        break;
                    // ship faces south
                    case SOUTH:
                        getCraft().setCruiseDirection(BlockFace.EAST);
                        break;
                }
            } else if (rotation == Rotation.CLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case WEST:
                        getCraft().setCruiseDirection(BlockFace.NORTH);
                        break;
                    // ship faces east
                    case EAST:
                        getCraft().setCruiseDirection(BlockFace.SOUTH);
                        break;
                    // ship faces north
                    case NORTH:
                        getCraft().setCruiseDirection(BlockFace.EAST);
                        break;
                    // ship faces south
                    case SOUTH:
                        getCraft().setCruiseDirection(BlockFace.WEST);
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
                if (newHitBox.intersects(craft.getHitBox()) && craft != getCraft()) {
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
                ApplicableRegionSet regions;
                if (Settings.IsLegacy){
                    regions = LegacyUtils.getApplicableRegions(LegacyUtils.getRegionManager(Movecraft.getInstance().getWorldGuardPlugin(), plugLoc.getWorld()), plugLoc);//.getApplicableRegions();
                } else {
                    regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(plugLoc.getWorld())).getApplicableRegions(BlockVector3.at(plugLoc.getBlockX(), plugLoc.getBlockY(), plugLoc.getBlockZ()));
                }

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

