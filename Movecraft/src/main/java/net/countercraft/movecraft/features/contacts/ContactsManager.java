package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.*;
import net.countercraft.movecraft.exception.EmptyHitBoxException;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ContactsManager extends BukkitRunnable implements Listener {
    private final Map<Craft, List<Craft>> contactsMap = new WeakHashMap<>();
    private final Map<PlayerCraft, Map<Craft, Long>> recentContacts = new WeakHashMap<>();

    @Override
    public void run() {
        runRecentContacts();
    }

    private void runRecentContacts() {
        for (World w : Bukkit.getWorlds()) {
            if (w == null)
                continue;

            for (PlayerCraft craft : CraftManager.getInstance().getPlayerCraftsInWorld(w)) {
                if (craft.getHitBox().isEmpty())
                    continue;

                if (!recentContacts.containsKey(craft))
                    recentContacts.put(craft, new WeakHashMap<>());

                for (Craft target : contactsMap.get(craft)) {
                    // has the craft not been seen in the last minute?
                    if (System.currentTimeMillis() - recentContacts.get(craft).getOrDefault(target, 0L) <= 60000)
                        continue;

                    Component message = contactMessage(false, craft, target);
                    if (message == null)
                        continue;

                    craft.getAudience().sendMessage(message);
                    recentContacts.get(craft).put(target, System.currentTimeMillis());
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

        Component notification;
        if (isNew) {
            notification = I18nSupport.getInternationalisedComponent("Contact - New Contact").style(Style.style(NamedTextColor.RED, TextDecoration.BOLD));
        }
        else {
            notification = I18nSupport.getInternationalisedComponent("Contact");
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

    private void update(Craft c) {
        // TODO
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(@NotNull CraftReleaseEvent e) {
        Craft craft = e.getCraft();
        contactsMap.remove(craft);
        for (Craft key : contactsMap.keySet()) {
            if (!contactsMap.get(key).contains(craft))
                continue;

            contactsMap.get(key).remove(craft);
        }

        if (craft instanceof PlayerCraft)
            recentContacts.remove(craft);

        for (PlayerCraft key : recentContacts.keySet()) {
            recentContacts.get(key).remove(craft);
        }
    }

    public List<Craft> get(Craft craft) {
        return contactsMap.get(craft);
    }
}
