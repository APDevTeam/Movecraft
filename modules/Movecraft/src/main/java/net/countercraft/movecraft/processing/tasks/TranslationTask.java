package net.countercraft.movecraft.processing.tasks;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.Result;
import net.countercraft.movecraft.processing.UnaryTaskPredicate;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TranslationTask implements Supplier<Effect> {

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
    public Effect get() {
        var preTranslationResult = preTranslationValidators.stream().reduce(UnaryTaskPredicate::and).orElseThrow().validate(craft);
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
        Effect teleportEffect = teleportEntities(preTranslateEvent.getWorld());
        return null;
    }

    private Result maxHeightValidator(Craft craft, MovecraftLocation translation){
        if (translation.getY()>0 && craft.getHitBox().getMaxY() + translation.getY() > craft.getType().getMaxHeightLimit(craft.getWorld()) && craft.getType().getCollisionExplosion() <= 0f) {
            return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
        }
        return Result.succeed();
    }

    private Result minHeightValidator(Craft craft, MovecraftLocation translation){
        if (craft.getHitBox().getMinX() + translation.getY() < craft.getType().getMinHeightLimit(craft.getWorld()) && translation.getY() < 0 && !craft.getSinking() && !craft.getType().getUseGravity()) {
            return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
        }
        return Result.succeed();
    }

    private Result hoverOverValidator(Craft craft, MovecraftLocation translation, HitBox newHitBox){
        if (craft.getType().getForbiddenHoverOverBlocks().size() > 0){
            MovecraftLocation test = new MovecraftLocation(newHitBox.getMidPoint().getX(), newHitBox.getMinY(), newHitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (craft.getMovecraftWorld().getMaterial(test) == Material.AIR){
                test = test.translate(0, -1, 0);
            }
            Material testType = craft.getMovecraftWorld().getMaterial(test);
            if (craft.getType().getForbiddenHoverOverBlocks().contains(testType)){
                return Result.failWithMessage(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
            }
        }
        return Result.succeed();
    }

    private Effect moveCraft(){
        return null;
    }

    private Effect teleportEntities(World destinationWorld){
        return () -> {
            if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
                Location midpoint = craft.getHitBox().getMidPoint().toBukkit(craft.getWorld());
                for (Entity entity : craft.getWorld().getNearbyEntities(midpoint, craft.getHitBox().getXLength() / 2.0 + 1, craft.getHitBox().getYLength() / 2.0 + 2, craft.getHitBox().getZLength() / 2.0 + 1)) {
                    if (entity.getType() == EntityType.PLAYER) {
                        if(craft.getSinking()){
                            continue;
                        }
                        EntityUpdateCommand eUp = new EntityUpdateCommand(entity, translation.getX(), translation.getY(), translation.getZ(), 0, 0, destinationWorld);
                        eUp.doUpdate();
                    } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                        EntityUpdateCommand eUp = new EntityUpdateCommand(entity, translation.getX(), translation.getY(), translation.getZ(), 0, 0, destinationWorld);
                        eUp.doUpdate();
                    }
                }
            }
        };
    }
}
