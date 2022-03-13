package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;

public class SinkingCraftImpl extends BaseCraft implements SinkingCraft {
    public SinkingCraftImpl(@NotNull BaseCraft original) {
        super(original.type, original.w);
        hitBox = original.hitBox;
        collapsedHitBox.addAll(original.collapsedHitBox);
        fluidLocations = original.fluidLocations;
        setAudience(original.getAudience());
    }
}
