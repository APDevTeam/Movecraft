package net.countercraft.movecraft.listener;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.async.FuelBurnRunnable;
import net.countercraft.movecraft.events.RunnableRegistrationEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RunnableRegistrationListener implements Listener {

    @EventHandler
    public void onRegisterTickingFunctions(RunnableRegistrationEvent event) {
        event.register(1, AsyncManager::processCruise);
        event.register(1, AsyncManager::processSinking);
        event.register(1, new FuelBurnRunnable());
    }

}
