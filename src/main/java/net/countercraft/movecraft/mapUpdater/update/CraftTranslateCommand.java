package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class CraftTranslateCommand extends UpdateCommand {
    private Logger logger = Movecraft.getInstance().getLogger();
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;
    @NotNull private final Rotation rotation;


    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement) {
        this(craft,displacement, Rotation.NONE);
    }

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull Rotation rotation){
        this.craft = craft;
        this.displacement = displacement;
        this.rotation = rotation;
    }

    @Override
    public void doUpdate() {
        Movecraft.getInstance().getWorldHandler().translateCraft(craft,displacement,rotation);
    }



    @NotNull
    public Craft getCraft(){
        return craft;
    }



}
