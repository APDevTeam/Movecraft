package net.countercraft.movecraft.towny;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.events.HitBoxDetectEvent;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.utils.TownyWorldHeightLimits;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownyCompatManager implements Listener {

    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){

        TownyWorldHeightLimits whLimits = TownyUtils.getWorldLimits(event.getCraft().getW());
        TownBlock townBlock = null;
        for (MovecraftLocation ml : event.getNewHitBox()){
            townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getW()));
        }
        if (townBlock == null){
            return;
        }
        Town town = TownyUtils.getTown(townBlock);
    }

    @EventHandler
    public void onHitboxDetect(HitBoxDetectEvent event) {
        for (MovecraftLocation ml : event.getHitBox()) {
            TownBlock townBlock = TownyUtils.getTownBlock(ml.toBukkit(event.getCraft().getW()));
            if (townBlock == null || !townBlock.hasTown()) {
                continue;
            }
            Town town = TownyUtils.getTown(townBlock);
            if (town == null)
                continue;
        }
    }
}
