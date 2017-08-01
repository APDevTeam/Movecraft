package net.countercraft.movecraft.utils;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.MaterialData;

public class SignUtils {
    /**
     * @param sign
     * @return
     */
    public static BlockFace getFacing(Sign sign) {
        MaterialData materialData = sign.getData();
        org.bukkit.material.Sign matSign = (org.bukkit.material.Sign) materialData;
        return matSign.getFacing();
    }
}
