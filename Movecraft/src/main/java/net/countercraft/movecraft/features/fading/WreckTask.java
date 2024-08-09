package net.countercraft.movecraft.features.fading;

import io.papermc.paper.util.Tick;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.DeferredEffect;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class WreckTask implements Supplier<Effect> {

    private final @NotNull HitBox hitBox;
    private final @NotNull Map<MovecraftLocation, BlockData> phaseBlocks;
    private final @NotNull MovecraftWorld world;
    private final int fadeMaximumTicks;

    public WreckTask(@NotNull HitBox wreck, @NotNull MovecraftWorld world, @NotNull Map<MovecraftLocation, BlockData> phaseBlocks){
        this.hitBox = Objects.requireNonNull(wreck);
        this.phaseBlocks = Objects.requireNonNull(phaseBlocks);
        this.world = Objects.requireNonNull(world);

        int ticks = Tick.tick().fromDuration(Duration.ofSeconds(Settings.FadeWrecksAfter));
        this.fadeMaximumTicks = (int) (ticks / (Settings.FadePercentageOfWreckPerCycle / 100.0));
    }

    @Override
    public Effect get() {
        return partialUpdate();
    }

    public Effect partialUpdate(){
        // TODO: parallel?
        Effect accumulator = Effect.NONE;
        for (MovecraftLocation location : hitBox){
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
