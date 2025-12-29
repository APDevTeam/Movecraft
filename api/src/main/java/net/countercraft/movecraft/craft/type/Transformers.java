package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.craft.type.transform.TypeSafeTransform;
import net.countercraft.movecraft.util.Tags;

import java.util.HashMap;
import java.util.Map;

public class Transformers {

    public static TypeSafeTransform register(TypeSafeTransform transform) {
        if (!TypeSafeCraftType.TRANSFORM_REGISTRY.add(transform)) {
            throw new IllegalStateException("No duplicate transformers allowed!");
        }
        return transform;
    }

    static void registerAll() {
        // Convert speed to TICK_COOLDOWN
        register((getter, setter, deleter) -> {
            PerWorldData<Double> speedData = getter.getWithoutParent(PropertyKeys.SPEED);
            if (speedData == null) {
                return false;
            }
            Map<String, Integer> mapping = new HashMap<>(speedData.getOverrides().size());
            for (Map.Entry<String, Double> entry : speedData.getOverrides().entrySet()) {
                mapping.put(entry.getKey(), (int) Math.ceil(20 / entry.getValue()));
            }
            int defaultValue = (int) Math.ceil(20 / speedData.getDefaultFallback());
            PerWorldData<Integer> tickCooldown = new PerWorldData<>(defaultValue, mapping);
            setter.accept(PropertyKeys.TICK_COOLDOWN, tickCooldown);
            deleter.add(PropertyKeys.SPEED);
            return true;
        });
        // Convert cruiseSpeed to CRUISE_TICK_COOLDOWN
        register((getter, setter, deleter) -> {
            PerWorldData<Double> speedData = getter.get(PropertyKeys.CRUISE_SPEED);
            PerWorldData<Integer> skipData = getter.get(PropertyKeys.CRUISE_SKIP_BLOCKS);
            if (speedData == null || skipData == null) {
                return false;
            }
            Map<String, Integer> mapping = new HashMap<>(speedData.getOverrides().size());
            for (Map.Entry<String, Double> entry : speedData.getOverrides().entrySet()) {
                int value = (int) Math.round((1.0D + skipData.get(entry.getKey())) * 20.0D / entry.getValue());
                mapping.put(entry.getKey(), value);
            }
            for (Map.Entry<String, Integer> entry : skipData.getOverrides().entrySet()) {
                int value = (int) Math.round((1.0D + speedData.get(entry.getKey())) * 20.0D / entry.getValue());
                mapping.put(entry.getKey(), value);
            }
            PerWorldData<Integer> tickCooldown = new PerWorldData<>((int) Math.ceil(20 / speedData.getDefaultFallback()), mapping);
            setter.accept(PropertyKeys.CRUISE_TICK_COOLDOWN, tickCooldown);
            deleter.add(PropertyKeys.CRUISE_SPEED);
            return true;
        });
        // Convert vertCruiseSpeed to VERT_CRUISE_TICK_COOLDOWN
        register((getter, setter, deleter) -> {
            PerWorldData<Double> speedData = getter.get(PropertyKeys.VERT_CRUISE_SPEED);
            PerWorldData<Integer> skipData = getter.get(PropertyKeys.VERT_CRUISE_SKIP_BLOCKS);
            if (speedData == null || skipData == null) {
                return false;
            }
            Map<String, Integer> mapping = new HashMap<>(speedData.getOverrides().size());
            for (Map.Entry<String, Double> entry : speedData.getOverrides().entrySet()) {
                int value = (int) Math.round((1.0D + skipData.get(entry.getKey())) * 20.0D / entry.getValue());
                mapping.put(entry.getKey(), value);
            }
            for (Map.Entry<String, Integer> entry : skipData.getOverrides().entrySet()) {
                int value = (int) Math.round((1.0D + speedData.get(entry.getKey())) * 20.0D / entry.getValue());
                mapping.put(entry.getKey(), value);
            }
            PerWorldData<Integer> tickCooldown = new PerWorldData<>((int) Math.ceil(20 / speedData.getDefaultFallback()), mapping);
            setter.accept(PropertyKeys.VERT_CRUISE_TICK_COOLDOWN, tickCooldown);
            deleter.add(PropertyKeys.VERT_CRUISE_SPEED);
            return true;
        });
        // Convert canFly to blockedByWater and remove canFly
        register((getter, setter, deleter) -> {
            if (getter.has(PropertyKeys.CAN_FLY)) {
                setter.accept(PropertyKeys.BLOCKED_BY_WATER, getter.get(PropertyKeys.CAN_FLY));
                deleter.add(PropertyKeys.CAN_FLY);
                return true;
            }
            return false;
        });
        // Fix gravityDropDistance to be negative
        register((getter, setter, deleter) -> {
            Integer dropDist = getter.getWithoutParent(PropertyKeys.GRAVITY_DROP_DISTANCE);
            if (dropDist == null) {
                return false;
            }
            if (dropDist > 0) {
                setter.accept(PropertyKeys.GRAVITY_DROP_DISTANCE, -dropDist);
                return true;
            }
            return false;
        });
        // Add WATER to PASSTHROUGH_BLOCKS if not BLOCKED_BY_WATER
        register((getter, setter, deleter) -> {
            if (getter.get(PropertyKeys.BLOCKED_BY_WATER)) {
                return false;
            }
            getter.get(PropertyKeys.PASSTHROUGH_BLOCKS).addEnumSet(Tags.WATER);
            return true;
        });
        // Add WATER to FORBIDDEN_HOVER_OVER_BLOCKS if not canHoverOverWater
        register((getter, setter, deleter) -> {
            if (getter.get(PropertyKeys.CAN_HOVER_OVER_WATER)) {
                return false;
            }
            getter.get(PropertyKeys.FORBIDDEN_HOVER_OVER_BLOCKS).addEnumSet(Tags.WATER);
            return true;
        });
        // Remove speed, sinkSpeed, cruiseSpeed, vertCruiseSpeed, perWorldSpeed, perWorldCruiseSpeed,
        //   and perWorldVertCruiseSpeed
        register((getter, setter, deleter) -> {
            deleter.add(PropertyKeys.SPEED);
            deleter.add(PropertyKeys.SINK_SPEED);
            deleter.add(PropertyKeys.CRUISE_SPEED);
            deleter.add(PropertyKeys.VERT_CRUISE_SPEED);
            return true;
        });
    }

}
