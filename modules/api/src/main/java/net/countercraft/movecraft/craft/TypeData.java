package net.countercraft.movecraft.craft;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A holder for any type data configured by users with type safe wrappers.
 */
public final class TypeData {
    private static final TypeData EMPTY = new TypeData(Collections.emptyMap());

    private final @NotNull Map<String, Object> backingData;

    private TypeData(Map<String, Object> data){
        this.backingData = Collections.unmodifiableMap(data);
    }

    /**
     * Creates a new TypeData, loading information from the given file.
     *
     * Any IO errors will be caught and ignored, yielding an empty data object.
     *
     * @param file Input File
     * @return Resulting data
     */
    @NotNull
    public static TypeData loadConfiguration(@NotNull File file) {
        final InputStream input;
        try {
            input = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return EMPTY;
        }
        try(input){

            Yaml yaml = new Yaml();
            return new TypeData(yaml.load(input));
        } catch (IOException e) {
            e.printStackTrace();
            return EMPTY;
        }
    }

    private boolean containsKey(@NotNull String key){
        return this.backingData.containsKey(key);
    }

    private void requireOneOf(@NotNull String... keys){
        for(String key : keys){
            if(this.containsKey(key)){
                return;
            }
        }
        throw new IllegalArgumentException("No keys found for " + Arrays.toString(keys));
    }

    private void requireKey(@NotNull String key){
        if(!this.containsKey(key)){
            throw new IllegalArgumentException("No key found for " + key);
        }
    }

    public boolean getBoolean(@NotNull String key){
        requireKey(key);
        return (Boolean) backingData.get(key);
    }

    public boolean getBooleanOrDefault(@NotNull String key, boolean defaultValue){
        return (Boolean) backingData.getOrDefault(key, defaultValue);
    }

    public int getInt(@NotNull String key){
        requireKey(key);
        return (Integer) backingData.get(key);
    }

    public int getIntOrDefault(@NotNull String key, int defaultValue){
        return (Integer) backingData.getOrDefault(key, defaultValue);
    }

    public double getDouble(@NotNull String key){
        requireKey(key);
        return (Double) backingData.get(key);
    }

    public double getDoubleOrDefault(@NotNull String key, double defaultValue){
        return (Double) backingData.getOrDefault(key, defaultValue);
    }

    @NotNull
    public String getString(@NotNull String key){
        requireKey(key);
        return (String) backingData.get(key);
    }

    @Contract("_, !null -> !null")
    public String getStringOrDefault(@NotNull String key, @Nullable String defaultValue){
        return (String) backingData.getOrDefault(key, defaultValue);
    }

    @NotNull
    public Material getMaterial(@NotNull String key){
        requireKey(key);
        return Material.valueOf((String) backingData.get(key));
    }

    @Contract("_, !null -> !null")
    public Material getMaterialOrDefault(@NotNull String key, @Nullable Material defaultValue){
        return this.containsKey(key) ? Material.valueOf((String) backingData.get(key)) : defaultValue;
    }

    public Sound getSound(@NotNull String key){
        requireKey(key);
        return Sound.valueOf((String) backingData.get(key));
    }

    @Contract("_, !null -> !null")
    public Sound getSoundOrDefault(@NotNull String key, @Nullable Sound defaultValue){
        return this.containsKey(key) ? Sound.valueOf((String) backingData.get(key)) : defaultValue;
    }

    @NotNull
    public EnumSet<Material> getMaterials(@NotNull String key){
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        requireKey(key);
        if(!(this.backingData.get(key) instanceof ArrayList)){
            throw new IllegalArgumentException("key " + key + " must be a list of materials.");
        }
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            if (!(object instanceof String)) {
                throw new IllegalArgumentException("Entry " + object + " must be a material for key " + key);
            }
            String materialName = (String) object;
            returnList.add(Material.valueOf(materialName));
        }
        return returnList;
    }

    @NotNull
    public EnumSet<Material> getMaterialsOrEmpty(@NotNull String key){
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        if(!(this.backingData.get(key) instanceof ArrayList)){
            return returnList;
        }
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            String materialName = (String) object;
            returnList.add(Material.valueOf(materialName));
        }
        return returnList;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public TypeData getData(@NotNull String key){
        requireKey(key);
        if(!(backingData.get(key) instanceof Map)){
            throw new IllegalArgumentException("Value for " + key + " must be a map");
        }
        return new TypeData((Map<String, Object>) backingData.get(key));
    }

    @NotNull
    public List<?> getList(@NotNull String key){
        requireKey(key);
        if(!(backingData.get(key) instanceof List)){
            throw new IllegalArgumentException("Value for key " + key + " must be a list");
        }
        return (List<?>) backingData.get(key);
    }

    @NotNull
    public List<?> getListOrEmpty(@NotNull String key){
        if(!containsKey(key) || !(backingData.get(key) instanceof List)){
            return Collections.emptyList();
        }
        return (List<?>) backingData.get(key);
    }

    @NotNull
    public List<String> getStringList(@NotNull String key){
        var list = getList(key);
        var out = new ArrayList<String>();
        for(Object object : list){
            if(!(object instanceof String)){
                throw new IllegalArgumentException("Values in list under key " + key + " must be strings");
            }
            out.add((String) object);
        }
        return out;
    }

    @NotNull
    public List<String> getStringListOrEmpty(@NotNull String key){
        var list = getListOrEmpty(key);
        return list.stream().filter(object -> object instanceof String).map(object -> (String) object).collect(Collectors.toCollection(ArrayList::new));
    }

    @NotNull
    public Map<String, Object> getBackingData(){
        return backingData;
    }
}
