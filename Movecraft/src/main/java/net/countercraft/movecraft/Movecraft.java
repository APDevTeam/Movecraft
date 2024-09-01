/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft;

import net.countercraft.movecraft.async.AsyncManager;
import net.countercraft.movecraft.commands.*;
import net.countercraft.movecraft.config.DataPackService;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.config.SettingsService;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.features.contacts.ContactsCommand;
import net.countercraft.movecraft.features.contacts.ContactsManager;
import net.countercraft.movecraft.features.contacts.ContactsSign;
import net.countercraft.movecraft.features.fading.WreckManager;
import net.countercraft.movecraft.features.status.StatusManager;
import net.countercraft.movecraft.features.status.StatusSign;
import net.countercraft.movecraft.lifecycle.ListenerLifecycleService;
import net.countercraft.movecraft.lifecycle.ServiceHost;
import net.countercraft.movecraft.lifecycle.WorkerServiceHost;
import net.countercraft.movecraft.listener.*;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.sign.*;
import net.countercraft.movecraft.support.SmoothTeleportFactory;
import net.countercraft.movecraft.support.WorldHandlerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.int4.dirk.api.Injector;
import org.int4.dirk.di.Injectors;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    private static Movecraft instance;

    private Logger logger;
    private boolean shuttingDown;
    private Injector injector;

    public static synchronized Movecraft getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        injector.getInstance(ServiceHost.class).stopAll();
        injector = null;
    }

    @Override
    public void onEnable() {
        injector = Injectors.manual();
        injector.registerInstance(getLogger());
        injector.registerInstance(this);
        injector.register(AsyncManager.class);
        injector.register(MapUpdateManager.class);
        injector.register(SmoothTeleportFactory.class);
        injector.register(WorldHandlerFactory.class);
        injector.registerInstance(WorldManager.INSTANCE);
        injector.register(WreckManager.class);
        injector.register(I18nSupport.class);
        injector.register(SettingsService.class);

        // TODO: make this work somehow
        if(shuttingDown && Settings.IGNORE_RESET) {
            logger.severe("Movecraft is incompatible with the reload command. Movecraft has shut down and will restart when the server is restarted.");
            logger.severe("If you wish to use the reload command and Movecraft, you may disable this check inside the config.yml by setting 'safeReload: false'");
            getPluginLoader().disablePlugin(this);

            return;
        }

        injector.register(DataPackService.class);
        injector.register(CraftManager.class);

        //TODO: migrate to aikar or brigadier commands, left in place for now
        getCommand("movecraft").setExecutor(new MovecraftCommand());
        getCommand("release").setExecutor(new ReleaseCommand());
        getCommand("pilot").setExecutor(new PilotCommand());
        getCommand("rotate").setExecutor(new RotateCommand());
        getCommand("cruise").setExecutor(new CruiseCommand());
        getCommand("craftreport").setExecutor(new CraftReportCommand());
        getCommand("manoverboard").setExecutor(new ManOverboardCommand());
        getCommand("scuttle").setExecutor(new ScuttleCommand());
        getCommand("crafttype").setExecutor(new CraftTypeCommand());
        getCommand("craftinfo").setExecutor(new CraftInfoCommand());

        injector.register(InteractListener.class);
        injector.register(BlockListener.class);
        injector.register(PlayerListener.class);
        injector.register(ChunkManager.class);

        // Signs
        injector.register(AscendSign.class);
        injector.register(CraftSign.class);
        injector.register(CruiseSign.class);
        injector.register(DescendSign.class);
        injector.register(HelmSign.class);
        injector.register(MoveSign.class);
        injector.register(NameSign.class);
        injector.register(PilotSign.class);
        injector.register(RelativeMoveSign.class);
        injector.register(ReleaseSign.class);
        injector.register(RemoteSign.class);
        injector.register(SpeedSign.class);
        injector.register(SubcraftRotateSign.class);
        injector.register(TeleportSign.class);
        injector.register(ScuttleSign.class);

        injector.register(CraftPilotListener.class);
        injector.register(CraftReleaseListener.class);

        injector.register(ContactsManager.class);
        injector.register(ContactsSign.class);
        getCommand("contacts").setExecutor(new ContactsCommand());

        injector.register(StatusManager.class);
        injector.register(StatusSign.class);

        // Lifecycle management
        injector.register(WorkerServiceHost.class);
        injector.register(ListenerLifecycleService.class);

        // Startup
        injector.getInstance(ServiceHost.class).startAll();
        logger.info("[V %s] has been enabled.".formatted(getDescription().getVersion()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        logger = getLogger();
        saveDefaultConfig();
    }

    public WorldHandler getWorldHandler(){
        return injector.getInstance(WorldHandler.class);
    }

    public SmoothTeleport getSmoothTeleport() {
        return injector.getInstance(SmoothTeleport.class);
    }

    public AsyncManager getAsyncManager() {
        return injector.getInstance(AsyncManager.class);
    }

    public @NotNull WreckManager getWreckManager(){
        return injector.getInstance(WreckManager.class);
    }
}
