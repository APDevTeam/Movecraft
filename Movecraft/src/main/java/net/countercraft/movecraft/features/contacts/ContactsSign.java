package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.sign.AbstractInformationSign;
import net.countercraft.movecraft.sign.SignListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ContactsSign extends AbstractInformationSign {

    protected final int MAX_DISTANCE_COLOR_RED = 64 * 64;
    protected final int MAX_DISTANCE_COLOR_YELLOW = 128 * 128;

    protected @NotNull Component contactsLine(@NotNull Craft base, @NotNull Craft target) {
        if (target instanceof ContactProvider contactProvider) {
            Component provided = contactProvider.getContactsLine(base);
            if (provided != null) {
                return provided;
            }
        }

        MovecraftLocation baseCenter = base.getHitBox().getMidPoint();
        MovecraftLocation targetCenter = target.getHitBox().getMidPoint();
        int distanceSquared = baseCenter.distanceSquared(targetCenter);

        String craftTypeName = target.getCraftProperties().getName();

        if (target.getName() != null && !target.getNameRaw().isBlank()) {
            craftTypeName = target.getNameRaw();
        }
        // TODO: Why do we check if it is greater than 9 and then limit it to 8 characters?
        if (craftTypeName.length() > 9)
            craftTypeName = craftTypeName.substring(0, 7);

        Style style = STYLE_COLOR_GREEN;
        if (distanceSquared <= MAX_DISTANCE_COLOR_RED) {
            style = STYLE_COLOR_RED;
        }
        else if (distanceSquared <= MAX_DISTANCE_COLOR_YELLOW) {
            style = STYLE_COLOR_YELLOW;
        }

        Component result = Component.text(craftTypeName + " ").style(style);

        double distance = Math.sqrt(distanceSquared);
        distance = distance / 1000;

        String directionStr = "" + String.format("%.2f", distance);
        final BlockFace direction = ContactsManager.getDirection(baseCenter, targetCenter);
        directionStr = directionStr + " " + ContactsManager.getDirectionAppreviation(direction);
        result = result.append(Component.text(directionStr).style(STYLE_COLOR_WHITE));
        String tmp = PlainTextComponentSerializer.plainText().serialize(result);
        return result;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        player.performCommand("contacts");

        return super.internalProcessSignWithCraft(clickType, sign, craft, player);
    }

    @Override
    protected @Nullable Component getUpdateString(int lineIndex, Component oldData, Craft craft) {
        Craft contact = null;
        List<UUID> contacts = craft.getDataTag(Craft.CONTACTS);

        int contactIndex = lineIndex - 1;

        if (contacts.isEmpty() || contacts.size() <= contactIndex) {
            return EMPTY;
        }
        contact = Craft.getCraftByUUID(contacts.get(contactIndex));
        if (contact == null) {
            return Component.empty();
        }

        return contactsLine(craft, contact);
    }

    @Override
    protected @Nullable Component getDefaultString(int lineIndex, Component oldComponent) {
        return EMPTY;
    }

    @Override
    protected void performUpdate(Component[] newComponents, SignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {
        for (int i = 0; i < newComponents.length; i++) {
            Component newComp = newComponents[i];
            if (newComp != null) {
                sign.line(i, newComp);
            }
        }
        if (refreshCause != REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT && sign.block() != null) {
            sign.block().update();
        }
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }
}
