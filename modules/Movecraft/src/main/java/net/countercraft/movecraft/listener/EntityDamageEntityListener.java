package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageEntityListener implements Listener {
    @EventHandler
    public void onPlayerDeath(EntityDamageByEntityEvent e) {  // changed to death so when you shoot up an airship and hit the pilot, it still sinks
        if (e instanceof Player) {
            Player p = (Player) e;
            CraftManager.getInstance().removeCraft(CraftManager.getInstance().getCraftByPlayer(p), CraftReleaseEvent.Reason.DEATH);
        }
    }
}
