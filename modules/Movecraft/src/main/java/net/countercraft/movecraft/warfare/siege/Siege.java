package net.countercraft.movecraft.warfare.siege;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Siege {
    private final List<String> craftsToWin, commandsOnStart, commandsOnLose, commandsOnWin;
    private final int scheduleStart, scheduleEnd, delayBeforeStart, duration, dayOfWeek, dailyIncome, cost;
    private final String attackRegion, captureRegion, name;
    private AtomicReference<SiegeStage> stage;
    private int startTime, lastUpdate;
    private long lastPayout;
    private final boolean doubleCostPerOwnedSiegeRegion;
    private UUID playerUUID;

    public Siege(String name, String captureRegion, String attackRegion, int scheduleStart, int scheduleEnd, int delayBeforeStart, int duration, int dayOfWeek, int dailyIncome, int cost, boolean doubleCostPerOwnedSiegeRegion, List<String> craftsToWin, List<String> commandsOnStart, List<String> commandsOnWin, List<String> commandsOnLose) {
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
    }

    public AtomicReference<SiegeStage> getStage() {
        return stage;
    }

    public void setStage(AtomicReference<SiegeStage> stage) {
        this.stage = stage;
    }

    public List<String> getCraftsToWin() {
        return craftsToWin;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

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

    public List<String> getCommandsOnStart() {
        return commandsOnStart;
    }

    public List<String> getCommandsOnWin() {
        return commandsOnWin;
    }

    public List<String> getCommandsOnLose() {
        return commandsOnLose;
    }

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
}
