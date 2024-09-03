package net.countercraft.movecraft.lifecycle;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class WorkerHost implements HostedService {
    private final @NotNull Plugin plugin;
    private final @NotNull List<Worker> workers;
    private @NotNull List<BukkitTask> tasks;

    public WorkerHost(@NotNull Plugin plugin, @NotNull List<Worker> workers) {
        this.plugin = plugin;
        this.workers = workers;
        tasks = List.of();
    }

    @Override
    public void start() {
        tasks = workers.stream().map(worker -> {
          if(worker.isAsync()){
              return plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, worker::run, worker.getDelay(), worker.getPeriod());
          } else {
              return plugin.getServer().getScheduler().runTaskTimer(plugin, worker::run, worker.getDelay(), worker.getPeriod());
          }
        }).collect(Collectors.toList());
    }

    @Override
    public void stop() {
        var oldTasks = tasks;
        tasks = List.of();
        oldTasks.forEach(BukkitTask::cancel);
    }
}
