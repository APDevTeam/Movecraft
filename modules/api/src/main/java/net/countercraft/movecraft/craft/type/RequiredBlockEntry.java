package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

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
public class RequiredBlockEntry {
    private final EnumSet<Material> materials;
    private final double max;
    private final boolean numericMax;
    private final double min;
    private final boolean numericMin;


    public RequiredBlockEntry(EnumSet<Material> materials, @NotNull Pair<Boolean, ? extends Number> min, @NotNull Pair<Boolean, ? extends Number> max) {
        this.materials = materials;
        this.min = min.getRight().doubleValue();
        this.numericMin = min.getLeft();
        this.max = max.getRight().doubleValue();
        this.numericMax = max.getLeft();
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
}
