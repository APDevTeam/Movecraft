package net.countercraft.movecraft.features.fading;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.effects.SetBlockEffect;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Fades a block if the data for the intended block has not been mutated since creation.
 */
public class FadeTask implements Supplier<Effect> {
    private final @NotNull BlockData compareData;
    private final @NotNull BlockData setData;
    private final @NotNull MovecraftWorld world;
    private final @NotNull MovecraftLocation location;

    public FadeTask(@NotNull BlockData compareData, @NotNull BlockData setData, @NotNull MovecraftWorld world, @NotNull MovecraftLocation location){
        this.compareData = compareData;
        this.setData = setData;
        this.world = world;
        this.location = location;
    }

    @Override
    public Effect get() {
        var testData = world.getData(location);

        return () -> Objects.requireNonNull(Bukkit.getWorld(world.getWorldUUID()))
            .getChunkAtAsync(location.toBukkit(null))
            .thenRunAsync(() -> WorldManager.INSTANCE.submit(() -> testData.equals(compareData)
                ? new SetBlockEffect(world, location, setData)
                : null));
    }
}
