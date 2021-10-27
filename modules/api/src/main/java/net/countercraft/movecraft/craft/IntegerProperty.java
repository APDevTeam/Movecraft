package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class IntegerProperty {
    private final String key;
    private final Predicate<Integer> validator;
    private final boolean hasDefault;
    private final int defaultValue;

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     */
    IntegerProperty(@NotNull String key) {
        this.key = key;
        this.validator = null;
        this.hasDefault = false;
        this.defaultValue = 0;
    }

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     * @param defaultValue the default value for this property
     */
    IntegerProperty(@NotNull String key, int defaultValue) {
        this.key = key;
        this.validator = null;
        this.defaultValue = defaultValue;
        this.hasDefault = true;
    }

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     * @param validator a validator for the value of this property
     */
    IntegerProperty(@NotNull String key, @Nullable Predicate<Integer> validator) {
        this.key = key;
        this.validator = validator;
        this.hasDefault = false;
        this.defaultValue = 0;
    }

    /**
     * Construct an IntegerProperty
     *
     * @param key the key for this property
     * @param validator a validator for the value of this property
     * @param defaultValue the default value for this property
     */
    IntegerProperty(@NotNull String key, @Nullable Predicate<Integer> validator, int defaultValue) {
        this.key = key;
        this.validator = validator;
        this.defaultValue = defaultValue;
        this.hasDefault = true;
    }

    /**
     * Load and validate the property from data
     *
     * @param data TypeData to read the property from
     * @return the value
     */
    @Nullable Integer load(@NotNull TypeData data) {
        int value;
        if(hasDefault)
            value = data.getIntOrDefault(key, defaultValue);
        else
            value = data.getInt(key);

        if(validator != null && !validator.test(value))
            return null;

        return value;
    }

    /**
     * Get the string key for this property
     *
     * @return the key
     */
    @NotNull String getKey() {
        return key;
    }
}
