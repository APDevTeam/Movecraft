package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This class represents a single flyblock or moveblock entry.
 * <p>
 * It contains a set of materials which it applies to.
 * <p>
 * It also contains variables to determine the min and max.
 * <p>
 * A numeric limit represents a configured static limit (regardless of size), ex: "N10" for 10 blocks.<br>
 * A percentage limit represents a limit which scales with craft size, ex: 10 for 10%.
 */
@SerializableAs("RequiredBlockEntry")
public class RequiredBlockEntry implements ConfigurationSerializable {
    private final EnumSet<Material> materials;
    private String name;
    private final double max;
    private final boolean numericMax;
    private final double min;
    private final boolean numericMin;
    /* Displayname for use in "too much flyblock" messages instead of the long list*/
    private final String displayName;

    public RequiredBlockEntry(EnumSet<Material> materials, @NotNull Pair<Boolean, ? extends Number> min, @NotNull Pair<Boolean, ? extends Number> max, @NotNull String name) {
        this(materials, min, max, name, "");
    }

    public RequiredBlockEntry(EnumSet<Material> materials, @NotNull Pair<Boolean, ? extends Number> min, @NotNull Pair<Boolean, ? extends Number> max, @NotNull String name, final String displayName) {
        this.materials = materials;
        this.min = min.getRight().doubleValue();
        this.numericMin = min.getLeft();
        this.max = max.getRight().doubleValue();
        this.numericMax = max.getLeft();
        this.name = name;
        this.displayName = displayName;
    }

    public RequiredBlockEntry(RequiredBlockEntry requiredBlockEntry) {
        this.materials = EnumSet.copyOf(requiredBlockEntry.materials);
        this.min = requiredBlockEntry.min;
        this.numericMin = requiredBlockEntry.numericMin;
        this.max = requiredBlockEntry.max;
        this.numericMax = requiredBlockEntry.numericMax;
        this.name = String.valueOf(requiredBlockEntry.name);
        this.displayName = String.valueOf(requiredBlockEntry.displayName);
    }

    /**
     * Check if this <code>RequiredBlockEntry</code> contains a material.
     *
     * @param m Material to check
     * @return <code>true</code> if this contains the material, <code>false</code> if it does not
     */
    public boolean contains(Material m) {
        return materials.contains(m);
    }

    /**
     * Get a set of the materials this <code>RequiredBlockEntry</code> contains.
     *
     * @return A copy of the materials this contains
     */
    public Set<Material> getMaterials() {
        return Collections.unmodifiableSet(materials);
    }

    /**
     * Convert the materials contained in this to a string.
     *
     * @return A string representation of the materials contained in this
     */
    public String materialsToString() {
        Set<String> names = new HashSet<>();
        for(Material m : materials) {
            names.add(m.name().toLowerCase().replace("_", " "));
        }
        return String.join(", ", names);
    }

    /**
     * Check a block count and size against this
     *
     * @param count Count of this entry on the craft
     * @param size Size of the craft
     * @return <code>true</code> if the count and size pass the min and max bounds, <code>false</code> if it does not
     */
    public boolean check(int count, int size) {
        return check(count, size, 1.0D);
    }

    /**
     * Check a block count and size against this with the specified sinkPercent
     *
     * @param count Count of this entry on the craft
     * @param size Size of the craft
     * @param sinkPercent The allowed percentage below the max
     * @return <code>true</code> if the count and size pass the min and max bounds, <code>false</code> if it does not
     */
    public boolean check(int count, int size, double sinkPercent) {
        double blockPercent = 100D * (double) count / size;
        if(numericMin) {
            if(count < min)
                return false;
        }
        else {
            if(blockPercent < min * sinkPercent)
                return false;
        }
        if(numericMax) {
            return !(count > max);
        }
        else {
            return !(blockPercent > max);
        }
    }

    static Pair<Boolean, ? extends Number> parseLimit(@NotNull Object input) {
        if (input instanceof String) {
            String str = (String) input;
            if (str.contains(TypeData.NUMERIC_PREFIX)) {
                String[] parts = str.split(TypeData.NUMERIC_PREFIX);
                int val = Integer.parseInt(parts[1]);
                return new Pair<>(true, val);
            }
            else
                return new Pair<>(false, Double.valueOf(str));
        }
        else if (input instanceof Integer) {
            return new Pair<>(false, (Integer) input);
        }
        else
            return new Pair<>(false, (double) input);
    }

