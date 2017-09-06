package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class CraftTranslateCommand extends UpdateCommand {
    private Logger logger = Movecraft.getInstance().getLogger();
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement){
        this.craft = craft;
        this.displacement = displacement;
    }

    @Override
    public void doUpdate() {
        Movecraft.getInstance().getWorldHandler().translateCraft(craft,displacement);
    }



    @NotNull
    public Craft getCraft(){
        return craft;
    }



}
