package net.countercraft.movecraft.features.fading;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.DeferredEffect;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.util.CollectorUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WreckTask implements Supplier<Effect> {

    private final @NotNull HitBox hitBox;
    private final @NotNull Map<MovecraftLocation, BlockData> phaseBlocks;
    private final @NotNull MovecraftWorld world;
    private final int fadeDelayTicks;
    private final int maximumFadeDurationTicks;

    public WreckTask(@NotNull HitBox wreck, @NotNull MovecraftWorld world, @NotNull Map<MovecraftLocation, BlockData> phaseBlocks){
        this.hitBox = Objects.requireNonNull(wreck);
        this.phaseBlocks = Objects.requireNonNull(phaseBlocks);
        this.world = Objects.requireNonNull(world);
        this.fadeDelayTicks = Settings.FadeWrecksAfter * 20;
        this.maximumFadeDurationTicks = (int) (Settings.FadeTickCooldown *  (100.0 / Settings.FadePercentageOfWreckPerCycle));
    }

    @Override
    public Effect get() {
        var updates = hitBox
            .asSet()
            .stream()
            .collect(Collectors.groupingBy(location -> location.scalarDivide(16).hadamardProduct(1,0,1), CollectorUtils.toHitBox()))
            .values()
            .stream()
            .map(slice -> ForkJoinTask.adapt(() -> partialUpdate(slice)))
            .toList();

        return ForkJoinTask
            .invokeAll(updates)
            .stream()
            .map(ForkJoinTask::join)
            .reduce(Effect.NONE, Effect::andThen);
    }

    private @NotNull Effect partialUpdate(@NotNull HitBox slice){
        Effect accumulator = Effect.NONE;
        for (MovecraftLocation location : slice){
            // Get the existing data
            final BlockData data = world.getData(location);
            // Determine the replacement data
            BlockData replacementData = phaseBlocks.getOrDefault(location, Material.AIR.createBlockData());
            // Calculate ticks until replacement
            long fadeTicks = this.fadeDelayTicks;
            fadeTicks += (int) (Math.random() * maximumFadeDurationTicks);
            fadeTicks += 20L * Settings.ExtraFadeTimePerBlock.getOrDefault(data.getMaterial(), 0);
            // Deffer replacement until time delay elapses
            accumulator = accumulator.andThen(new DeferredEffect(fadeTicks, () -> WorldManager.INSTANCE.submit(new FadeTask(data, replacementData, world, location))));
        }

        // TODO: Determine if we need to reduce the spread of deferred effects due to runnable overhead
        return accumulator;
    }
}
