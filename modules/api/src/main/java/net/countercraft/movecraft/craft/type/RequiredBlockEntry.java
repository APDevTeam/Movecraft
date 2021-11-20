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
}
