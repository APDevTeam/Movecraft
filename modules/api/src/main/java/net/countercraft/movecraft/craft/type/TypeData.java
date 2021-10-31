package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
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

    private TypeData(Map<String, Object> data) {
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
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return EMPTY;
        }
        try(input) {
            Yaml yaml = new Yaml();
            return new TypeData(yaml.load(input));
        }
        catch (IOException e) {
            e.printStackTrace();
            return EMPTY;
        }
    }

    private boolean containsKey(@NotNull String key) {
        return this.backingData.containsKey(key);
    }

    private void requireOneOf(@NotNull String... keys) {
        for(String key : keys) {
            if(this.containsKey(key))
                return;
        }
        throw new KeyNotFoundException("No keys found for " + Arrays.toString(keys));
    }

    private void requireKey(@NotNull String key) {
        if(!this.containsKey(key))
            throw new KeyNotFoundException("No key found for " + key);
    }

    /**
     * Gets the requested boolean by its key.
     * If they key is not found, an error is thrown.
     * If the value is found, but is not a boolean, an error is thrown.
     *
     * @param key Key of boolean to get
     * @return The requested boolean
     */
    public boolean getBoolean(@NotNull String key) {
        requireKey(key);
        if (backingData.get(key) instanceof Boolean)
            return (Boolean) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type boolean");
    }

    /**
     * Gets the requested boolean by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a boolean, an error ir thrown
     *
     * @param key Key of boolean to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested boolean
     */
    public boolean getBooleanOrDefault(@NotNull String key, boolean defaultValue) {
        if (!containsKey(key) || !(backingData.get(key) instanceof Boolean))
            return defaultValue;
        if(backingData.get(key) instanceof Boolean)
            return (Boolean) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type boolean");
    }

    /**
     * Gets the requested int by its key.
     * If they key is not found, an error is thrown.
     * If the key is found, but the value is not a int, an error is thrown.
     *
     * @param key Key of int to get
     * @return The requested int
     */
    public int getInt(@NotNull String key) {
        requireKey(key);
        if(backingData.get(key) instanceof Integer)
            return (Integer) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type int");
    }

    /**
     * Gets the requested int by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a int, an error is thrown.
     *
     * @param key Key of int to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested int
     */
    public int getIntOrDefault(@NotNull String key, int defaultValue) {
        if (!backingData.containsKey(key))
            return defaultValue;
        if(backingData.get(key) instanceof Integer)
            return (Integer) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type int");
    }

    /**
     * Gets the requested double by its key.
     * If they key is not found, an error is thrown.
     * If the key is found, but the value is not a double, an error is thrown.
     *
     * @param key Key of double to get
     * @return The requested double
     */
    public double getDouble(@NotNull String key) {
        requireKey(key);
        var data = backingData.get(key);
        if(data instanceof Integer)
            return (Integer) backingData.get(key);
        if(data instanceof Double)
            return (Double) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type double");
    }

    /**
     * Gets the requested double by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a double, an error is thrown.
     *
     * @param key Key of double to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested double
     */
    public double getDoubleOrDefault(@NotNull String key, double defaultValue) {
        if (!backingData.containsKey(key))
            return defaultValue;
        var data = backingData.get(key);
        if(data instanceof Integer)
            return (Integer) backingData.get(key);
        if(data instanceof Double)
            return (Double) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type double");
    }

    /**
     * Gets the requested String by its key.
     * If they key is not found, an error is thrown.
     * If the key is found, but the value is not a String, an error is thrown.
     *
     * @param key Key of String to get
     * @return The requested String
     */
    @NotNull
    public String getString(@NotNull String key) {
        requireKey(key);
        if(backingData.get(key) instanceof String)
            return (String) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type String");
    }

    /**
     * Gets the requested String by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a String, an error is thrown.
     *
     * This method will only return <code>null</code> if the defaultValue is <code>null</code>
     *
     * @param key Key of String to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested String
     */
    @Contract("_, !null -> !null")
    public String getStringOrDefault(@NotNull String key, @Nullable String defaultValue) {
        if (!backingData.containsKey(key))
            return defaultValue;
        if(backingData.get(key) instanceof Integer)
            return (String) backingData.get(key);
        throw new InvalidValueException("Value for key " + key + " must be of type String");
    }

    /**
     * Gets the requested Material by its key.
     * If they key is not found, an error is thrown.
     * If the key is found, but the value is not a Material, an error is thrown.
     *
     * @param key Key of Material to get
     * @return The requested Material
     */
    @NotNull
    public Material getMaterial(@NotNull String key) {
        requireKey(key);
        if(backingData.get(key) instanceof String) {
            try {
                return Material.valueOf(((String) backingData.get(key)).toUpperCase());
            }
            catch (IllegalArgumentException e){
                throw new InvalidValueException("Value for key " + key + " must be of type Material");
            }
        }
        throw new InvalidValueException("Value for key " + key + " must be of type Material");
    }

    /**
     * Gets the requested Material by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a Material, an error is thrown.
     *
     * This method will only return <code>null</code> if the defaultValue is <code>null</code>
     *
     * @param key Key of Material to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested Material
     */
    @Contract("_, !null -> !null")
    public Material getMaterialOrDefault(@NotNull String key, @Nullable Material defaultValue) {
        if (!this.containsKey(key))
            return defaultValue;
        if(backingData.get(key) instanceof String)
            return Material.valueOf(((String) backingData.get(key)).toUpperCase());
        throw new InvalidValueException("Value for key " + key + " must be of type Material");
    }

    /**
     * Gets the requested Sound by its key.
     * If they key is not found, an error is thrown.
     * If the value is found, but is not a Sound, an error is thrown.
     *
     * @param key Key of Sound to get
     * @return The requested Sound
     */
    public Sound getSound(@NotNull String key) {
        requireKey(key);
        if(backingData.get(key) instanceof String)
            return Sound.sound(Key.key((String) backingData.get(key)), Sound.Source.NEUTRAL, 2f, 1f);
        throw new InvalidValueException("Value for key " + key + " must be of type Sound");
    }

    /**
     * Gets the requested Sound by its key.
     * If they key is not found, the default is returned.
     * If the key is found, but the value is not a Sound, an error is thrown.
     *
     * This method will only return <code>null</code> if the defaultValue is <code>null</code>
     *
     * @param key Key of Sound to get
     * @param defaultValue The default return value to use if the provided key is not valid
     * @return The requested Sound
     */
    @Contract("_, !null -> !null")
    public Sound getSoundOrDefault(@NotNull String key, @Nullable Sound defaultValue) {
        if (!this.containsKey(key))
            return defaultValue;
        if(backingData.get(key) instanceof String)
            return Sound.sound(Key.key((String) backingData.get(key)), Sound.Source.NEUTRAL, 2f, 1f);
        throw new InvalidValueException("Value for key " + key + " must be of type Sound");
    }

    /**
     * Gets the requested materials by their key.
     * If they key is not found, an error is thrown.
     * If the value is found, but is not a List of Materials, an error is thrown.
     *
     * @param key Key of the Materials to get
     * @return The requested Materials
     */
    @NotNull
    public EnumSet<Material> getMaterials(@NotNull String key) {
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        requireKey(key);
        if(!(this.backingData.get(key) instanceof ArrayList))
            throw new InvalidValueException("key " + key + " must be a list of materials.");
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            if (!(object instanceof String)) {
                if(object == null)
                    throw new InvalidValueException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
                throw new InvalidValueException("Entry " + object + " must be a material for key " + key);
            }
            String materialName = (String) object;
            EnumSet<Material> materials = Tags.parseMaterials(materialName);
            if(materials.isEmpty())
                throw new InvalidValueException("Entry " + object + " describes an empty or non-existent Tag for key " + key);
            returnList.addAll(materials);
        }
        return returnList;
    }

    /**
     * Gets the requested materials by their key.
     * If they key is not found, or if the value is not a List of Materials, an empty set is returned.
     *
     * @param key Key of the Materials to get
     * @return The requested Materials, or an empty set
     */
    @NotNull
    public EnumSet<Material> getMaterialsOrEmpty(@NotNull String key) {
        EnumSet<Material> returnList = EnumSet.noneOf(Material.class);
        if(!(this.backingData.get(key) instanceof ArrayList))
            return returnList;
        for(Object object : (ArrayList<?>) this.backingData.get(key)){
            String materialName = (String) object;
            returnList.addAll(Tags.parseMaterials(materialName));
        }
        return returnList;
    }

    /**
     * Gets a child TypeData from its parent.
     * If the key is not found, or if the value is not valid, an error is thrown.
     *
     * @param key Key of the data to get
     * @return The requested child data entry
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public TypeData getData(@NotNull String key) {
        requireKey(key);
        if(!(backingData.get(key) instanceof Map))
            throw new InvalidValueException("Value for " + key + " must be a map");
        return new TypeData((Map<String, Object>) backingData.get(key));
    }

    /**
     * Gets a child TypeData from its parent.
     * If the key is not found, or if the value is not valid, an empty data entry is returned.
     *
     * @param key Key of the data to get
     * @return The requested child data entry
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public TypeData getDataOrEmpty(@NotNull String key) {
        if(containsKey(key) && backingData.get(key) instanceof Map)
            return new TypeData((Map<String, Object>) backingData.get(key));
        return EMPTY;
    }

    /**
     * Gets the requested List by its key.
     * If they key is not found, an error is thrown.
     * If the value is found, but is not a List, an error is thrown.
     *
     * @param key Key of the List to get
     * @return The requested List
     */
    @NotNull
    public List<?> getList(@NotNull String key) {
        requireKey(key);
        if(!(backingData.get(key) instanceof List))
            throw new InvalidValueException("Value for key " + key + " must be a list");
        return (List<?>) backingData.get(key);
    }

    /**
     * Gets the requested List by its key.
     * If they key is not found, or if the value is found, but is not a List, an empty list is returned.
     *
     * @param key Key of the List to get
     * @return The requested List
     */
    @NotNull
    public List<?> getListOrEmpty(@NotNull String key) {
        if(!containsKey(key) || !(backingData.get(key) instanceof List))
            return Collections.emptyList();
        return (List<?>) backingData.get(key);
    }

    /**
     * Gets the requested List of Strings by its key.
     * If they key is not found, an error is thrown.
     * If the value is found, but is not a List of Strings, an error is thrown.
     *
     * @param key Key of the List of Strings to get
     * @return The requested List of Strings
     */
    @NotNull
    public List<String> getStringList(@NotNull String key) {
        var list = getList(key);
        var out = new ArrayList<String>();
        for(Object object : list){
            if(!(object instanceof String))
                throw new InvalidValueException("Values in list under key " + key + " must be strings");
            out.add((String) object);
        }
        return out;
    }

    /**
     * Gets the requested List of Strings by its key.
     * If they key is not found, an empty List is returned.
     * If the value is found, but is not a List, an empty List is returned.
     * Any non string entries are dropped.
     *
     * @param key Key of the List of Strings to get
     * @return The requested List of Strings
     */
    @NotNull
    public List<String> getStringListOrEmpty(@NotNull String key) {
        var list = getListOrEmpty(key);
        return list.stream().filter(object -> object instanceof String).map(object -> (String) object).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Gets all key value pairs stored in this TypeData
     *
     * @return the data represented by this TypeData
     */
    @NotNull
    public Map<String, Object> getBackingData() {
        return backingData;
    }


    public static class KeyNotFoundException extends IllegalArgumentException {

        public KeyNotFoundException(String s) {
            super(s);
        }
    }

    public static class InvalidValueException extends IllegalArgumentException {

        public InvalidValueException(String s) {
            super(s);
        }
    }
}
