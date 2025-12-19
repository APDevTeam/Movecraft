package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

// TODO: Why doesnt this implement subcraft?? => it isnt used for subcraftRotate signs! Only when used while the parent isnt piloted!
public class SubcraftRotateCraft extends BaseCraft implements PilotedCraft {
    private WeakReference<Player> pilot;
    private final UUID pilotUUID;

    public SubcraftRotateCraft(@NotNull TypeSafeCraftType type, @NotNull World world, @NotNull Player pilot) {
        super(type, world);
        this.pilot = new WeakReference<>(pilot);
        // Copy UUID just to be safe
        this.pilotUUID = UUID.fromString(pilot.getUniqueId().toString());
    }

    @Nullable
    @Override
    public Player getPilot() {
        if (this.pilot.get() == null) {
            this.pilot = new WeakReference<>(Bukkit.getPlayer(this.getPilotUUID()));
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
}
