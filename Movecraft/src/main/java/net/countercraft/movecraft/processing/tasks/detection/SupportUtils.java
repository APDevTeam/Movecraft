package net.countercraft.movecraft.processing.tasks.detection;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Lantern;

import java.util.Optional;

/**
 * @author Intybyte/Vaan1310
 * An util craft that uses block data to get its supporting block
 */

public class SupportUtils {
    public static Optional<BlockFace> getSupportFace(BlockData data) {

        if (data.getMaterial() == Material.LADDER) {
            BlockFace face = ((Directional) data).getFacing().getOppositeFace();
            return Optional.of(face);
        }

        //TODO: Use pattern matched switch statements once we update do Java 21
        //TODO: This should become Hangable instead when we drop support for 1.18
        if (data instanceof Lantern lantern)
            return Optional.of(lantern.isHanging() ? BlockFace.UP : BlockFace.DOWN);

        if (data instanceof FaceAttachable faceAttachable && data instanceof Directional directional) {
            return switch (faceAttachable.getAttachedFace()) {
                case FLOOR -> Optional.of(BlockFace.DOWN);
                case WALL -> Optional.of(directional.getFacing().getOppositeFace());
                case CEILING -> Optional.of(BlockFace.UP);
            };
        }

        return Optional.empty();
    }
}
