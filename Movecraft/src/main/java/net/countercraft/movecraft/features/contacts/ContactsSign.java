package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class ContactsSign implements Listener {
    private static final String HEADER = "Contacts:";
    private final ContactsManager contactsManager;

    public ContactsSign(ContactsManager contactsManager) {
        this.contactsManager = contactsManager;
    }

    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event) {
        World world = event.getCraft().getWorld();
        for (MovecraftLocation location : event.getCraft().getHitBox()) {
            var block = location.toBukkit(world).getBlock();
            if (!Tag.SIGNS.isTagged(block.getType()))
                continue;

            BlockState state = block.getState();
            if (!(state instanceof Sign sign))
                continue;

            if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER))
                continue;

            sign.setLine(1, "");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
        }
    }

    @EventHandler
    public final void onSignTranslateEvent(@NotNull SignTranslateEvent event) {
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        Craft base = event.getCraft();
        int line = 1;
        for (Craft target : contactsManager.get(base)) {
            if (line > 3)
                break;

            event.setLine(line++, contactsLine(base, target));
        }
    }

    private static @NotNull String contactsLine(@NotNull Craft base, @NotNull Craft target) {
        MovecraftLocation baseCenter = base.getHitBox().getMidPoint();
        MovecraftLocation targetCenter = target.getHitBox().getMidPoint();
        int distanceSquared = baseCenter.distanceSquared(targetCenter);

        String result = ChatColor.BLUE + target.getType().getStringProperty(CraftType.NAME);
        if (result.length() > 9)
            result = result.substring(0, 7);

        result += " " + (int) Math.sqrt(distanceSquared);
        int diffX = baseCenter.getX() - targetCenter.getX();
        int diffZ = baseCenter.getZ() - targetCenter.getZ();
        if (Math.abs(diffX) > Math.abs(diffZ)) {
            if (diffX<0) {
                result +=" E";
            } else {
                result +=" W";
            }
        } else {
            if (diffZ<0) {
                result +=" S";
            } else {
                result +=" N";
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClickEvent(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (!(block.getState() instanceof Sign sign))
            return;

        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        event.setCancelled(true);
        event.getPlayer().performCommand("contacts");
    }
}
