package net.countercraft.movecraft.compatmanager;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GriefPreventionCompatManager implements Listener {
    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){
        if (Movecraft.getInstance().getGriefPreventionPlugin() == null){
            return;
        }
        Craft craft = event.getCraft();
        for (MovecraftLocation mLoc : event.getNewHitBox()){
            Location loc = mLoc.toBukkit(craft.getW());
            GriefPrevention gp = Movecraft.getInstance().getGriefPreventionPlugin();
            String gpMessage = gp.allowBuild(craft.getNotificationPlayer(), loc);
            if (gpMessage == null){
                return;
            }
            if (gpMessage.contains(gp.dataStore.getMessage(Messages.NoBuildPermission).replace("{0}", ""))){
                event.setFailMessage(I18nSupport.getInternationalisedString("Translation - Failed GriefPrevention"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCraftRotate(CraftRotateEvent event){
        if (Movecraft.getInstance().getGriefPreventionPlugin() == null){
            return;
        }
        Craft craft = event.getCraft();
        for (MovecraftLocation mLoc : event.getNewHitBox()){
            Location loc = mLoc.toBukkit(craft.getW());
            GriefPrevention gp = Movecraft.getInstance().getGriefPreventionPlugin();
            String gpMessage = gp.allowBuild(craft.getNotificationPlayer(), loc);
            if (gpMessage == null){
                return;
            }
            if (gpMessage.contains(gp.dataStore.getMessage(Messages.NoBuildPermission).replace("{0}", ""))){
                event.setFailMessage(I18nSupport.getInternationalisedString("Rotation - Failed GriefPrevention"));
                event.setCancelled(true);
            }
        }
    }
}
