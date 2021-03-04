package net.countercraft.movecraft.utils;


public enum CraftStatus {
    NORMAL,
    SINKING,
    DISABLED,
    SINKING_DISABLED;

    public boolean isSinking() {
        return (this == SINKING || this == SINKING_DISABLED);
    }

    public boolean isDisabled() {
        return (this == DISABLED || this == SINKING_DISABLED);
    }

    public static CraftStatus fromBooleans (boolean sinking, boolean disabled) {
        if (!sinking && !disabled) {
            return NORMAL;
        }
        if (!disabled) {
            return SINKING;
        }
        if (!sinking) {
            return DISABLED;
        }
        return SINKING_DISABLED;
    }
}
