package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface ContactProvider {


    public Component getDetectedMessage(boolean isNew, Craft detectingCraft);

    public boolean contactPickedUpBy(Craft other);

    public MovecraftLocation getContactLocation();

    public double getDetectionMultiplier(boolean overWaterLine, MovecraftWorld world);

    public default @NotNull List<UUID> getContactUUIDs(Craft self, @NotNull Set<Craft> candidates) {
        Map<Craft, Integer> inRangeDistanceSquared = new HashMap<>();
        for (Craft target : candidates) {
            if (target instanceof SubCraft)
                continue;
            if (self instanceof PilotedCraft && target instanceof PilotedCraft
                    && ((PilotedCraft) self).getPilot() == ((PilotedCraft) target).getPilot())
                continue;

            MovecraftLocation baseCenter;
            MovecraftLocation targetCenter;
            try {
                baseCenter = self.getHitBox().getMidPoint();
                targetCenter = target.getHitBox().getMidPoint();
            } catch (EmptyHitBoxException e) {
                continue;
            }

            int distanceSquared = baseCenter.distanceSquared(targetCenter);
            double detectionMultiplier;
            if (targetCenter.getY() > 65) { // TODO: fix the water line
                detectionMultiplier = (double) target.getType().getPerWorldProperty(
                        CraftType.PER_WORLD_DETECTION_MULTIPLIER, target.getMovecraftWorld());
            } else {
                detectionMultiplier = (double) target.getType().getPerWorldProperty(
                        CraftType.PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER, target.getMovecraftWorld());
            }
            int detectionRange = (int) (target.getOrigBlockCount() * detectionMultiplier);
            detectionRange = detectionRange * 10;
            if (distanceSquared > detectionRange)
                continue;

            inRangeDistanceSquared.put(target, distanceSquared);
        }

        List<UUID> result = new ArrayList<>(inRangeDistanceSquared.keySet().size());
        inRangeDistanceSquared.keySet().forEach(c -> result.add(c.getUUID()));
        result.sort(Comparator.comparingInt(inRangeDistanceSquared::get));
        return result;
    }

}
