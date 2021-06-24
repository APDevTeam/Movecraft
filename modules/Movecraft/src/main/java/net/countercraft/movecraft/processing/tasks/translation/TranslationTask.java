package net.countercraft.movecraft.processing.tasks.translation;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.MonadicPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.tasks.translation.effects.TeleportationEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TranslationTask implements Supplier<Effect> {

    private static final List<MonadicPredicate<Craft>> preTranslationValidators = new ArrayList<>();
    static {
        preTranslationValidators.add((craft -> craft.getHitBox().isEmpty() ? Result.failWithMessage("Empty hitbox") : Result.succeed()));
        preTranslationValidators.add((craft -> craft.getDisabled() && !craft.getSinking() ? Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled")) : Result.succeed()));
    }

    private MovecraftRotation rotation;
    private final Craft craft;
    private MovecraftWorld destinationWorld;
    private MovecraftLocation translation;

    public TranslationTask(@NotNull Craft craft, @NotNull MovecraftLocation translation, @NotNull MovecraftWorld destinationWorld, @NotNull MovecraftRotation rotation) {
        this.rotation = rotation;
        this.craft = craft;
        this.translation = translation;
        this.destinationWorld = destinationWorld;
    }

    @Override
    public Effect get() {
        var preTranslationResult = preTranslationValidators.stream().reduce(MonadicPredicate::and).orElseThrow().validate(craft);
        if(!preTranslationResult.isSucess()){
            return () -> craft.getAudience().sendMessage(Component.text(preTranslationResult.getMessage()));
        }
        var preTranslateEvent = WorldManager.INSTANCE.executeMain(()->{
            var event = new CraftPreTranslateEvent(craft, translation.getX(), translation.getY(), translation.getZ(), craft.getWorld());
            Bukkit.getServer().getPluginManager().callEvent(event);
            return event;
        });
        if (preTranslateEvent.isCancelled()) {
            return ()-> craft.getAudience().sendMessage(Component.text(preTranslateEvent.getFailMessage()));
        }
        translation = new MovecraftLocation(preTranslateEvent.getDx(), preTranslateEvent.getDy(), preTranslateEvent.getDz());
        destinationWorld = CachedMovecraftWorld.of(preTranslateEvent.getWorld());
        //TODO: Portal movement
        //TODO: Gravity

        //TODO: Max/Min heights
        //TODO: Fuel
        //TODO: Hover over

        //TODO: Obstruction, harvest, world border
        //TODO: Move fluid box

        //TODO: Translation event
        //TODO: Sinking?
        //TODO: Collision explosion
        //TODO: Collision event
        Effect movementEffect = moveCraft();
        Effect teleportEffect = new TeleportationEffect(craft, translation, preTranslateEvent.getWorld());
        return null;
    }

    private Effect moveCraft(){
        return null;
    }
}
