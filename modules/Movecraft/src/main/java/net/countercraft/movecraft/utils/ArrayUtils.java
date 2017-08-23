package net.countercraft.movecraft.utils;

import java.util.Arrays;
import java.util.List;

public class ArrayUtils {
    public static <T> List<T> subtractAsList(T[] array1, T[] array2){
        List<T> list = Arrays.asList(array1);
        list.removeAll(Arrays.asList(array2));
        return list;
    }

    public static <T> T[] subtract(T[] array1, T[] array2){
        List<T> list = Arrays.asList(array1);
        list.removeAll(Arrays.asList(array2));
        return list.toArray(array1.clone());
    }


}
