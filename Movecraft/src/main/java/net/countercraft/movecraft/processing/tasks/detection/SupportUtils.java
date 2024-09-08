package net.countercraft.movecraft.processing.tasks.detection;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Hangable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SupportUtils {
    @Nullable
    public static BlockFace getSupportFace(@NotNull BlockData data) {
        return switch (data) {
            case Hangable hangable -> hangable.isHanging() ? BlockFace.UP : BlockFace.DOWN;
            case FaceAttachable faceAttachable -> switch (faceAttachable.getAttachedFace()) {
                case FLOOR -> BlockFace.DOWN;
                case WALL ->
                        faceAttachable instanceof Directional directional ? directional.getFacing().getOppositeFace() : null;
                case CEILING -> BlockFace.UP;
            };
            case Directional directional -> directional.getFacing().getOppositeFace();
            default -> null;
        };
    }
}
