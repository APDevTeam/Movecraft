package net.countercraft.movecraft.mapUpdater.update;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PotionEffectsUpdateCommand extends UpdateCommand {

    private final Set<LivingEntity> livingEntities = new HashSet<>();
    private final Set<PotionEffect> potionEffects;
    public PotionEffectsUpdateCommand(Location loc, int effectRange, Set<PotionEffect> potionEffects){
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
            for (PotionEffect effect : potionEffects){

                entity.addPotionEffect(effect);
            }
        }
    }
}
