package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ManOverboardEvent extends CraftEvent {
    private Location location;

    public ManOverboardEvent(@NotNull Craft c, Location location) {
        super(c);
        this.location = location;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
