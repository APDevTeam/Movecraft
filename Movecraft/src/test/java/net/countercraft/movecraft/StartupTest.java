package net.countercraft.movecraft;

import net.countercraft.movecraft.lifecycle.PluginBuilder;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Test;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class StartupTest {
    @Test
    public void testInjection(){
        var host = buildTestApplication();
    }

    @Test
    public void testStartStop(){
        var host = buildTestApplication();

        host.host().startAll();
        host.host().stopAll();
    }

    private static PluginBuilder.Application buildTestApplication(){
        var pluginManager = mock(PluginManager.class);
        var scheduler = mock(BukkitScheduler.class);

        var server = mock(Server.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getPluginManager()).thenReturn(pluginManager);

        var plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("movecraft-unit-test"));
        when(plugin.getServer()).thenReturn(server);

        // Create builder
        var builder = PluginBuilder.createFor(plugin);

        // Register plugin services
        Startup.registerServices(builder);

        // Build
        return builder.build();
    }
}
