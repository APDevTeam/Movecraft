package net.countercraft.movecraft.compatmanager;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.TerritoryAccess;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FactionsCompatManager implements Listener {

    @EventHandler
    public void onCraftTranslate(CraftTranslateEvent event){
        if (Movecraft.getInstance().getFactionsPlugin() == null){
            return;
        }
        Factions factions = Movecraft.getInstance().getFactionsPlugin();
        Player player = event.getCraft().getNotificationPlayer();
        MPlayer mPlayer = MPlayer.get(player);
        PS ps = null;
        Faction faction = FactionColl.get().getNone();
        for (MovecraftLocation location : event.getNewHitBox()) {
            ps = PS.valueOf(location.toBukkit(event.getCraft().getW()));
            faction = BoardColl.get().getFactionAt(ps);
            if (faction != FactionColl.get().getNone()){
                break;
            }
        }
        if (faction == FactionColl.get().getNone()){
            return;
        }
        if (faction.equals(FactionColl.get().getWarzone())){
            if (Settings.FactionsBlockMoveInWarzone ){
                event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player not permitted to move in warzone"));
                event.setCancelled(true);
            }
        }
        if (faction.equals(FactionColl.get().getSafezone())){
            if (Settings.FactionsBlockMoveInSafezone ){
                event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player not permitted to move in safezone"));
                event.setCancelled(true);
            }

        }
        if (ps == null){
            return;
        }
        TerritoryAccess tAccess = BoardColl.get().getTerritoryAccessAt(ps);

        Faction pFaction = mPlayer.getFaction();
        if (!tAccess.isFactionGranted(pFaction)){
            event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Faction Has No Access").replace("{FACTION}",faction.getColorTo(pFaction) + faction.getName() + ChatColor.RESET));
            event.setCancelled(true);
            return;
        }
        if (!tAccess.isMPlayerGranted(mPlayer)){
            event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player Has No Access").replace("{FACTION}",faction.getColorTo(pFaction) + faction.getName() + ChatColor.RESET));
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onCraftRotate(CraftRotateEvent event){
        if (Movecraft.getInstance().getFactionsPlugin() == null){
            return;
        }
        Factions factions = Movecraft.getInstance().getFactionsPlugin();
        Player player = event.getCraft().getNotificationPlayer();
        MPlayer mPlayer = MPlayer.get(player);
        PS ps = null;
        Faction faction = FactionColl.get().getNone();
        for (MovecraftLocation location : event.getNewHitBox()) {
            ps = PS.valueOf(location.toBukkit(event.getCraft().getW()));
            faction = BoardColl.get().getFactionAt(ps);
            if (faction != FactionColl.get().getNone()){
                break;
            }
        }
        if (ps == null){
            return;
        }
        if (faction == FactionColl.get().getNone()){
            return;
        }
        if (faction.equals(FactionColl.get().getWarzone())){
            if (Settings.FactionsBlockMoveInWarzone ){
                event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player not permitted to move in warzone"));
                event.setCancelled(true);
            }
        }
        if (faction.equals(FactionColl.get().getSafezone())){
            if (Settings.FactionsBlockMoveInSafezone ){
                event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player not permitted to move in safezone"));
                event.setCancelled(true);
            }

        }

        TerritoryAccess tAccess = BoardColl.get().getTerritoryAccessAt(ps);

        Faction pFaction = mPlayer.getFaction();
        if (!tAccess.isFactionGranted(pFaction)){
            event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Faction Has No Access").replace("{FACTION}",faction.getColorTo(pFaction) + faction.getName() + ChatColor.RESET));
            event.setCancelled(true);
            return;
        }
        if (!tAccess.isMPlayerGranted(mPlayer)){
            event.setFailMessage(I18nSupport.getInternationalisedString("Factions - Translation Failed Player Has No Access").replace("{FACTION}",faction.getColorTo(pFaction) + faction.getName() + ChatColor.RESET));
            event.setCancelled(true);
        }
    }
}
