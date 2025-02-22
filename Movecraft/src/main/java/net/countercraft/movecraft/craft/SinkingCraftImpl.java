package net.countercraft.movecraft.craft;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

public class SinkingCraftImpl extends BaseCraft implements SinkingCraft {
    public SinkingCraftImpl(@NotNull Craft original) {
        super(original.getType(), original.getWorld());
        hitBox = original.getHitBox();
        collapsedHitBox.addAll(original.getCollapsedHitBox());
        fluidLocations = original.getFluidLocations();
        setOrigBlockCount(original.getOrigBlockCount());
        setCruiseDirection(original.getCruiseDirection());
        setLastTranslation(original.getLastTranslation());
        // If the craft is sinking we dont need an audience anymore
        setAudience(Audience.empty());
    }

    @Override
    public @NotNull Audience getAudience() {
        return Audience.empty();
    }
}
