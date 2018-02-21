package net.countercraft.movecraft.sign;

import com.earth2me.essentials.User;
import net.countercraft.movecraft.Movecraft;
<<<<<<< HEAD
import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.events.SignTranslateEvent;
import net.countercraft.movecraft.api.config.Settings;
=======
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.config.Settings;
>>>>>>> upstream/master
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class CrewSign implements Listener{

    @EventHandler
    public final void onSignChange(SignChangeEvent event){
        if (!event.getLine(0).equalsIgnoreCase("Crew:")) {
            return;
        }
        Player player = event.getPlayer();
        event.setLine(1, player.getName());
    }

    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event){
        Craft craft = event.getCraft();
        if(Settings.AllowCrewSigns && event.getLine(0).equalsIgnoreCase("Crew:")) {
            String crewName=event.getLine(1);
            Player crewPlayer= Movecraft.getInstance().getServer().getPlayer(crewName);
            if(crewPlayer!=null) {
                Location location = event.getBlock().getLocation();
                location=location.subtract(0, 1, 0);
                if(craft.getW().getBlockAt(location).getType().equals(Material.BED_BLOCK)) {
                    crewPlayer.setBedSpawnLocation(location);
                    if(Settings.SetHomeToCrewSign)

                        if (Movecraft.getInstance().getEssentialsPlugin() != null){
                            User u = Movecraft.getInstance().getEssentialsPlugin().getUser(crewPlayer);
                            u.setHome("home", location);
                        }

                }
            }
        }
    }
}
