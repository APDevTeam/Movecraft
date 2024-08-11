package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.Nullable;

public class SpeedSign extends AbstractInformationSign {

    public SpeedSign() {
        super();
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Craft craft) {
        if (clickType != Action.RIGHT_CLICK_BLOCK)
            return false;

        final int gearShifts = craft.getType().getIntProperty(CraftType.GEAR_SHIFTS);
        int currentGear = craft.getCurrentGear();
        if (gearShifts == 1) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(I18nSupport.getInternationalisedString("Gearshift - Disabled for craft type")));
            return false;
        }
        currentGear++;
        if (currentGear > gearShifts)
            currentGear = 1;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(I18nSupport.getInternationalisedString("Gearshift - Gear changed")
                        + " " + currentGear + " / " + gearShifts));
        craft.setCurrentGear(currentGear);

        return true;
    }

    @Override
    protected @Nullable Component getUpdateString(int lineIndex, Component oldData, Craft craft) {
        // TODO: Display the gear somewhere?
        switch(lineIndex) {
            case 1:
                return Component.text(String.format("%.2f",craft.getSpeed()) + "m/s");
            case 2:
                return Component.text(String.format("%.2f",craft.getMeanCruiseTime() * 1000) + "ms");
            case 3:
                return Component.text(craft.getTickCooldown() + "T");
            default:
                return null;
        }
    }

    static final Component[] DEFAULT_VALUES = new Component[] {
            null,
            Component.text("0 m/s"),
            Component.text("0 ms"),
            Component.text("0 T")
    };

    @Override
    protected @Nullable Component getDefaultString(int lineIndex, Component oldComponent) {
        // TODO: Display the gear somewhere?
        return DEFAULT_VALUES[lineIndex];
    }

    @Override
    protected void performUpdate(Component[] newComponents, AbstractSignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {
        for (int i = 0; i < newComponents.length; i++) {
            Component newComp = newComponents[i];
            if (newComp != null) {
                sign.line(i, newComp);
            }
        }
        if (refreshCause != REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT) {
            sign.block().update(true);
        }
    }

    @Override
    protected void sendUpdatePacket(Craft craft, AbstractSignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {

    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        return;
    }

}
