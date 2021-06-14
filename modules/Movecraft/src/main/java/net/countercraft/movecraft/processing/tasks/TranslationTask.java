package net.countercraft.movecraft.processing.tasks;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.Result;
import net.countercraft.movecraft.processing.UnaryTaskPredicate;
import net.countercraft.movecraft.processing.WorldManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TranslationTask implements Runnable {

    private static final List<UnaryTaskPredicate<Craft>> preTranslationValidators = new ArrayList<>();
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
    public void run() {
        var preTranslationResult = preTranslationValidators.stream().reduce(UnaryTaskPredicate::and).orElseThrow().validate(craft);
        if(!preTranslationResult.isSucess()){
            craft.getAudience().sendMessage(Component.text(preTranslationResult.getMessage()));
            return;
        }
        var preTranslateEvent = WorldManager.INSTANCE.executeMain(()->{
            var event = new CraftPreTranslateEvent(craft, translation.getX(), translation.getY(), translation.getZ(), craft.getWorld());
            Bukkit.getServer().getPluginManager().callEvent(event);
            return event;
        });
        if (preTranslateEvent.isCancelled()) {
            craft.getAudience().sendMessage(Component.text(preTranslateEvent.getFailMessage()));
            return;
        }
        translation = new MovecraftLocation(preTranslateEvent.getDx(), preTranslateEvent.getDy(), preTranslateEvent.getDz());
        destinationWorld = CachedMovecraftWorld.of(preTranslateEvent.getWorld());
        //TODO: add portal movement as an event handler

    }
}
