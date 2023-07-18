package net.countercraft.movecraft.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

public class SignUtils {
    /**
     * @param sign
     * @return
     */
    public static BlockFace getFacing(Sign sign) {
        BlockData blockData = sign.getBlockData();
        if(blockData instanceof Directional){
            return ((Directional) blockData).getFacing();
        }
        return BlockFace.SELF;
    }
}
