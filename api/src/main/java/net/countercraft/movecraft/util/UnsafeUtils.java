package net.countercraft.movecraft.util;

import org.jetbrains.annotations.NotNull;
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

    public static @Nullable Field getFieldOfType(@NotNull Class<?> type, @Nullable Class<?> clazz) {
        if (clazz == null)
            return null;
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type) {
                return field;
            }
        }
        return getFieldOfType(type, clazz.getSuperclass());
    }

    public static void trySetFieldOfType(@NotNull Class<?> type, @NotNull Object holder, @NotNull Object value) {
        Field field = getFieldOfType(type, holder.getClass());
        if (field != null) {
            setField(field, holder, value);
        }
    }
}
