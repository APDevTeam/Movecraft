package net.countercraft.movecraft.repair;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepairUtils {
    private static Method GET_BLOCK;
    static {
        try {
            GET_BLOCK = BlockArrayClipboard.class.getDeclaredMethod("getBlock", Vector.class);
        } catch (NoSuchMethodException e) {
            GET_BLOCK = null;
        }
    }


    public static BaseBlock getBlock(Clipboard clipboard, Vector pos){
        BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
        try {
            return GET_BLOCK != null ? (BaseBlock) GET_BLOCK.invoke(bac, pos) : null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }


}
