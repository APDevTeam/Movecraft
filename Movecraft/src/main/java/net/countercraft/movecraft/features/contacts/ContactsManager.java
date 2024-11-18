package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.datatag.CraftDataTagContainer;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.*;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.features.contacts.events.LostContactEvent;
import net.countercraft.movecraft.features.contacts.events.NewContactEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.*;

public class ContactsManager extends BukkitRunnable implements Listener {
    private static final CraftDataTagKey<Map<Craft, Long>> RECENT_CONTACTS = CraftDataTagRegistry.INSTANCE.registerTagKey(new NamespacedKey("movecraft", "recent-contacts"), craft -> new WeakHashMap<>());

    @Override
    public void run() {
        runContacts();
        runRecentContacts();
    }

    private void runContacts() {
        for (World w : Bukkit.getWorlds()) {
            if (w == null)
                continue;

            Set<Craft> craftsInWorld = CraftManager.getInstance().getCraftsInWorld(w);
            for (Craft base : craftsInWorld) {
                if (base instanceof SinkingCraft || base instanceof SubCraft)
                    continue;

                update(base, craftsInWorld);
            }
        }
    }

    private void update(@NotNull Craft base, @NotNull Set<Craft> craftsInWorld) {
        List<Craft> previousContacts = base.getDataTag(Craft.CONTACTS);
        if (previousContacts == null)
            previousContacts = new ArrayList<>(0);
        List<Craft> futureContacts = get(base, craftsInWorld);

        Set<Craft> newContacts = new HashSet<>(futureContacts);
        previousContacts.forEach(newContacts::remove);
        for (Craft target : newContacts) {
            NewContactEvent event = new NewContactEvent(base, target);
            Bukkit.getServer().getPluginManager().callEvent(event);
        }

        Set<Craft> oldContacts = new HashSet<>(previousContacts);
        futureContacts.forEach(oldContacts::remove);
        for (Craft target : oldContacts) {
            LostContactEvent event = new LostContactEvent(base, target);
            Bukkit.getServer().getPluginManager().callEvent(event);
        }

        base.setDataTag(Craft.CONTACTS, futureContacts);
    }

    private @NotNull List<Craft> get(Craft base, @NotNull Set<Craft> craftsInWorld) {
        Map<Craft, Integer> inRangeDistanceSquared = new HashMap<>();
        for (Craft target : craftsInWorld) {
            if (target instanceof SubCraft)
                continue;
            if (base instanceof PilotedCraft && target instanceof PilotedCraft
                    && ((PilotedCraft) base).getPilot() == ((PilotedCraft) target).getPilot())
                continue;

            MovecraftLocation baseCenter;
            MovecraftLocation targetCenter;
            try {
                baseCenter = base.getHitBox().getMidPoint();
                targetCenter = target.getHitBox().getMidPoint();
            }
            catch (EmptyHitBoxException e) {
                continue;
            }

            int distanceSquared = baseCenter.distanceSquared(targetCenter);
            double detectionMultiplier;
            if (targetCenter.getY() > 65) { // TODO: fix the water line
                detectionMultiplier = (double) target.getType().getPerWorldProperty(
                        CraftType.PER_WORLD_DETECTION_MULTIPLIER, target.getMovecraftWorld());
            }
            else {
                detectionMultiplier = (double) target.getType().getPerWorldProperty(
                        CraftType.PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER, target.getMovecraftWorld());
            }
            int detectionRange = (int) (target.getOrigBlockCount() * detectionMultiplier);
            detectionRange = detectionRange * 10;
            if (distanceSquared > detectionRange)
                continue;

            inRangeDistanceSquared.put(target, distanceSquared);
        }

        List<Craft> result = new ArrayList<>(inRangeDistanceSquared.keySet().size());
        result.addAll(inRangeDistanceSquared.keySet());
        result.sort(Comparator.comparingInt(inRangeDistanceSquared::get));
        return result;
    }

    private void runRecentContacts() {
        for (World w : Bukkit.getWorlds()) {
            if (w == null)
                continue;

            for (PlayerCraft base : CraftManager.getInstance().getPlayerCraftsInWorld(w)) {
                if (base.getHitBox().isEmpty())
                    continue;

                for (Craft target : base.getDataTag(Craft.CONTACTS)) {
                    // has the craft not been seen in the last minute?
                    if (System.currentTimeMillis() - base.getDataTag(RECENT_CONTACTS).getOrDefault(target, 0L) <= 60000)
                        continue;

                    Component message = contactMessage(false, base, target);
                    if (message == null)
                        continue;

                    base.getAudience().sendMessage(message);
                    base.getDataTag(RECENT_CONTACTS).put(target, System.currentTimeMillis());
                }
            }
        }
    }

