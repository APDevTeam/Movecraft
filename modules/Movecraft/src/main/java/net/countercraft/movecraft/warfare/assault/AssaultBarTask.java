package net.countercraft.movecraft.warfare.assault;

import net.countercraft.movecraft.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;


public class AssaultBarTask extends BukkitRunnable {
    private static boolean assaultInPreparation = false;
    private static long startTime;
    private static Plugin movecraft;
    private static BossBar prepBar;
    private static String assaultName;
    public AssaultBarTask(Plugin movecraft,String assaultName, long startTime){
        this.assaultInPreparation = true;
        this.startTime = startTime;
        this.movecraft = movecraft;
        this.assaultName = assaultName;
        this.prepBar = Bukkit.createBossBar(assaultName, BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
        this.prepBar.setProgress(0.0);
    }

    @Override
    public void run() {
        long timePassed = (System.currentTimeMillis() - startTime) / 1000;
        assaultInPreparation = timePassed <= Settings.AssaultDelay;
        double progress = (double) timePassed / (double) Settings.AssaultDelay;
        for (Player p : Bukkit.getOnlinePlayers()){
            prepBar.addPlayer(p);
        }
        //Bukkit.broadcastMessage(String.valueOf(startTime) + ", " + String.valueOf(System.currentTimeMillis()) + ", " + String.valueOf(progress) + ", " + String.valueOf(timePassed));
        if (assaultInPreparation){
            prepBar.setVisible(true);
            if (progress >= 0.0 && progress <= 1.0){
                prepBar.setProgress(progress);

            }
        } else {
            prepBar.setVisible(false);
            this.cancel();
        }
    }
}
