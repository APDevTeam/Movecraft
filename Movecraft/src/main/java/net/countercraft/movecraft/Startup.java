package net.countercraft.movecraft;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.config.DataPackHostedService;
import net.countercraft.movecraft.config.SettingsHostedService;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.features.contacts.ContactsManager;
import net.countercraft.movecraft.features.contacts.ContactsSign;
import net.countercraft.movecraft.features.fading.WreckManager;
import net.countercraft.movecraft.features.status.StatusManager;
import net.countercraft.movecraft.features.status.StatusSign;
import net.countercraft.movecraft.lifecycle.PluginBuilder;
import net.countercraft.movecraft.listener.BlockListener;
import net.countercraft.movecraft.listener.CraftPilotListener;
import net.countercraft.movecraft.listener.CraftReleaseListener;
import net.countercraft.movecraft.listener.InteractListener;
import net.countercraft.movecraft.listener.PlayerListener;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.support.SmoothTeleportFactory;
import net.countercraft.movecraft.support.WorldHandlerFactory;

public class Startup {
    public static void registerServices(PluginBuilder injector){
        injector
            .register(AsyncManager.class)
            .register(MapUpdateManager.class)
            .register(SmoothTeleportFactory.class)
            .register(WorldHandlerFactory.class)
            .registerInstance(WorldManager.INSTANCE)
            .register(WreckManager.class)
            .register(I18nSupport.class)
            .register(SettingsHostedService.class)
            .register(DataPackHostedService.class)
            .register(CraftManager.class)
            .register(InteractListener.class)
            .register(BlockListener.class)
            .register(PlayerListener.class)
            .register(ChunkManager.class)
            .register(CraftPilotListener.class)
            .register(CraftReleaseListener.class)
            .register(ContactsManager.class)
            .register(StatusManager.class);

        // Signs
        injector
            .register(StatusSign.class)
            .register(ContactsSign.class);
    }
}
