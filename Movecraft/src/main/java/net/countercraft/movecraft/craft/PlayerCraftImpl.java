package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class PlayerCraftImpl extends BaseCraft implements PlayerCraft {
    private WeakReference<Player> pilot;
    private final UUID pilotUUID;
    private boolean pilotLocked;
    private double pilotLockedX;
    private double pilotLockedY;
    private double pilotLockedZ;

    public PlayerCraftImpl(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot) {
        super(type, world);
        // Copy UUID just to be safe
        this.pilotUUID = UUID.fromString(pilot.getUniqueId().toString());
        this.pilot = new WeakReference<>(pilot);
        pilotLocked = false;
        pilotLockedX = 0.0;
        pilotLockedY = 0.0;
        pilotLockedZ = 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerCraftImpl))
            return false;

        return super.equals(obj);
    }

    @Nullable
    @Override
    public Player getPilot() {
        if (this.pilot.get() == null) {
            this.pilot = new WeakReference<> (Bukkit.getPlayer(this.getPilotUUID()));
            this.setAudience(this.pilot.get());
        } else {
            Player bukkitPilot = Bukkit.getPlayer(this.getPilotUUID());
            if (!this.pilot.get().isOnline() || (this.pilot.get() != bukkitPilot)) {
                this.pilot = new WeakReference<> (bukkitPilot);
                this.setAudience(this.pilot.get());
            }
        }
        return this.pilot.get();
    }

    @Override
    public @NotNull UUID getPilotUUID() {
        return this.pilotUUID;
    }

    public boolean getPilotLocked() {
        return pilotLocked;
    }

    public void setPilotLocked(boolean pilotLocked) {
        this.pilotLocked = pilotLocked;
    }

    public double getPilotLockedX() {
        return pilotLockedX;
    }

    public void setPilotLockedX(double pilotLockedX) {
        this.pilotLockedX = pilotLockedX;
    }

    public double getPilotLockedY() {
        return pilotLockedY;
    }

    public void setPilotLockedY(double pilotLockedY) {
        this.pilotLockedY = pilotLockedY;
    }

    public double getPilotLockedZ() {
        return pilotLockedZ;
    }

    public void setPilotLockedZ(double pilotLockedZ) {
        this.pilotLockedZ = pilotLockedZ;
    }
}
