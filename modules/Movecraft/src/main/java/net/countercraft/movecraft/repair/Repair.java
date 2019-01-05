package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class Repair {
    private String name;
    private Craft craft;
    private LinkedList<UpdateCommand> updateCommands, fragileBlockUpdateCommands;
    private UUID playerUUID;
    private long missingBlocks, durationInTicks, ticksSinceStart;
    private BossBar progressBar;
    private Location signLoc;
    private AtomicBoolean running = new AtomicBoolean(true);
    public Repair(String name, Craft craft, LinkedList<UpdateCommand> updateCommands, LinkedList<UpdateCommand> fragileBlockUpdateCommands,  UUID playerUUID, long missingBlocks, Location signLoc){
        this.name = name;
        this.craft = craft;
        this.updateCommands = updateCommands;
        this.fragileBlockUpdateCommands = fragileBlockUpdateCommands;
        this.durationInTicks = missingBlocks * Settings.RepairTicksPerBlock;
        this.ticksSinceStart = 0;
        this.missingBlocks = missingBlocks;
        this.playerUUID = playerUUID;
        this.signLoc = signLoc;
        progressBar = Bukkit.createBossBar(this.name, BarColor.WHITE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        progressBar.setVisible(true);
        progressBar.setProgress(0.0);
    }
    public String getName(){
        return name;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMissingBlocks() {
        return missingBlocks;
    }

    public void setMissingBlocks(int missingBlocks) {
        this.missingBlocks = missingBlocks;
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public void setRunning(AtomicBoolean running) {
        this.running = running;
    }

    public long getDurationInTicks() {
        return durationInTicks;
    }

    public void setDurationInTicks(int durationInTicks) {
        this.durationInTicks = durationInTicks;
    }

    public Location getSignLoc() {
        return signLoc;
    }

    public void setSignLoc(Location signLoc) {
        this.signLoc = signLoc;
    }

    public Craft getCraft() {
        return craft;
    }

    public BossBar getProgressBar() {
        return progressBar;
    }

    public LinkedList<UpdateCommand> getUpdateCommands() {
        return updateCommands;
    }

    public long getTicksSinceStart() {
        return ticksSinceStart;
    }

    public void setTicksSinceStart(long ticksSinceStart) {
        this.ticksSinceStart = ticksSinceStart;
    }

    public LinkedList<UpdateCommand> getFragileBlockUpdateCommands() {
        return fragileBlockUpdateCommands;
    }

}
