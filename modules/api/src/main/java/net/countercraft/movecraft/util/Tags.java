package net.countercraft.movecraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class Tags {

    @Nullable
    public static EnumSet<Material> parseBlockRegistry(@NotNull String string){
        if(!string.startsWith("#")){
            return null;
        }
        String nameKey = string.substring(1);
        var key = keyFromString(nameKey);
        if(key == null){
            throw new IllegalArgumentException("Entry " + string + " is not a valid tag!");
        }
        var tags = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class).getValues();
        return tags.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(tags);
    }

    @Nullable
    public static <T extends Enum<T> & Keyed> EnumSet<T> parseRegistry(@NotNull String identifier, String registry, Class<T> clazz){
        if(!identifier.startsWith("#")){
            return null;
        }
        String nameKey = identifier.substring(1);
        var key = keyFromString(nameKey);
        if(key == null){
            throw new IllegalArgumentException("Entry " + identifier + " is not a valid tag!");
        }
        var tag = Bukkit.getTag(registry, key, clazz);
        if(tag == null){
            throw new IllegalArgumentException("Entry " + identifier + " is not a valid tag!");
        }
        var tagged = tag.getValues();
        return tagged.isEmpty() ? EnumSet.noneOf(clazz) : EnumSet.copyOf(tagged);
    }

    /**
     * Gets a NamespacedKey from the supplied string with a default namespace of minecraft.
     * This is intended to be used to parse NamespacedKeys from user input before the API existed in 1.16
     * @param string the string to convert to a NamespacedKey
     * @return the created NamespacedKey, or null if invalid
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static NamespacedKey keyFromString(@NotNull String string){
        try{
            if(string.contains(":")){
                int index = string.indexOf(':');
                String namespace = string.substring(0,index);
                String key = string.substring(index+1);
                // While a string based constructor is not supposed to be used,
                // their does not exist any other method for doing this in < 1.16
                return new NamespacedKey(namespace, key);
            } else {
                return NamespacedKey.minecraft(string);
            }
        }catch(IllegalArgumentException e){
            return null;
        }
    }
}
