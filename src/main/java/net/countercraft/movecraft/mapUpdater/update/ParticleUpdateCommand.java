package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.config.Settings;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.EnumParticle;
import net.minecraft.server.v1_11_R1.PacketPlayOutWorldParticles;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Random;

public class ParticleUpdateCommand implements UpdateCommand {
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
                    EntityPlayer craftPlayer = ((CraftPlayer) p).getHandle();
                    craftPlayer.playerConnection.sendPacket(new PacketPlayOutWorldParticles(
                            EnumParticle.VILLAGER_HAPPY, true,
                            (float) location.getX(),
                            (float) location.getY(),
                            (float) location.getZ(),
                            1,  1,  1, 0, 9));
                }
            }
            return true;
        }
        return false;
    }
}
