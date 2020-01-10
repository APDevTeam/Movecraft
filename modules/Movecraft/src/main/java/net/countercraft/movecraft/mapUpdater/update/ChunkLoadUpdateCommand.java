package net.countercraft.movecraft.mapUpdater.update;

import org.bukkit.Chunk;

import java.util.Objects;

public class ChunkLoadUpdateCommand extends UpdateCommand {

    private final Chunk chunk;
    private final boolean forceLoaded;
    private final boolean generate;

    public ChunkLoadUpdateCommand(Chunk chunk, boolean forceLoaded, boolean generate) {
        this.chunk = chunk;
        this.forceLoaded = forceLoaded;
        this.generate = generate;
    }

    @Override
    public void doUpdate() {
        chunk.setForceLoaded(forceLoaded);
        chunk.load(generate);
    }

    public boolean isForceLoaded() {
        return forceLoaded;
    }

    public boolean isGenerate() {
        return generate;
    }

    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChunkLoadUpdateCommand))
            return false;
        ChunkLoadUpdateCommand that = (ChunkLoadUpdateCommand) o;
        return forceLoaded == that.forceLoaded &&
                generate == that.generate &&
                chunk.equals(that.chunk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunk, forceLoaded, generate);
    }
}
