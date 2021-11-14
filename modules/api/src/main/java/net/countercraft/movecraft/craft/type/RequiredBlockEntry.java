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

public class RequiredBlockEntry {
    private static final String NUMERIC_PREFIX = "N"; // an N indicates a specific quantity, IE: N2 for exactly 2 of the block

    @NotNull
    private static Pair<Boolean, Number> parseLimit(@NotNull Object input) {
        if (input instanceof String) {
            String str = (String) input;
            if (str.contains(NUMERIC_PREFIX)) {
                String[] parts = str.split(NUMERIC_PREFIX);
                int val = Integer.parseInt(parts[1]);
                return new Pair<>(true, val);
            }
            else
                return new Pair<>(false, Double.valueOf(str));
        }
        else if (input instanceof Integer) {
            return new Pair<>(false, (double) input);
        }
        else
            return new Pair<>(false, (double) input);
    }

    @NotNull
    private static Pair<Pair<Boolean, Number>, Pair<Boolean, Number>> parseRange(@NotNull ArrayList<?> limits) {
        if(limits.size() != 2)
            throw new IllegalArgumentException("Range must be a pair, but found " + limits.size() + " entries");

        var min = parseLimit(limits.get(0));
        var max = parseLimit(limits.get(1));
        return new Pair<>(min, max);
    }

    @NotNull
    private static EnumSet<Material> parseMaterials(String key, Object materials) {
        EnumSet<Material> result = EnumSet.noneOf(Material.class);
        if(materials instanceof ArrayList) {
            // List, load each as a tag/material
            for(Object o : (ArrayList<?>) materials) {
                if (!(o instanceof String)) {
                    if(o == null)
                        throw new IllegalArgumentException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
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
                throw new IllegalArgumentException("Entry " + key + " has a null value. This usually indicates you've attempted to use a tag that is not surrounded by quotes");
            throw new IllegalArgumentException("Entry " + materials + " must be a material for key " + key);
        }
        return result;
    }

    @NotNull
    public static RequiredBlockEntry of(String key, Object entryKey, Object entryValue) {
        EnumSet<Material> materials = parseMaterials(key, entryKey);
        var range = parseRange((ArrayList<?>) entryValue);

        var min = range.getLeft();
        var max = range.getRight();
        return new RequiredBlockEntry(materials, (double) min.getRight(), min.getLeft(), (double) max.getRight(), max.getLeft());
    }


    private final EnumSet<Material> materials;
    private final double max;
    private final boolean numericMax;
    private final double min;
    private final boolean numericMin;

    public RequiredBlockEntry(EnumSet<Material> materials, double min, boolean numericMin, double max, boolean numericMax) {
        this.materials = materials;
        this.min = min;
        this.numericMin = numericMin;
        this.max = max;
        this.numericMax = numericMax;
    }

    public boolean contains(Material m) {
        return materials.contains(m);
    }

    public Set<Material> getMaterials() {
        return Collections.unmodifiableSet(materials);
    }

    public String materialsToString() {
        Set<String> names = new HashSet<>();
        for(Material m : materials) {
            names.add(m.name().toLowerCase().replace("_", " "));
        }
        return String.join(", ", names);
    }

    public boolean check(int count, int size) {
        return check(count, size, 1.0D);
    }

    public boolean check(int count, int size, double sinkPercent) {
        double blockPercent = 100D * count / size;
        if(numericMin) {
            if(count < min)
                return false;
        }
        else {
            if(blockPercent * sinkPercent < min)
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

    public Pair<DetectionResult, String> detect(int count, int size) {
        double blockPercent = 100D * count / size;
        if(numericMin) {
            if(count < min)
                return new Pair<>(DetectionResult.NOT_ENOUGH, String.format("%d < %d", count, (int) min));
        }
        else {
            if(blockPercent < min)
                return new Pair<>(DetectionResult.NOT_ENOUGH, String.format("%.2f%% < %.2f%%", blockPercent, min));
        }
        if(numericMax) {
            if(count > max)
                return new Pair<>(DetectionResult.TOO_MUCH, String.format("%d > %d", count, (int) max));
        }
        else {
            if(blockPercent > max)
                return new Pair<>(DetectionResult.TOO_MUCH, String.format("%.2f%% > %.2f%%", blockPercent, max));
        }
        return new Pair<>(DetectionResult.SUCCESS, "");
    }
}
