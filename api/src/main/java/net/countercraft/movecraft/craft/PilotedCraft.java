package net.countercraft.movecraft.craft;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PilotedCraft extends Craft {

    @NotNull
    Player getPilot();
}
