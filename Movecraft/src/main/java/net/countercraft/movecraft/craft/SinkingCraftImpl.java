package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.datatag.CraftDataTagContainer;
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
        setAudience(original.getAudience());
    }

    @Override
    protected CraftDataTagContainer createContainer() {
        // No tags here!
        return null;
    }
}
