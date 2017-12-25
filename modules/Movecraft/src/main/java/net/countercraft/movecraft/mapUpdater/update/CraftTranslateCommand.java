package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.config.Settings;
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
        long time = System.nanoTime();
        Movecraft.getInstance().getWorldHandler().translateCraft(craft,displacement);
        time = System.nanoTime() - time;
        if(Settings.Debug)
            logger.info("Total time: " + (time / 1e9) + " seconds. Moving with cooldown of " + craft.getTickCooldown());
        //MapUpdateManager.getInstance().blockUpdatesPerCraft.put(craft,(int)(time * 1e18));
        craft.addMoveTime(time/1e9f);
    }



    @NotNull
    public Craft getCraft(){
        return craft;
    }



}
