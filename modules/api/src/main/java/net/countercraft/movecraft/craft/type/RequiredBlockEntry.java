package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class RequiredBlockEntry {
    private static final String NUMERIC_PREFIX = "N"; // an N indicates a specific quantity, IE: N2 for exactly 2 of the block

    @NotNull
    private static Pair<Boolean, Number> parse(@NotNull Object input) {
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
    public static RequiredBlockEntry of(@NotNull ArrayList<?> limits) {
        if(limits.size() != 2)
            throw new IllegalArgumentException("Range must be a pair, but found " + limits.size() + " entries");

        var min = parse(limits.get(0));
        var max = parse(limits.get(1));
        return new RequiredBlockEntry((double) min.getRight(), min.getLeft(), (double) max.getRight(), max.getLeft());
    }


    private final double max;
    private final boolean numericMax;
    private final double min;
    private final boolean numericMin;

    public RequiredBlockEntry(double min, boolean numericMin, double max, boolean numericMax) {
        this.min = min;
        this.numericMin = numericMin;
        this.max = max;
        this.numericMax = numericMax;
    }

    public boolean verify(int count, int size) {
        double percent = 100D * count / size;
        return ((numericMax ? count : percent) <= max) && ((numericMin ? count : percent) >= min);
    }
}
