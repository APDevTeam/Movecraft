package net.countercraft.movecraft.compatmanager;

import com.songoda.kingdoms.constants.kingdom.Kingdom;
import com.songoda.kingdoms.constants.land.Land;
import com.songoda.kingdoms.constants.land.SimpleChunkLocation;
import com.songoda.kingdoms.constants.player.KingdomPlayer;
import com.songoda.kingdoms.main.Kingdoms;
import com.songoda.kingdoms.manager.game.GameManagement;
import com.songoda.kingdoms.manager.game.LandManager;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KingdomsCompatManager implements Listener {
    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){
        if (Movecraft.getInstance().getKingdomsPlugin() == null){
            return;
        }
        Craft craft = event.getCraft();
        Land foreignLand = null;
        for (MovecraftLocation moveLoc : event.getNewHitBox()) {
            LandManager lm = GameManagement.getLandManager();
            SimpleChunkLocation c = new SimpleChunkLocation(moveLoc.toBukkit(craft.getW()).getChunk());
            Land land = lm.getOrLoadLand(c);
            KingdomPlayer kp = Kingdoms.getInstance().getManagers().getPlayerManager().getSession(craft.getNotificationPlayer());
            Kingdom k = kp.getKingdom();
            if (land.getOwnerUUID() == null){
                continue;
            }
            if (land.getOwnerUUID() != k.getKingdomUuid()){
                foreignLand = land;
                break;
            }
        }
        if (foreignLand == null){
            return;
        }
        event.setFailMessage(I18nSupport.getInternationalisedString("Translation - Kingdoms Player not allowed to build"));
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraftRotate(CraftRotateEvent event){
        if (Movecraft.getInstance().getKingdomsPlugin() == null){
            return;
        }
        Craft craft = event.getCraft();
        Land foreignLand = null;
        for (MovecraftLocation moveLoc : event.getNewHitBox()) {
            LandManager lm = GameManagement.getLandManager();
            SimpleChunkLocation c = new SimpleChunkLocation(moveLoc.toBukkit(craft.getW()).getChunk());
            Land land = lm.getOrLoadLand(c);
            KingdomPlayer kp = Kingdoms.getInstance().getManagers().getPlayerManager().getSession(craft.getNotificationPlayer());
            Kingdom k = kp.getKingdom();
            if (land.getOwnerUUID() == null){
                continue;
            }
            if (land.getOwnerUUID() != k.getKingdomUuid()){
                foreignLand = land;
                break;
            }
        }
        if (foreignLand == null){
            return;
        }
        event.setFailMessage(I18nSupport.getInternationalisedString("Rotation - Kingdoms Player not allowed to build"));
        event.setCancelled(true);
    }
}