    public static @Nullable Component contactMessage(boolean isNew, @NotNull Craft base, @NotNull Craft target) {
        MovecraftLocation baseCenter, targetCenter;
        try {
            baseCenter = base.getHitBox().getMidPoint();
            targetCenter = target.getHitBox().getMidPoint();
        }
        catch (EmptyHitBoxException e) {
            return null;
        }
        int distSquared = baseCenter.distanceSquared(targetCenter);

        Component notification = Component.empty();
        if (isNew) {
            notification = notification.append(I18nSupport.getInternationalisedComponent("Contact - New Contact").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        }
        else {
            notification = notification.append(I18nSupport.getInternationalisedComponent("Contact"));
        }
        notification = notification.append(Component.text( ": "));

        Component name = Component.empty();
        if (!target.getName().isEmpty()) {
            name = name.append(Component.text(target.getName() + " ("));
        }
        name = name.append(Component.text(target.getType().getStringProperty(CraftType.NAME)));
        if (!target.getName().isEmpty()) {
            name = name.append(Component.text(")"));
        }
        if (target instanceof SinkingCraft) {
            name = name.color(NamedTextColor.RED);
        }
        else if (target.getDisabled()) {
            name = name.color(NamedTextColor.BLUE);
        }
        notification = notification.append(name);

        notification = notification.append(Component.text(" "))
                .append(I18nSupport.getInternationalisedComponent("Contact - Commanded By"))
                .append(Component.text(" "));

        if (target instanceof PilotedCraft) {
            notification = notification.append(((PilotedCraft) target).getPilot().displayName());
        }
        else {
            notification = notification.append(Component.text("null"));
        }

        notification = notification.append(Component.text(", "))
                .append(I18nSupport.getInternationalisedComponent("Contact - Size"))
                .append(Component.text(": "))
                .append(Component.text(target.getOrigBlockCount()))
                .append(Component.text(", "))
                .append(I18nSupport.getInternationalisedComponent("Contact - Range"))
                .append(Component.text(": "))
                .append(Component.text((int) Math.sqrt(distSquared)))
                .append(Component.text(" "))
                .append(I18nSupport.getInternationalisedComponent("Contact - To The"))
                .append(Component.text(" "));

        BlockFace direction = getDirection(baseCenter, targetCenter);
        notification = notification.append(I18nSupport.getInternationalisedComponent("Contact - Direction - " + direction.name()));

        notification = notification.append(Component.text("."));
        return notification;
    }

    public static String getDirectionAppreviation(BlockFace face) {
        return DIRECTION_APPREVIATIONS[face.ordinal()];
    }

    static String[] DIRECTION_APPREVIATIONS = calcDirectionAppreviations();

    private static String[] calcDirectionAppreviations() {
        String[] result = new String[BlockFace.values().length];
        for (BlockFace face : BlockFace.values()) {
            String ident = face.name();
            String[] splitted = ident.split("_");
            String value = "";
            for (String s : splitted) {
                value = value + splitted[0];
            }
            result[face.ordinal()] = value;
        }
        return result;
    }

    static Map<Pair<Double, Double>, BlockFace> DIRECTION_MAPPING_4 = calcDirectionMapping4();
    static Map<Pair<Double, Double>, BlockFace> DIRECTION_MAPPING_8 = calcDirectionMapping8();
    static Map<Pair<Double, Double>, BlockFace> DIRECTION_MAPPING_16 = calcDirectionMapping16();

    static Map<Pair<Double, Double>, BlockFace> calcDirectionMapping4() {
        List<BlockFace> directionCompass = new ArrayList<>();
        directionCompass.add(BlockFace.NORTH);
        directionCompass.add(BlockFace.EAST);
        directionCompass.add(BlockFace.SOUTH);
        directionCompass.add(BlockFace.WEST);

        return calcDirectionMapping(directionCompass);
    }

    static Map<Pair<Double, Double>, BlockFace> calcDirectionMapping8() {
        List<BlockFace> directionCompass = new ArrayList<>();
        directionCompass.add(BlockFace.NORTH);
        directionCompass.add(BlockFace.NORTH_EAST);
        directionCompass.add(BlockFace.EAST);
        directionCompass.add(BlockFace.SOUTH_EAST);
        directionCompass.add(BlockFace.SOUTH);
        directionCompass.add(BlockFace.SOUTH_WEST);
        directionCompass.add(BlockFace.WEST);
        directionCompass.add(BlockFace.NORTH_WEST);

        return calcDirectionMapping(directionCompass);
    }

    static Map<Pair<Double, Double>, BlockFace> calcDirectionMapping16() {
        List<BlockFace> directionCompass = new ArrayList<>();
        directionCompass.add(BlockFace.NORTH);
        directionCompass.add(BlockFace.NORTH_NORTH_EAST);
        directionCompass.add(BlockFace.NORTH_EAST);
        directionCompass.add(BlockFace.EAST_NORTH_EAST);
        directionCompass.add(BlockFace.EAST);
        directionCompass.add(BlockFace.EAST_SOUTH_EAST);
        directionCompass.add(BlockFace.SOUTH_EAST);
        directionCompass.add(BlockFace.SOUTH_SOUTH_EAST);
        directionCompass.add(BlockFace.SOUTH);
        directionCompass.add(BlockFace.SOUTH_SOUTH_WEST);
        directionCompass.add(BlockFace.SOUTH_WEST);
        directionCompass.add(BlockFace.WEST_SOUTH_WEST);
        directionCompass.add(BlockFace.WEST);
        directionCompass.add(BlockFace.WEST_NORTH_WEST);
        directionCompass.add(BlockFace.NORTH_WEST);
        directionCompass.add(BlockFace.NORTH_NORTH_WEST);

        return calcDirectionMapping(directionCompass);
    }

    static Map<Pair<Double, Double>, BlockFace> calcDirectionMapping(List<BlockFace> directionCompass) {
        final double angleIncrement = (360.0 / directionCompass.size());
        final double halfAngleIncrement = angleIncrement / 2;
        double currentAngle = 0;

        final Map<Pair<Double, Double>, BlockFace> result = new HashMap<>();

        for(int i = 0; i < directionCompass.size(); i++) {
            BlockFace face = directionCompass.get(i);
            double borderleft = currentAngle - halfAngleIncrement;
            double borderRight = currentAngle + halfAngleIncrement;
            currentAngle += angleIncrement;
            result.put(new Pair<>(borderleft, borderRight), face);
        }

        return result;
    }


    static final double ROTATION_OFFSET = 0;

    public static BlockFace getDirection(MovecraftLocation self, MovecraftLocation other) {
        final MovecraftLocation distanceVector = other.subtract(self);
        final Vector2d vector = new Vector2d(distanceVector.getX(), distanceVector.getZ());
        final Vector2d vectorNormalized = vector.normalize();

        double angle = ROTATION_OFFSET + (Math.atan2(vectorNormalized.y(), vectorNormalized.x()) * 180 / Math.PI);
        if (angle < 0) {
            angle += 360;
        }

        final Map<Pair<Double, Double>, BlockFace> directionMap = grabDirectionMap(vectorNormalized.lengthSquared());

        for (Map.Entry<Pair<Double, Double>, BlockFace> entry : directionMap.entrySet()) {
            final Pair<Double, Double> filter = entry.getKey();
            if (filter.getLeft() <= angle && filter.getRight() >= angle) {
                return entry.getValue();
            }
        }
        return BlockFace.SELF;
    }

    // TODO: Make this dependant on the max settings for contacts and use percentages!
    static final double RANGE_FOR_16_DIRECTIONS = 512 * 512;
    static final double RANGE_FOR_8_DIRECTIONS = 1024 * 1024;

    static Map<Pair<Double, Double>, BlockFace> grabDirectionMap(double lengthSquared) {
        if (lengthSquared < RANGE_FOR_16_DIRECTIONS) {
            return DIRECTION_MAPPING_16;
        }
        if (lengthSquared < RANGE_FOR_8_DIRECTIONS) {
            return DIRECTION_MAPPING_8;
        }
        return DIRECTION_MAPPING_4;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(@NotNull CraftReleaseEvent e) {
        remove(e.getCraft());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftSink(@NotNull CraftSinkEvent e) {
        remove(e.getCraft());
    }

    private void remove(Craft base) {
        for (Craft other : CraftManager.getInstance().getCrafts()) {
            List<Craft> contacts = other.getDataTag(Craft.CONTACTS);
            if (contacts.contains(base))
                continue;

            contacts.remove(base);
            other.setDataTag(Craft.CONTACTS, contacts);
        }

        for (Craft other : CraftManager.getInstance().getCrafts()) {
            Map<Craft, Long> recentContacts = other.getDataTag(RECENT_CONTACTS);
            if (!recentContacts.containsKey(other))
                continue;

            recentContacts.remove(base);
            other.setDataTag(RECENT_CONTACTS, recentContacts);
        }
    }

    @EventHandler
    public void onNewContact(@NotNull NewContactEvent e) {
        Craft base = e.getCraft();
        Craft target = e.getTargetCraft();
        Component notification = contactMessage(true, base, target);
        if (notification != null)
            base.getAudience().sendMessage(notification);

        Object object = base.getType().getObjectProperty(CraftType.COLLISION_SOUND);
        if (!(object instanceof Sound sound))
            throw new IllegalStateException("COLLISION_SOUND must be of type Sound");
        base.getAudience().playSound(sound);

        if (base instanceof PlayerCraft) {
            Map<Craft, Long> recentContacts = base.getDataTag(RECENT_CONTACTS);
            recentContacts.put(target, System.currentTimeMillis());
            base.setDataTag(RECENT_CONTACTS, recentContacts);
        }
    }
}
