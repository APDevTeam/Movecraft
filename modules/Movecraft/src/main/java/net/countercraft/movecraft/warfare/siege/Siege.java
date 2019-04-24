package net.countercraft.movecraft.warfare.siege;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Siege {
    @NotNull
    private final List<String> craftsToWin, commandsOnStart, commandsOnLose, commandsOnWin;
    private final int scheduleStart, scheduleEnd, delayBeforeStart, duration, dayOfWeek, dailyIncome, cost;
    @NotNull private final String attackRegion, captureRegion, name;
    @NotNull private final AtomicReference<SiegeStage> stage;
    private int startTime, lastUpdate;
    private long lastPayout;
    private final boolean doubleCostPerOwnedSiegeRegion;
    private UUID playerUUID;
    private final Scoreboard scoreboard;

    public Siege(@NotNull String name, @NotNull String captureRegion, @NotNull String attackRegion, int scheduleStart, int scheduleEnd, int delayBeforeStart, int duration, int dayOfWeek, int dailyIncome, int cost, boolean doubleCostPerOwnedSiegeRegion, @NotNull List<String> craftsToWin, @NotNull List<String> commandsOnStart, @NotNull List<String> commandsOnWin, @NotNull List<String> commandsOnLose) {
        this.commandsOnWin = commandsOnWin;
        this.commandsOnLose = commandsOnLose;
        this.craftsToWin = craftsToWin;
        this.scheduleStart = scheduleStart;
        this.scheduleEnd = scheduleEnd;
        this.delayBeforeStart = delayBeforeStart;
        this.duration = duration;
        this.dayOfWeek = dayOfWeek;
        this.dailyIncome = dailyIncome;
        this.cost = cost;
        this.doubleCostPerOwnedSiegeRegion = doubleCostPerOwnedSiegeRegion;
        this.attackRegion = attackRegion;
        this.captureRegion = captureRegion;
        this.name = name;
        this.commandsOnStart = commandsOnStart;
        startTime = 0;
        lastUpdate = 0;
        stage = new AtomicReference<>();
        stage.set(SiegeStage.INACTIVE);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective(name, "dummy",name);
        scoreboard.registerNewObjective("stage","dummy","Stage: " + stage.get().name().toLowerCase().replace("_", " "));
        scoreboard.registerNewObjective("timeLeft","dummy","Time left: ");
        scoreboard.registerNewObjective("shipInRegion", "dummy", "");
        for (Objective objective : scoreboard.getObjectives()){
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    @NotNull
    public AtomicReference<SiegeStage> getStage() {
        return stage;
    }

    public void setStage(SiegeStage stage) {
        this.stage.set(stage);
    }

    @NotNull
    public List<String> getCraftsToWin() {
        return craftsToWin;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @NotNull
    public String getAttackRegion() {
        return attackRegion;
    }

    public int getDuration() {
        return duration;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    @NotNull
    public List<String> getCommandsOnStart() {
        return commandsOnStart;
    }

    @NotNull
    public List<String> getCommandsOnWin() {
        return commandsOnWin;
    }

    @NotNull
    public List<String> getCommandsOnLose() {
        return commandsOnLose;
    }

    @NotNull
    public String getCaptureRegion() {
        return captureRegion;
    }

    public int getCost() {
        return cost;
    }

    public long getLastPayout() {
        return lastPayout;
    }

    public void setLastPayout(long lastPayout) {
        this.lastPayout = lastPayout;
    }

    public int getDailyIncome() {
        return dailyIncome;
    }

    public int getScheduleEnd() {
        return scheduleEnd;
    }

    public int getScheduleStart() {
        return scheduleStart;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getDelayBeforeStart() {
        return delayBeforeStart;
    }

    public int getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(int lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isDoubleCostPerOwnedSiegeRegion() {
        return doubleCostPerOwnedSiegeRegion;
    }

    public Scoreboard getScoreboard(){
        return scoreboard;
    }
}
