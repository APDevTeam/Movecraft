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
import net.countercraft.movecraft.commands.CraftInfoCommand;
import net.countercraft.movecraft.commands.CraftReportCommand;
import net.countercraft.movecraft.commands.CraftTypeCommand;
import net.countercraft.movecraft.commands.CruiseCommand;
import net.countercraft.movecraft.commands.ManOverboardCommand;
import net.countercraft.movecraft.commands.MovecraftCommand;
import net.countercraft.movecraft.commands.PilotCommand;
import net.countercraft.movecraft.commands.ReleaseCommand;
import net.countercraft.movecraft.commands.RotateCommand;
import net.countercraft.movecraft.commands.ScuttleCommand;
import net.countercraft.movecraft.config.DataPackHostedService;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.config.SettingsHostedService;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.features.contacts.ContactsCommand;
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
import net.countercraft.movecraft.sign.AscendSign;
import net.countercraft.movecraft.sign.CraftSign;
import net.countercraft.movecraft.sign.CruiseSign;
import net.countercraft.movecraft.sign.DescendSign;
import net.countercraft.movecraft.sign.HelmSign;
import net.countercraft.movecraft.sign.MoveSign;
import net.countercraft.movecraft.sign.NameSign;
import net.countercraft.movecraft.sign.PilotSign;
import net.countercraft.movecraft.sign.RelativeMoveSign;
import net.countercraft.movecraft.sign.ReleaseSign;
import net.countercraft.movecraft.sign.RemoteSign;
import net.countercraft.movecraft.sign.ScuttleSign;
import net.countercraft.movecraft.sign.SpeedSign;
import net.countercraft.movecraft.sign.SubcraftRotateSign;
import net.countercraft.movecraft.sign.TeleportSign;
import net.countercraft.movecraft.support.SmoothTeleportFactory;
import net.countercraft.movecraft.support.WorldHandlerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class Movecraft extends JavaPlugin {
    private static Movecraft instance;
    private boolean shuttingDown;
    private PluginBuilder.Application application;

    public static synchronized Movecraft getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        application.host().stopAll();
        application = null;
    }

    @Override
    public void onEnable() {
        var injector = PluginBuilder.create();
        registerServices(injector);

        // TODO: make this work somehow
        if(shuttingDown && Settings.IGNORE_RESET) {
            this.getLogger().severe("Movecraft is incompatible with the reload command. Movecraft has shut down and will restart when the server is restarted.");
            this.getLogger().severe("If you wish to use the reload command and Movecraft, you may disable this check inside the config.yml by setting 'safeReload: false'");
            getPluginLoader().disablePlugin(this);

            return;
        }

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

        //TODO: Sign rework
        getServer().getPluginManager().registerEvents(new AscendSign(), this);
        getServer().getPluginManager().registerEvents(new CraftSign(), this);
        getServer().getPluginManager().registerEvents(new CruiseSign(), this);
        getServer().getPluginManager().registerEvents(new DescendSign(), this);
        getServer().getPluginManager().registerEvents(new HelmSign(), this);
        getServer().getPluginManager().registerEvents(new MoveSign(), this);
        getServer().getPluginManager().registerEvents(new NameSign(), this);
        getServer().getPluginManager().registerEvents(new PilotSign(), this);
        getServer().getPluginManager().registerEvents(new RelativeMoveSign(), this);
        getServer().getPluginManager().registerEvents(new ReleaseSign(), this);
        getServer().getPluginManager().registerEvents(new RemoteSign(), this);
        getServer().getPluginManager().registerEvents(new SpeedSign(), this);
        getServer().getPluginManager().registerEvents(new SubcraftRotateSign(), this);
        getServer().getPluginManager().registerEvents(new TeleportSign(), this);
        getServer().getPluginManager().registerEvents(new ScuttleSign(), this);


        getCommand("contacts").setExecutor(new ContactsCommand());

        // Startup
        application = injector.build();
        application.host().startAll();
        logger.info("[V %s] has been enabled.".formatted(getDescription().getVersion()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;
        saveDefaultConfig();
    }

    private void registerServices(PluginBuilder injector){
        injector.registerInstance(getLogger());
        injector.registerInstance(this);
        injector.register(AsyncManager.class);
        injector.register(MapUpdateManager.class);
        injector.register(SmoothTeleportFactory.class);
        injector.register(WorldHandlerFactory.class);
        injector.registerInstance(WorldManager.INSTANCE);
        injector.register(WreckManager.class);
        injector.register(I18nSupport.class);
        injector.register(SettingsHostedService.class);
        injector.register(DataPackHostedService.class);
        injector.register(CraftManager.class);
        injector.register(InteractListener.class);
        injector.register(BlockListener.class);
        injector.register(PlayerListener.class);
        injector.register(ChunkManager.class);
        injector.register(CraftPilotListener.class);
        injector.register(CraftReleaseListener.class);
        injector.register(ContactsManager.class);
        injector.register(StatusManager.class);

        injector.register(StatusSign.class);
        injector.register(ContactsSign.class);

    }

    private record MovecraftAPI(
        @NotNull WorldHandler worldHandler,
        @NotNull SmoothTeleport smoothTeleport,
        @NotNull AsyncManager asyncManager,
        @NotNull WreckManager wreckManager) {}

    public WorldHandler getWorldHandler(){
        return application.container().getService(WorldHandler.class);
    }

    public SmoothTeleport getSmoothTeleport() {
        return application.container().getService(SmoothTeleport.class);
    }

    public AsyncManager getAsyncManager() {
        return application.container().getService(AsyncManager.class);
    }

    public @NotNull WreckManager getWreckManager(){
        return application.container().getService(WreckManager.class);
    }
}
