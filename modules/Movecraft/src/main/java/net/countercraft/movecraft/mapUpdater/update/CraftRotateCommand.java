package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import net.countercraft.movecraft.api.craft.Craft;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;


public class CraftRotateCommand extends UpdateCommand{
    private Logger logger = Movecraft.getInstance().getLogger();
    @NotNull private final Craft craft;
    @NotNull private final Rotation rotation;
    @NotNull final MovecraftLocation originLocation;

    public CraftRotateCommand(@NotNull final Craft craft,@NotNull final MovecraftLocation originLocation, @NotNull final Rotation rotation){
        this.craft = craft;
        this.rotation = rotation;
        this.originLocation = originLocation;
    }

    @Override
    public void doUpdate() {
        Movecraft.getInstance().getWorldHandler().rotateCraft(craft,originLocation,rotation);
    }



    @NotNull
    public Craft getCraft(){
        return craft;
    }
}
