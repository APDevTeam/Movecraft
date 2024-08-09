package net.countercraft.movecraft.features.fading;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.DeferredEffect;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.tasks.detection.DetectionTask;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.CollectorUtils;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WreckTask implements Supplier<Effect> {

    private final @NotNull HitBox hitBox;
    private final @NotNull Map<MovecraftLocation, BlockData> phaseBlocks;
    private final @NotNull MovecraftWorld world;
    private final int fadeMaximumTicks;

    public WreckTask(@NotNull HitBox wreck, @NotNull MovecraftWorld world, @NotNull Map<MovecraftLocation, BlockData> phaseBlocks){
        this.hitBox = Objects.requireNonNull(wreck);
        this.phaseBlocks = Objects.requireNonNull(phaseBlocks);
        this.world = Objects.requireNonNull(world);

        // TODO: figure out when Tick class got added
//        int ticks = Tick.tick().fromDuration(Duration.ofSeconds(Settings.FadeWrecksAfter));
        int ticks = Settings.FadeWrecksAfter * 20;
        this.fadeMaximumTicks = (int) (ticks / (Settings.FadePercentageOfWreckPerCycle / 100.0));
    }

    @Override
    public Effect get() {
        var updates = hitBox
            .asSet()
            .stream()
            .collect(Collectors.groupingBy(location -> location.scalarMod(16), CollectorUtils.toHitBox()))
            .values()
            .stream()
            .map(slice -> ForkJoinTask.adapt(() -> partialUpdate(slice)))
            .toList();

        return ForkJoinTask.invokeAll(updates).stream().map(ForkJoinTask::join).reduce(Effect.NONE, Effect::andThen);
    }

    public Effect partialUpdate(HitBox slice){
        Effect accumulator = Effect.NONE;
        for (MovecraftLocation location : slice){
            // Get the existing data
            final BlockData data = world.getData(location);
            // Determine the replacement data
            BlockData replacementData = phaseBlocks.getOrDefault(location, Material.AIR.createBlockData());
            // Calculate ticks until replacement
            long fadeTicks = (int) (Math.random() * fadeMaximumTicks);
            fadeTicks += Settings.ExtraFadeTimePerBlock.getOrDefault(data.getMaterial(), 0);
            // Deffer replacement until time delay elapses
            accumulator.andThen(new DeferredEffect(fadeTicks, () -> WorldManager.INSTANCE.submit(new FadeTask(data, replacementData, world, location))));
        }

        // TODO: Determine if we need to reduce the spread of deferred effects due to runnable overhead
        return accumulator;
    }
}
