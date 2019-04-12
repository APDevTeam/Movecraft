package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PotionEffectsUpdateCommand extends UpdateCommand {

    private final Set<LivingEntity> livingEntities = new HashSet<>();
    private final Map<PotionEffect, Integer> potionEffects;
    public PotionEffectsUpdateCommand(Location loc, int effectRange, Map<PotionEffect,Integer> potionEffects){
        this.potionEffects = potionEffects;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc,effectRange,effectRange,effectRange)){
            if (!(entity instanceof LivingEntity)){
                continue;
            }
            livingEntities.add((LivingEntity) entity);
        }
    }
    @Override
    public void doUpdate() {
        for (LivingEntity entity : livingEntities){
            for (PotionEffect effect : potionEffects.keySet()){
                new BukkitRunnable() {
                    int timePassed = 0;
                    final int delay = potionEffects.get(effect);
                    @Override
                    public void run() {
                        timePassed++;
                        if (timePassed < delay)
                            return;
                        entity.addPotionEffect(effect);

                        cancel();
                    }
                }.runTaskTimer(Movecraft.getInstance(),0,20);

            }
        }
    }
}
