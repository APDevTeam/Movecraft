package net.countercraft.movecraft.lifecycle;

import jakarta.inject.Inject;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListenerLifecycleService implements Service {
    private final @NotNull List<Listener> listeners;
    private final @NotNull Plugin plugin;

    @Inject
    public ListenerLifecycleService(@NotNull List<Listener> listeners, @NotNull Plugin plugin){
        this.listeners = listeners;
        this.plugin = plugin;
    }

    @Override
    public void start() {
        listeners.forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
    }
}
