package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.TrackedLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftProperties;
import net.countercraft.movecraft.craft.type.PropertyKey;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ContactBlockHelper {

    static final NamespacedKey TRACTION_BLOCK_LOCATIONS = new NamespacedKey("movecraft", "traction-block-locations");
    static final NamespacedKey TOUCHED_BLOCK_LOCATIONS = new NamespacedKey("movecraft", "blocks-touched-by-craft");
    static final NamespacedKey REQUIRED_CONTACT_BLOCK_LOCATIONS_OUTSIDE = new NamespacedKey("movecraft", "required-contact-blocks-outside");

    static List<MovecraftLocation> genericGetList(final Craft craft, final PropertyKey<BlockSetProperty> blockTypeKey, final NamespacedKey trackingKey, final BiConsumer<Craft, BlockSetProperty> calculationFunction) {
        BlockSetProperty blockTypes = craft.getCraftProperties().get(blockTypeKey);
        if (blockTypes == null || blockTypes.isEmpty()) {
            return new ArrayList<>();
        }

        Set<TrackedLocation> trackingList = craft.getTrackedLocations().getOrDefault(trackingKey, null);
        if (trackingList == null || trackingList.isEmpty()) {
            calculationFunction.accept(craft, blockTypes);
        }
        trackingList = craft.getTrackedLocations().getOrDefault(trackingKey, null);

        List<MovecraftLocation> result = new ArrayList<>();
        trackingList.forEach(tl -> {
            result.add(tl.getAbsoluteLocation());
        });
        return result;
    }

    static List<MovecraftLocation> getTractionBlocksOf(final Craft craft) {
        return genericGetList(craft, PropertyKeys.TRACTION_BLOCKS, TRACTION_BLOCK_LOCATIONS, ContactBlockHelper::calculateTractionBlocks);
    }

    static List<MovecraftLocation> getBlocksCraftIsTouching(final Craft craft) {
        return genericGetList(craft, PropertyKeys.REQUIRED_CONTACT_BLOCKS, TOUCHED_BLOCK_LOCATIONS, ContactBlockHelper::calculateTouchingBlocks);
    }

    static void calculateTractionBlocks(final Craft craft, final BlockSetProperty tractionBlockTypes) {
        Set<TrackedLocation> trackedLocations = craft.getTrackedLocations().computeIfAbsent(TRACTION_BLOCK_LOCATIONS, k -> new HashSet<>());
        List<MovecraftLocation> result = new ArrayList<>();

        if (craft.getTrackedLocations().isEmpty() || trackedLocations == null || trackedLocations.isEmpty()) {
            // Nothing calculated yet! => calculate
            for (MovecraftLocation movecraftLocation : craft.getHitBox()) {
                Location bukkitLocation = movecraftLocation.toBukkit(craft.getWorld());
                if (tractionBlockTypes.contains(NamespacedIDUtil.getBlockID(bukkitLocation.getBlock()))) {
                    trackedLocations.add(new TrackedLocation(craft, movecraftLocation));
                    result.add(movecraftLocation);
                }
            }
        }
        else {
            List<TrackedLocation> toRemove = new ArrayList<>();
            for (TrackedLocation trackedLocation : trackedLocations) {
                MovecraftLocation absoluteLocation = trackedLocation.getAbsoluteLocation();
                Location bukkitLocation = absoluteLocation.toBukkit(craft.getWorld());
                if (tractionBlockTypes.contains(NamespacedIDUtil.getBlockID(bukkitLocation.getBlock()))) {
                    result.add(absoluteLocation);
                } else {
                    toRemove.add(trackedLocation);
                }
            }
            trackedLocations.removeAll(toRemove);
        }
    }

    static final BlockFace CHECK_DIRECTIONS[] = new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH};
    static final MovecraftLocation DELTA = new MovecraftLocation(1, 1,1);

    static void calculateTouchingBlocks(final Craft craft, final BlockSetProperty requiredContactBlockTypes) {
        HitBox hitBox = craft.getHitBox();
        HitBox outerBoundingBox = new SolidHitBox(new MovecraftLocation(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ()).subtract(DELTA), new MovecraftLocation(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ()).add(DELTA));
        for (BlockFace face : CHECK_DIRECTIONS) {
            MovecraftLocation delta = new MovecraftLocation(face.getModX(), face.getModY(), face.getModZ());
            List<MovecraftLocation> outsideLocs = new ArrayList<>();
            for (MovecraftLocation movecraftLocation : hitBox) {
                MovecraftLocation checkLoc = movecraftLocation.add(delta);
                // If that position is NOT part of the original hitbox, it is relevant for us!
                if (!hitBox.inBounds(checkLoc) || !hitBox.contains(checkLoc)) {
                    outsideLocs.add(checkLoc);
                }
            }
            outerBoundingBox = outerBoundingBox.union(new BitmapHitBox(outsideLocs));
        }
        // And now, create the tracked locations
        Set<TrackedLocation> trackedTouching = craft.getTrackedLocations().computeIfAbsent(TOUCHED_BLOCK_LOCATIONS, k -> new HashSet<>());
        trackedTouching.clear();
        Set<TrackedLocation> trackedRequiredContact = craft.getTrackedLocations().computeIfAbsent(REQUIRED_CONTACT_BLOCK_LOCATIONS_OUTSIDE, k -> new HashSet<>());
        trackedRequiredContact.clear();
        for (MovecraftLocation movecraftLocation : outerBoundingBox) {
            // Attention: We need two separate instances! Otherwise we will touch them twice on rotation or transfer!
            NamespacedKey material = NamespacedIDUtil.getBlockID(craft.getMovecraftWorld().getData(movecraftLocation));
            if (requiredContactBlockTypes.contains(material)) {
                trackedRequiredContact.add(new TrackedLocation(craft, movecraftLocation));
            }
            trackedTouching.add(new TrackedLocation(craft, movecraftLocation));
        }
    }

    public static void validateTractionBlocks(Craft craft, List<MovecraftLocation> tractionBlocks) {
        // Check all the traction blocks!
        BlockSetProperty requiredContactBlockTypes = craft.getCraftProperties().get(PropertyKeys.REQUIRED_CONTACT_BLOCKS);
        if (requiredContactBlockTypes == null || requiredContactBlockTypes.isEmpty()) {
            return;
        }
        // Now, validate!
    }

    public static void validateRequiredContactBlocks(Craft craft) {
        // Scan for the required contact blocks!
        BlockSetProperty requiredContactBlockTypes = craft.getCraftProperties().get(PropertyKeys.REQUIRED_CONTACT_BLOCKS);
        if (requiredContactBlockTypes == null || requiredContactBlockTypes.isEmpty()) {
            return;
        }
        // We have required contact blocks! Validate!
    }

    public static void onPilot(CraftPilotEvent event) {
        onCraftUpdate(event.getCraft());
    }

    static void performSingleUpdate(final Craft craft, NamespacedKey trackingKey, PropertyKey<BlockSetProperty> materialSetKey, BiConsumer<Craft, BlockSetProperty> calculationFunction) {
        Set<TrackedLocation> trackedLocations = craft.getTrackedLocations().getOrDefault(trackingKey, null);
        if (trackedLocations != null && !trackedLocations.isEmpty()) {
            trackedLocations.clear();
        }
        BlockSetProperty blockTypes = craft.getCraftProperties().get(materialSetKey);
        if (blockTypes == null || blockTypes.isEmpty()) {
            craft.getTrackedLocations().put(trackingKey, new HashSet<>());
        } else {
            calculationFunction.accept(craft, blockTypes);
        }
    }

    public static void onPreTranslate(CraftPreTranslateEvent event) {
        // Check if the craft itself is still in a valid position
        // TODO: This might be unnecessary!
        if (!isValid(event.getCraft(), event::setFailMessage)) {
            event.setCancelled(true);
        }
        // Now check if the craft will be on a valid position after the rotation
        if (!isValid(event.getCraft(), event::setFailMessage, new MovecraftLocation(event.getDx(), event.getDy(), event.getDz()))) {
            event.setCancelled(true);
        }
    }

    public static void onRotate(CraftRotateEvent event) {
        // Check if the craft itself is still in a valid position
        // TODO: This might be unnecessary!
        if (!isValid(event.getCraft(), event::setFailMessage)) {
            event.setCancelled(true);
        }
        // Now check if the craft will be on a valid position after the rotation
        if (!isValid(event.getCraft(), event::setFailMessage, event.getNewHitBox(), event.getRotation())) {
            event.setCancelled(true);
        }
    }

    public static void onCraftUpdate(Craft craft) {
        // Refresh traction block locations
        performSingleUpdate(craft, TRACTION_BLOCK_LOCATIONS, PropertyKeys.TRACTION_BLOCKS, ContactBlockHelper::calculateTractionBlocks);
        performSingleUpdate(craft, TOUCHED_BLOCK_LOCATIONS, PropertyKeys.REQUIRED_CONTACT_BLOCKS, ContactBlockHelper::calculateTouchingBlocks);
    }

    static boolean isValid(final Craft craft, Consumer<String> failureMessageSetter, final HitBox hitBox, MovecraftRotation rotation) {
        return isValid(craft, failureMessageSetter, hitBox, Optional.of(rotation), Optional.empty());
    }

    static boolean isValid(final Craft craft, Consumer<String> failureMessageSetter, MovecraftLocation deltaTranslation) {
        // First: Compute the translated hitbox, then pass that to the other method!
        BitmapHitBox hitBox = new BitmapHitBox();
        for (MovecraftLocation movecraftLocation : craft.getHitBox()) {
            hitBox.add(movecraftLocation.add(deltaTranslation));
        }
        return isValid(craft, failureMessageSetter, hitBox, deltaTranslation);
    }

    static boolean isValid(final Craft craft, Consumer<String> failureMessageSetter, final HitBox hitBox, MovecraftLocation deltaTranslation) {
        return isValid(craft, failureMessageSetter, hitBox, Optional.empty(), Optional.of(deltaTranslation));
    }

    static boolean isValid(final Craft craft, Consumer<String> failureMessageSetter) {
        return isValid(craft, failureMessageSetter, craft.getHitBox(), Optional.empty(), Optional.empty());
    }

    // TODO: Move to MOVECRAFT module and provide failure message!
    // DONE: Add parameters for translation and rotation (defaults to null). If those are set, we need to transform the positions first!
    static boolean isValid(final Craft craft, Consumer<String> failureMessageSetter, final HitBox hitBox, Optional<MovecraftRotation> optRot, Optional<MovecraftLocation> optDelta) {
        // First: Do we even use traction blocks?
        // If yes, validate we have any
        // And then, check if the traction blocks are touching the required contact blocks
        // Second: If we dont use traction blocks, do we use required contact blocks?
        // If yes, we must at least touch ONE of them
        // This means, that the REQUIRED_CONTACT_BLOCK_LOCATIONS_OUTSIDE set must not be empty!
        if (craft instanceof SinkingCraft || !(craft instanceof PilotedCraft)) {
            return true;
        }
        final CraftProperties craftType = craft.getCraftProperties();

        BlockSetProperty tractionBlockSet = craftType.get(PropertyKeys.TRACTION_BLOCKS);
        BlockSetProperty requiredContactBlockSet = craftType.get(PropertyKeys.REQUIRED_CONTACT_BLOCKS);

        boolean tractionBlocksConfigured = tractionBlockSet != null && !tractionBlockSet.isEmpty();
        boolean contactBlocksCongirued = requiredContactBlockSet != null && !requiredContactBlockSet.isEmpty();
        // If we dont have any config at all, we can quit...
        // Also if we dont have any contact blocks specified!
        if ((!tractionBlocksConfigured && !contactBlocksCongirued)) {
            return true;
        }

        final HitBox bitmapHitBox = new BitmapHitBox(hitBox);

        if (tractionBlocksConfigured) {
            // TODO: Make this a property of each entry!
            // TODO: Move to own method
            for (TrackedLocation trackedTractionBlock : craft.getTrackedLocations().getOrDefault(TRACTION_BLOCK_LOCATIONS, Set.of())) {
                TrackedLocation trackedLocationToUse = new TrackedLocation(trackedTractionBlock);
                if (optRot.isPresent()) {
                    trackedLocationToUse.rotate(optRot.get());
                }
                MovecraftLocation movecraftLocation = trackedLocationToUse.getAbsoluteLocation();
                if (optDelta.isPresent()) {
                    movecraftLocation = movecraftLocation.add(optDelta.get());
                }
                boolean foundAtLeastOneBlock = false;
                boolean foundAtLeastOneTouchingOutSide = false;
                for (BlockFace face : CHECK_DIRECTIONS) {
                    MovecraftLocation checkLocation = movecraftLocation.add(face.getModX(), face.getModY(), face.getModZ());
                    if (bitmapHitBox.inBounds(checkLocation) && bitmapHitBox.contains(checkLocation)) {
                        continue;
                    }
                    foundAtLeastOneTouchingOutSide = true;
                    NamespacedKey checkForContactBlock = NamespacedIDUtil.getBlockID(craft.getMovecraftWorld().getData(checkLocation));
                    if (contactBlocksCongirued) {
                        foundAtLeastOneBlock = requiredContactBlockSet.contains(checkForContactBlock);
                    } else {
                        foundAtLeastOneBlock = !craft.getMovecraftWorld().getMaterial(checkLocation).isAir();
                    }
                }
                // If we dont touch any outside block we can just continue as in this case the block isnt a "wheel"
                if (!foundAtLeastOneTouchingOutSide) {
                    continue;
                }
                // TODO: Do all traction blocks need to touch the contact blocks? => Implementation wise it currently is the case that all traction blocks must touch one
                if (!foundAtLeastOneBlock) {
                    // We failed :(
                    return false;
                }
            }
        } else {
            if (optDelta.isPresent() || optRot.isPresent()) {
                // TODO: Scan through all touching blocks. Then rotate and translate them. Then check against the enumset
            } else {
                return !craft.getTrackedLocations().getOrDefault(REQUIRED_CONTACT_BLOCK_LOCATIONS_OUTSIDE, Set.of()).isEmpty();
            }
        }
        return false;
    }

    public static void onDetect(CraftDetectEvent event) {
        if (event.getCraft().getTrackedLocations().getOrDefault(TRACTION_BLOCK_LOCATIONS, null) == null || event.getCraft().getTrackedLocations().getOrDefault(TOUCHED_BLOCK_LOCATIONS, null) == null) {
            onCraftUpdate(event.getCraft());
        }
        // We now have the data!
        // Now we need to verify!
        if (!isValid(event.getCraft(), event::setFailMessage)) {
            event.setCancelled(true);
        }
    }
}
