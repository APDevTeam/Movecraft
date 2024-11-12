package net.countercraft.movecraft.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Lantern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SupportUtils {
    @Nullable
    public static BlockFace getSupportFace(@NotNull BlockData data) {
        if (data instanceof Lantern lantern)
            return lantern.isHanging() ? BlockFace.UP : BlockFace.DOWN;

        if (data instanceof Directional directional) {
            BlockFace normalCase = directional.getFacing().getOppositeFace();

            if (data instanceof FaceAttachable faceAttachable) {
                return switch (faceAttachable.getAttachedFace()) {
                    case FLOOR -> BlockFace.DOWN;
                    case WALL -> normalCase;
                    case CEILING -> BlockFace.UP;
                };
            }
            return normalCase;
        }
        return null;
    }
}
