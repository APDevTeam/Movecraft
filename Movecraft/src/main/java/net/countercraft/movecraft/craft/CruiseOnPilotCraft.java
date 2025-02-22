package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class CruiseOnPilotCraft extends BaseCraft implements PilotedCraft {
    private WeakReference<Player> pilot;
    private final UUID pilotUUID;

    public CruiseOnPilotCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot) {
        super(type, world);
        this.pilot = new WeakReference<>(pilot);
        // Copy UUID just to be safe
        this.pilotUUID = UUID.fromString(pilot.getUniqueId().toString());
        this.setAudience(Audience.empty());
    }

    @Nullable
    @Override
    public Player getPilot() {
        // Do not re-set the audience here! Crafts like this are like torpedoes, we dont need to receive messages from it
        if (this.pilot.get() == null) {
            this.pilot = new WeakReference<> (Bukkit.getPlayer(this.getPilotUUID()));
        } else {
            Player bukkitPilot = Bukkit.getPlayer(this.getPilotUUID());
            if (!this.pilot.get().isOnline() || (this.pilot.get() != bukkitPilot)) {
                this.pilot = new WeakReference<> (bukkitPilot);
            }
        }
        return this.pilot.get();
    }

    @Override
    public @NotNull UUID getPilotUUID() {
        return this.pilotUUID;
    }
}
