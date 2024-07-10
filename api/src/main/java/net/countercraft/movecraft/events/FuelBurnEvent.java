package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FuelBurnEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    private double burningFuel, fuelBurnRate;
    public FuelBurnEvent(@NotNull Craft craft, double burningFuel, double fuelBurnRate) {
        super(craft);
        this.burningFuel = burningFuel;
        this.fuelBurnRate = fuelBurnRate;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public double getBurningFuel() {
        return burningFuel;
    }

    public void setBurningFuel(double burningFuel) {
        this.burningFuel = burningFuel;
    }

    public double getFuelBurnRate() {
        return fuelBurnRate;
    }

    public void setFuelBurnRate(double fuelBurnRate) {
        this.fuelBurnRate = fuelBurnRate;
    }
}
