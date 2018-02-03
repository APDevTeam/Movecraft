package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.api.config.Settings;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Random;

public class ParticleUpdateCommand extends UpdateCommand {
    private Location location;
    private int smokeStrength;
    private Random rand = new Random();
    private static int silhouetteBlocksSent; //TODO: remove this

    public ParticleUpdateCommand(Location location, int smokeStrength) {
        this.location = location;
        this.smokeStrength = smokeStrength;
    }

    @Override
    public void doUpdate() {
        // put in smoke or effects
        if (smokeStrength == 1) {
            location.getWorld().playEffect(location, Effect.SMOKE, 4);
        }
        if (Settings.SilhouetteViewDistance > 0 && silhouetteBlocksSent < Settings.SilhouetteBlockCount) {
            if (sendSilhouetteToPlayers())
                silhouetteBlocksSent++;
        }

    }

    private boolean sendSilhouetteToPlayers() {
        if (rand.nextInt(100) < 15) {

            for (Player p : location.getWorld().getPlayers()) { // this is necessary because signs do not get updated client side correctly without refreshing the chunks, which causes a memory leak in the clients
                double distSquared = location.distanceSquared(p.getLocation());
                if ((distSquared < Settings.SilhouetteViewDistance * Settings.SilhouetteViewDistance) && (distSquared > 32 * 32)) {
                    p.spawnParticle(Particle.VILLAGER_HAPPY, location, 9);
                }
            }
            return true;
        }
        return false;
    }
}
