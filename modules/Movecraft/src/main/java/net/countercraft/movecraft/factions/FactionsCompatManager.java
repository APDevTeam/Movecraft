package net.countercraft.movecraft.factions;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.MPlayer;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class FactionsCompatManager implements Listener {

    public void onCraftTranslate(CraftTranslateEvent event){
        if (Movecraft.getInstance().getFactionsPlugin() == null){
            return;
        }
        Factions factions = Movecraft.getInstance().getFactionsPlugin();
        Player player = event.getCraft().getNotificationPlayer();
        MPlayer mPlayer = MPlayer.get(player);

    }
}
