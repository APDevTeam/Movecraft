package net.countercraft.movecraft.craft;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PilotedCraft extends BaseCraft implements PlayerCraft{
    private final UUID id = UUID.randomUUID();
    private final Player pilot;
    public PilotedCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player player) {
        super(type, world);
        this.pilot = player;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PilotedCraft)){
            return false;
        }
        return this.id.equals(((PilotedCraft)obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NotNull
    @Override
    public Player getPlayer() {
        return pilot;
    }
}
