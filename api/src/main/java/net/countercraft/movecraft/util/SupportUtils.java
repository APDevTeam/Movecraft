package net.countercraft.movecraft.util;

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
            case Directional directional -> directional.getFacing().getOppositeFace();
            case Hangable hangable when hangable.isHanging() -> BlockFace.UP;
            case Hangable hangable when !hangable.isHanging() -> BlockFace.DOWN;
            case FaceAttachable faceAttachable -> switch (faceAttachable.getAttachedFace()) {
                case FLOOR -> BlockFace.DOWN;
                case CEILING -> BlockFace.UP;
                case WALL -> null; // Wall attachable should be Directional
            };
            default -> null;
        };
    }
}
