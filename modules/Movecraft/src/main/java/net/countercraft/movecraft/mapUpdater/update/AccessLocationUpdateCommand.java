package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AccessLocationUpdateCommand extends UpdateCommand {

    private final @NotNull InventoryView inventoryView;
    private final @NotNull Location newAccessLocation;

    public AccessLocationUpdateCommand(@NotNull InventoryView inventoryView, @NotNull Location newAccessLocation) {
        this.inventoryView = inventoryView;
        this.newAccessLocation = newAccessLocation;
    }

    public @NotNull InventoryView getInventoryView() {
        return inventoryView;
    }

    public @NotNull Location getNewAccessLocation() {
        return newAccessLocation;
    }

    @Override
    public void doUpdate() {
        Movecraft.getInstance().getWorldHandler().setAccessLocation(inventoryView, newAccessLocation);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AccessLocationUpdateCommand)) {
            return false;
        }
        AccessLocationUpdateCommand other = (AccessLocationUpdateCommand) obj;
        return other.inventoryView.equals(this.inventoryView) &&
                other.newAccessLocation.equals(this.newAccessLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inventoryView, newAccessLocation);
    }

}
