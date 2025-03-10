package net.countercraft.movecraft;

import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import io.papermc.paper.datapack.DatapackManager;
import net.countercraft.movecraft.lifecycle.PluginBuilder;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
        var scheduler = new BukkitSchedulerMock();
        var dataPackManager = mock(DatapackManager.class);

        var server = mock(Server.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getDatapackManager()).thenReturn(dataPackManager);

        Path directory;
        try {
            directory = Files.createTempDirectory(UUID.randomUUID().toString());
            directory.resolve("localisation").toFile().mkdir();
            directory.resolve("localisation").resolve("movecraftlang_null.properties").toFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("movecraft-unit-test"));
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getConfig()).thenReturn(mock(FileConfiguration.class));

        // Create builder
        var builder = PluginBuilder.createFor(plugin);

        // Register plugin services
        Startup.registerServices(builder);

        // Build
        return builder.build();
    }
}
