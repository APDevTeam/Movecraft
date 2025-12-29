package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.craft.type.property.BlockSetProperty;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.NamespacedIDUtil;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// TODO: Give access to the tracked locations maybe?
public class TractionBlockValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, TypeSafeCraftType> {

    static final BlockFace CHECK_DIRECTIONS[] = new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH};
    static final MovecraftLocation DELTA = new MovecraftLocation(1, 1,1);

    @Override
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld movecraftWorld, @NotNull HitBox hitBox, @NotNull TypeSafeCraftType craftType) {
        BlockSetProperty tractionBlockTypes = craftType.get(PropertyKeys.TRACTION_BLOCKS);
        BlockSetProperty requiredContactBlockTypes = craftType.get(PropertyKeys.REQUIRED_CONTACT_BLOCKS);
        if ((tractionBlockTypes == null || tractionBlockTypes.isEmpty()) && (requiredContactBlockTypes == null || requiredContactBlockTypes.isEmpty())) {
            return Result.succeed();
        }
        boolean checkTractionBlocks = tractionBlockTypes != null && !tractionBlockTypes.isEmpty();
        if (checkTractionBlocks) {
            for (MovecraftLocation movecraftLocation : hitBox) {
                NamespacedKey block = NamespacedIDUtil.getBlockID(movecraftWorld.getData(movecraftLocation.add(translation)));
                if (tractionBlockTypes.contains(block)) {
                    boolean foundAtLeastOneBlock = false;
                    for (BlockFace face : CHECK_DIRECTIONS) {
                        MovecraftLocation checkLocation = movecraftLocation.add(translation).add(new MovecraftLocation(face.getModX(), face.getModY(), face.getModZ()));
                        //Only check traction blocks OUTSIDE of the craft!
                        if (hitBox.inBounds(checkLocation) && hitBox.contains(checkLocation)) {
                            continue;
                        }

                        BlockData checkForContactBlockData = movecraftWorld.getData(checkLocation);
                        NamespacedKey checkForContactBlock = NamespacedIDUtil.getBlockID(checkForContactBlockData);
                        if (requiredContactBlockTypes == null || requiredContactBlockTypes.isEmpty()) {
                            foundAtLeastOneBlock = !checkForContactBlockData.getMaterial().isAir();
                        } else if (requiredContactBlockTypes.contains(checkForContactBlock)) {
                            foundAtLeastOneBlock = true;
                        }

                        if (foundAtLeastOneBlock) {
                            break;
                        }
                    }
                    if (!foundAtLeastOneBlock) {
                        return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft will lose traction"));
                    }
                }
            }
        } else {
            // No traction blocks! => Check for the blocks to be within a expanded hitbox of the craft
            // Collect all blocks that are directly next to a craft's blocks after the movement
            // Then check if at least one of them is a required contact block!
            HitBox postMoveHitBox = new SolidHitBox(new MovecraftLocation(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ()).add(translation), new MovecraftLocation(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ()).add(translation));
            HitBox checkHitBox = new BitmapHitBox(new SolidHitBox(new MovecraftLocation(postMoveHitBox.getMinX(), postMoveHitBox.getMinY(), postMoveHitBox.getMinZ()).subtract(DELTA), new MovecraftLocation(postMoveHitBox.getMaxX(), postMoveHitBox.getMaxY(), postMoveHitBox.getMaxZ()).add(DELTA)));
            List<MovecraftLocation> translatedPositions = new ArrayList<>();
            for (MovecraftLocation movecraftLocation : hitBox) {
                translatedPositions.add(movecraftLocation.add(translation));
            }
            postMoveHitBox = new BitmapHitBox(translatedPositions);
            for (BlockFace face : CHECK_DIRECTIONS) {
                MovecraftLocation delta = new MovecraftLocation(face.getModX(), face.getModY(), face.getModZ());
                List<MovecraftLocation> outsideLocs = new ArrayList<>();
                for (MovecraftLocation movecraftLocation : postMoveHitBox) {
                    MovecraftLocation checkLoc = movecraftLocation.add(delta);
                    if (!postMoveHitBox.inBounds(checkLoc) || !postMoveHitBox.contains(checkLoc)) {
                        outsideLocs.add(checkLoc);
                    }
                }
                checkHitBox = checkHitBox.union(new BitmapHitBox(outsideLocs));
            }
            for (MovecraftLocation movecraftLocation : checkHitBox) {
                BlockData blockBlockData = movecraftWorld.getData(movecraftLocation);
                NamespacedKey block = NamespacedIDUtil.getBlockID(blockBlockData);
                if (blockBlockData.getMaterial().isAir()) {
                    continue;
                }
                if (requiredContactBlockTypes.contains(block)) {
                    return Result.succeed();
                }
            }
            return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft will leave required contact blocks"));
        }
        return Result.succeed();
    }
}
