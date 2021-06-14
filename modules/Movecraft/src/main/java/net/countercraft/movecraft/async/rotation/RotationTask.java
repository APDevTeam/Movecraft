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

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.CraftRotateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static net.countercraft.movecraft.util.MathUtils.withinWorldBorder;

public class RotationTask extends AsyncTask {
    private final MovecraftLocation originPoint;
    private final MovecraftRotation rotation;
    private final World w;
    private final boolean isSubCraft;
    private boolean failed = false;
    private String failMessage;
    //private final MovecraftLocation[] blockList;    // used to be final, not sure why. Changed by Mark / Loraxe42
    private Set<UpdateCommand> updates = new HashSet<>();

    private final MutableHitBox oldHitBox;
    private final MutableHitBox newHitBox;
    private final MutableHitBox oldFluidList;
    private final MutableHitBox newFluidList;

    public RotationTask(Craft c, MovecraftLocation originPoint, MovecraftRotation rotation, World w, boolean isSubCraft) {
        super(c);
        this.originPoint = originPoint;
        this.rotation = rotation;
        this.w = w;
        this.isSubCraft = isSubCraft;
        this.newHitBox = new SetHitBox();
        this.oldHitBox = new SetHitBox(c.getHitBox());
        this.oldFluidList = new SetHitBox(c.getFluidLocations());
        this.newFluidList = new SetHitBox(c.getFluidLocations());
    }

    public RotationTask(Craft c, MovecraftLocation originPoint, MovecraftRotation rotation, World w) {
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
        if (!checkFuel()) {
            failMessage = I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel");
            failed = true;
            return;
        }
        // if a subcraft, find the parent craft. If not a subcraft, it is it's own parent
        Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getWorld());
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

            if (!withinWorldBorder(craft.getWorld(), newLocation)) {
                failMessage = I18nSupport.getInternationalisedString("Rotation - Failed Craft cannot pass world border") + String.format(" @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ());
                failed = true;
                return;
            }

            Material newMaterial = newLocation.toBukkit(w).getBlock().getType();
            if ((newMaterial == Material.AIR) || (newMaterial == Material.PISTON_HEAD) || craft.getType().getPassthroughBlocks().contains(newMaterial)) {
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
        Location tOP = new Location(getCraft().getWorld(), originPoint.getX(), originPoint.getY(), originPoint.getZ());
        tOP.setX(tOP.getBlockX() + 0.5);
        tOP.setZ(tOP.getBlockZ() + 0.5);

        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            Location midpoint = new Location(
                    craft.getWorld(),
                    (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                    (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                    (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
            for(Entity entity : craft.getWorld().getNearbyEntities(midpoint, oldHitBox.getXLength()/2.0 + 1, oldHitBox.getYLength()/2.0 + 2, oldHitBox.getZLength()/2.0 + 1)){
                if (((entity.getType() == EntityType.PLAYER || entity.getType() == EntityType.PRIMED_TNT) && !craft.getSinking()) || !craft.getType().getOnlyMovePlayers()) {
                    // Player is onboard this craft

                    Location adjustedPLoc = entity.getLocation().subtract(tOP);

                    double[] rotatedCoords = MathUtils.rotateVecNoRound(rotation, adjustedPLoc.getX(), adjustedPLoc.getZ());
                    float newYaw = rotation == MovecraftRotation.CLOCKWISE ? 90F : -90F;
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, rotatedCoords[0] + tOP.getX() - entity.getLocation().getX(), 0, rotatedCoords[1] + tOP.getZ() - entity.getLocation().getZ(), newYaw, 0);
                    updates.add(eUp);
                }
            }
        }

        if (getCraft().getCruising()) {
            if (rotation == MovecraftRotation.ANTICLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case WEST:
                        getCraft().setCruiseDirection(CruiseDirection.SOUTH);
                        break;
                    // ship faces east
                    case EAST:
                        getCraft().setCruiseDirection(CruiseDirection.NORTH);
                        break;
                    // ship faces north
                    case SOUTH:
                        getCraft().setCruiseDirection(CruiseDirection.EAST);
                        break;
                    // ship faces south
                    case NORTH:
                        getCraft().setCruiseDirection(CruiseDirection.WEST);
                        break;
                }
            } else if (rotation == MovecraftRotation.CLOCKWISE) {
                // ship faces west
                switch (getCraft().getCruiseDirection()) {
                    case WEST:
                        getCraft().setCruiseDirection(CruiseDirection.NORTH);
                        break;
                    // ship faces east
                    case EAST:
                        getCraft().setCruiseDirection(CruiseDirection.SOUTH);
                        break;
                    // ship faces north
                    case SOUTH:
                        getCraft().setCruiseDirection(CruiseDirection.WEST);
                        break;
                    // ship faces south
                    case NORTH:
                        getCraft().setCruiseDirection(CruiseDirection.EAST);
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
            Component faceMessage = I18nSupport.getInternationalisedComponent("Rotation - Farthest Extent Facing")
                    .append(Component.text(" "));
            if (Math.abs(farthestX) > Math.abs(farthestZ)) {
                if (farthestX > 0) {
                    faceMessage = faceMessage.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - East"));
                } else {
                    faceMessage = faceMessage.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - West"));
                }
            } else {
                if (farthestZ > 0) {
                    faceMessage = faceMessage.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - South"));
                } else {
                    faceMessage = faceMessage.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - North"));
                }
            }
            getCraft().getAudience().sendMessage(faceMessage);

            craftsInWorld = CraftManager.getInstance().getCraftsInWorld(getCraft().getWorld());
            for (Craft craft : craftsInWorld) {
                if (!newHitBox.intersection(craft.getHitBox()).isEmpty() && craft != getCraft()) {
                    //newHitBox.addAll(CollectionUtils.filter(craft.getHitBox(),newHitBox));
                    //craft.setHitBox(newHitBox);
                    if (Settings.Debug) {
                        Bukkit.broadcastMessage(String.format("Size of %s hitbox: %d, Size of %s hitbox: %d", this.craft.getType().getCraftName(), newHitBox.size(), craft.getType().getCraftName(), craft.getHitBox().size()));
                    }
                    craft.setHitBox(craft.getHitBox().difference(oldHitBox).union(newHitBox));
                    if (Settings.Debug){
                        Bukkit.broadcastMessage(String.format("Hitbox of craft %s intersects hitbox of craft %s", this.craft.getType().getCraftName(), craft.getType().getCraftName()));
                        Bukkit.broadcastMessage(String.format("Size of %s hitbox: %d, Size of %s hitbox: %d", this.craft.getType().getCraftName(), newHitBox.size(), craft.getType().getCraftName(), craft.getHitBox().size()));
                    }
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

    public MovecraftRotation getRotation() {
        return rotation;
    }

    public boolean getIsSubCraft() {
        return isSubCraft;
    }

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc) {
        Material testMaterial;
        MovecraftLocation aroundNewLoc;

        aroundNewLoc = newLoc.translate(1, 0, 0);
        testMaterial = craft.getWorld().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(-1, 0, 0);
        testMaterial = craft.getWorld().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, 1);
        testMaterial = craft.getWorld().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, -1);
        testMaterial = craft.getWorld().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        return !testMaterial.equals(mBlock) || oldHitBox.contains(aroundNewLoc);
    }



    public MutableHitBox getNewHitBox() {
        return newHitBox;
    }

    public MutableHitBox getNewFluidList() {
        return newFluidList;
    }
}
