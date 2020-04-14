package net.countercraft.movecraft.utils;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SignUtils {
    /**
     * @param sign
     * @return
     */
    public static BlockFace getFacing(@NotNull Sign sign) {
        if (Settings.IsLegacy) {
            MaterialData materialData = sign.getData();
            org.bukkit.material.Sign matSign = (org.bukkit.material.Sign) materialData;
            return matSign.getFacing();
        }
        BlockData data = sign.getBlockData();
        if (data instanceof WallSign){
            WallSign ws = (WallSign) data;
            return ws.getFacing();
        }
        org.bukkit.block.data.type.Sign s = (org.bukkit.block.data.type.Sign) data;
        return s.getRotation();
    }

    public static boolean isSign(@Nullable Block block){
        if (block == null)
            return false;
        final Material type = block.getType();

        return type.name().equals("SIGN_POST") ||
                type.name().equals("SIGN") ||
                type.name().equals("WALL_SIGN") ||
                type.name().endsWith("_SIGN") ||
                type.name().endsWith("_WALL_SIGN");
    }

    public static boolean isWallSign(@Nullable Block block) {
        if (block == null)
            return false;
        final Material type = block.getType();

        return type.name().equals("WALL_SIGN") ||
                type.name().endsWith("_WALL_SIGN");
    }
}
