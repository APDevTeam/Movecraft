package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.features.contacts.ContactProvider;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class SinkingCraftImpl extends BaseCraft implements SinkingCraft, ContactProvider {
    public SinkingCraftImpl(@NotNull Craft original) {
        super(original.getType(), original.getWorld(), original.getUUID());
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

    @Override
    public Component getDetectedMessage(boolean isNew, Craft detectingCraft) {
        return Component.empty();
    }

    @Override
    public boolean contactPickedUpBy(Craft other) {
        return false;
    }

    @Override
    public MovecraftLocation getContactLocation() {
        return this.getCraftOrigin();
    }

    @Override
    public double getDetectionMultiplier(boolean overWaterLine, MovecraftWorld world) {
        return 0;
    }
}
