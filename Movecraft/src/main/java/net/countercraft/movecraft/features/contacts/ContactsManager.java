package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.*;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.features.contacts.events.LostContactEvent;
import net.countercraft.movecraft.features.contacts.events.NewContactEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ContactsManager extends BukkitRunnable implements Listener {
    private final Map<Craft, List<Craft>> contactsMap = new WeakHashMap<>();
    private final Map<PlayerCraft, Map<Craft, Long>> recentContacts = new WeakHashMap<>();

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
            for (Craft craft : craftsInWorld) {
                update(craft, craftsInWorld);
            }
        }
    }

    private void update(Craft base, @NotNull Set<Craft> craftsInWorld) {
        List<Craft> previousContacts = contactsMap.get(base);
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

        contactsMap.put(base, futureContacts);
    }

    private @NotNull List<Craft> get(Craft base, @NotNull Set<Craft> craftsInWorld) {
        Map<Craft, Integer> inRangeDistanceSquared = new HashMap<>();
        for (Craft target : craftsInWorld) {
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
                        CraftType.PER_WORLD_DETECTION_MULTIPLIER, target.getWorld());
            }
            else {
                detectionMultiplier = (double) target.getType().getPerWorldProperty(
                        CraftType.PER_WORLD_UNDERWATER_DETECTION_MULTIPLIER, target.getWorld());
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

                if (!recentContacts.containsKey(base))
                    recentContacts.put(base, new WeakHashMap<>());

                for (Craft target : contactsMap.get(base)) {
                    // has the craft not been seen in the last minute?
                    if (System.currentTimeMillis() - recentContacts.get(base).getOrDefault(target, 0L) <= 60000)
                        continue;

                    Component message = contactMessage(false, base, target);
                    if (message == null)
                        continue;

                    base.getAudience().sendMessage(message);
                    recentContacts.get(base).put(target, System.currentTimeMillis());
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
        int diffX = baseCenter.getX() - targetCenter.getX();
        int diffZ = baseCenter.getZ() - targetCenter.getZ();
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

        if (Math.abs(diffX) > Math.abs(diffZ)) {
            if (diffX < 0) {
                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - East"));
            }
            else {
                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - West"));
            }
        }
        else {
            if (diffZ < 0) {
                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - South"));
            } else {
                notification = notification.append(I18nSupport.getInternationalisedComponent("Contact/Subcraft Rotate - North"));
            }
        }

        notification = notification.append(Component.text("."));
        return notification;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(@NotNull CraftReleaseEvent e) {
        Craft base = e.getCraft();
        contactsMap.remove(base);
        for (Craft key : contactsMap.keySet()) {
            if (!contactsMap.get(key).contains(base))
                continue;

            contactsMap.get(key).remove(base);
        }

        if (base instanceof PlayerCraft)
            recentContacts.remove(base);

        for (PlayerCraft key : recentContacts.keySet()) {
            if (!recentContacts.containsKey(key))
                continue;

            recentContacts.get(key).remove(base);
        }
    }

    public List<Craft> get(Craft base) {
        return contactsMap.get(base);
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

        if (base instanceof PlayerCraft playerCraft) {
            if (!recentContacts.containsKey(base))
                recentContacts.put(playerCraft, new WeakHashMap<>());

            recentContacts.get(playerCraft).put(target, System.currentTimeMillis());
        }
    }
}
