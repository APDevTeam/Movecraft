package net.countercraft.movecraft.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;

public class SignUtils {
    /**
     * @param sign
     * @return
     */
    public static BlockFace getFacing(Sign sign) {
        BlockData blockData = sign.getBlockData();
        if(blockData instanceof WallSign){
            return ((WallSign) blockData).getFacing();
        }
        return BlockFace.SELF;
    }
}
