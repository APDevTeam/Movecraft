package net.countercraft.movecraft.processing.tasks.detection;

import org.jetbrains.annotations.NotNull;

public enum Modifier {
    NONE(0),
    PERMIT(1),
    FAIL(3);

    private static final Modifier[] modifiers = new Modifier[]{NONE, PERMIT, null, FAIL};

    private final int mask;
    Modifier(int mask){
        this.mask = mask;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public Modifier merge(@NotNull Modifier other){
        return modifiers[this.mask | other.mask];
    }
}
