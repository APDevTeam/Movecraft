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

public final class Repair {
    private final String name;
    private final Craft craft;
    private final LinkedList<UpdateCommand> updateCommands, fragileBlockUpdateCommands;
    private final UUID playerUUID;
    private final long missingBlocks, durationInTicks;
    private long ticksSinceStart;
    private final  BossBar progressBar;
    private final Location signLoc;
    private final AtomicBoolean running = new AtomicBoolean(true);
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

    public long getMissingBlocks() {
        return missingBlocks;
    }

    public AtomicBoolean getRunning() {
        return running;
    }

    public long getDurationInTicks() {
        return durationInTicks;
    }

    public Location getSignLoc() {
        return signLoc;
    }

    public void setTicksSinceStart(long ticksSinceStart){
        this.ticksSinceStart = ticksSinceStart;
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

    public LinkedList<UpdateCommand> getFragileBlockUpdateCommands() {
        return fragileBlockUpdateCommands;
    }

}
