package net.countercraft.movecraft.craft;

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

    public static CraftStatus of (boolean sinking, boolean disabled) {
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