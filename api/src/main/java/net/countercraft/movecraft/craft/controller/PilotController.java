package net.countercraft.movecraft.craft.controller;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PilotController extends Controller {
    @NotNull
    Player getPilot();
}
