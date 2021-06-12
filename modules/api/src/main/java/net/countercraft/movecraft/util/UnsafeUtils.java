package net.countercraft.movecraft.util;

import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
    private static @Nullable Unsafe unsafe;
    static {
        Unsafe defered;
        try{
            var field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            defered = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            defered = null;
        }
        unsafe = defered;
    }

    public static void setField(Field field, Object holder, Object value){
        if(unsafe == null){
            return;
        }
        unsafe.putObject(holder, unsafe.objectFieldOffset(field), value);
    }
}
