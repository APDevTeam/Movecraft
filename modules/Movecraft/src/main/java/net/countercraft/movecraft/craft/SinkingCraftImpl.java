package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;

public class SinkingCraftImpl extends BaseCraft implements SinkingCraft {
    public SinkingCraftImpl(@NotNull Craft original) {
        super(original.getType(), original.getWorld());
        hitBox = original.getHitBox();
        collapsedHitBox.addAll(original.getCollapsedHitBox());
        fluidLocations = original.getFluidLocations();
        setCruiseDirection(original.getCruiseDirection());
        setAudience(original.getAudience());
    }
}
