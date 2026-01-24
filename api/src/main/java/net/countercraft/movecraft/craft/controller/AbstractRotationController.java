package net.countercraft.movecraft.craft.controller;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.InitiateTranslateEvent;
import net.countercraft.movecraft.sign.SignListener;
import net.countercraft.movecraft.util.SerializationUtil;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractRotationController implements ConfigurationSerializable, Cloneable {

    protected final boolean turningEnabled;

    public AbstractRotationController(Map<String, Object> rawData) {
        // Nothing to deserialize by default
        this.turningEnabled = SerializationUtil.deserializeBoolean("TurningEnabled", rawData, true);
    }

    public boolean onHelmInteraction(final Craft craft, final SignListener.SignWrapper signWrapper, final Action clickType, final Player interactor) {
        return this.turningEnabled;
    }

    public void onInitiateTranslation(final InitiateTranslateEvent event) {
        // Uninteresting for default movecraft but interesting for addons!
    }

    // TODO: Move rotateCraft logic to RotationController!
//    public boolean rotateCraft(final Craft craft, final MovecraftRotation movecraftRotation, final MovecraftLocation originPoint, BiConsumer<Craft, MovecraftRotation> rotationProcessor) {
//        return rotateCraft(craft, movecraftRotation, originPoint, false, rotationProcessor);
//    }
//    public boolean rotateCraft(final Craft craft, final MovecraftRotation movecraftRotation, final MovecraftLocation originPoint, boolean isSubCraft, BiConsumer<Craft, MovecraftRotation> rotationProcessor) {
//        if (!isSubCraft) {
//
//        }
//    }


    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serialized = Map.of(
                "TurningEnabled", this.turningEnabled
        );
        return addToSerialize(serialized);
    }
    public abstract @NotNull Map<String, Object> addToSerialize(@NotNull Map<String, Object> serialized);

    public abstract AbstractRotationController clone();

}