    static EnumSet<Material> parseMaterials(String key, Object materials) {
        EnumSet<Material> result = EnumSet.noneOf(Material.class);
        if(materials instanceof ArrayList) {
            // List, load each as a tag/material
            for(Object o : (ArrayList<?>) materials) {
                if (!(o instanceof String)) {
                    if(o == null)
                        throw new IllegalArgumentException("Entry in " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
                    throw new IllegalArgumentException("Entry " + o + " must be a material for key " + key);
                }
                String string = (String) o;
                result.addAll(Tags.parseMaterials(string));
            }
        }
        else if(materials instanceof String) {
            // Single entry, load as a tag/material
            String string = (String) materials;
            result.addAll(Tags.parseMaterials(string));
        }
        else {
            // Invalid entry, throw an error
            if(materials == null)
                throw new IllegalArgumentException("Entry in " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
            throw new IllegalArgumentException("Entry in " + materials + " must be a material for key " + key);
        }
        return result;
    }

    public static @NotNull RequiredBlockEntry deserialize(@NotNull Map<String, Object> args) {
        // TODO: Implement proper parsing logic!
        String displayName = (String) args.getOrDefault("displayName", "");
        Pair<Boolean, ? extends Number> min = parseLimit(args.getOrDefault("min", 0));
        Pair<Boolean, ? extends Number> max = parseLimit(args.getOrDefault("max", 1));
        EnumSet<Material> materials = parseMaterials(displayName, args.getOrDefault("materials", null));
        return null;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
                "displayName", this.displayName,
                "min", this.numericMin ? TypeData.NUMERIC_PREFIX : "" + this.min,
                "max", this.numericMax ? TypeData.NUMERIC_PREFIX : "" + this.max,
                "materials", this.materials
        );
    }

    public enum DetectionResult {
        NOT_ENOUGH,
        TOO_MUCH,
        SUCCESS
    }

    /**
     * Check a block count and size against this and return the result with a string
     *
     * @param count Count of this entry on the craft
     * @param size Size of the craft
     * @return A pair with the result (not enough, too much, or success) and the reason for failure (or blank for success)
     */
    public Pair<DetectionResult, String> detect(int count, int size) {
        double blockPercent = 100D * (double) count / size;

        if(numericMin && count < min)
            return new Pair<>(DetectionResult.NOT_ENOUGH, String.format("%d < %d", count, (int) min));
        else if(!numericMin && blockPercent < min)
            return new Pair<>(DetectionResult.NOT_ENOUGH, String.format("%.2f%% < %.2f%%", blockPercent, min));

        if(numericMax && count > max)
            return new Pair<>(DetectionResult.TOO_MUCH, String.format("%d > %d", count, (int) max));
        else if(!numericMax && blockPercent > max)
            return new Pair<>(DetectionResult.TOO_MUCH, String.format("%.2f%% > %.2f%%", blockPercent, max));

        return new Pair<>(DetectionResult.SUCCESS, "");
    }

    /**
     * Get the max for this <code>RequiredBlockEntry</code>
     * @return
     * If this is a numeric max, this returns the integer value (N1 = 1.0D).
     * If this is not a numeric max, this returns the percentage (1.0D = 100%)
     */
    public double getMax() {
        return max;
    }

    /**
     * Check if this has a numeric or percentage max
     * @return
     * If this is a numeric max, this returns <code>true</code>
     * If this is not a numeric max, this returns <code>false</code>
     */
    public boolean isNumericMax() {
        return numericMax;
    }

    /**
     * Get the min for this <code>RequiredBlockEntry</code>
     * @return
     * If this is a numeric min, this returns the integer value (N1 = 1.0D).
     * If this is not a numeric min, this returns the percentage (0.5D = 50%)
     */
    public double getMin() {
        return min;
    }

    /**
     * Check if this has a numeric or percentage min
     * @return
     * If this is a numeric min, this returns <code>true</code>
     * If this is not a numeric min, this returns <code>false</code>
     */
    public boolean isNumericMin() {
        return numericMin;
    }

    public String getName () {
        return name;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
