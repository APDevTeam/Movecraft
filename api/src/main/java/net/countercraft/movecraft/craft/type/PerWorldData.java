package net.countercraft.movecraft.craft.type;

import java.util.HashMap;
import java.util.Map;

public class PerWorldData<T> {

    private final T defaultFallback;
    private final Map<String, T> worldMapping;

    public PerWorldData(T defaultFallback) {
        this.defaultFallback = defaultFallback;
        this.worldMapping = new HashMap<>();
    }

    public PerWorldData(T defaultFallback, Map<String, T> override) {
        this.defaultFallback = defaultFallback;
        this.worldMapping = new HashMap<>(override);
    }

    public T get() {
        return this.defaultFallback;
    }

    public T get(String worldName) {
        return this.worldMapping.getOrDefault(worldName, this.defaultFallback);
    }

}
