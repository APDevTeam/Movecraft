package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCruiseSign extends AbstractCraftSign {

    private final String suffixOn;
    private final String suffixOff;
    private final String ident = AbstractMovecraftSign.findIdent(this);

    public AbstractCruiseSign(boolean ignoreCraftIsBusy, final String suffixOn, final String suffixOff) {
        this(null, ignoreCraftIsBusy, suffixOn, suffixOff);
    }

    public AbstractCruiseSign(final String permission, boolean ignoreCraftIsBusy, final String suffixOn, final String suffixOff) {
        super(permission, ignoreCraftIsBusy);
        this.suffixOn = suffixOn;
        this.suffixOff = suffixOff;
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (PlainTextComponentSerializer.plainText().serialize(sign.line(0)).isBlank()) {
            return false;
        }
        String[] headerSplit = this.getSplitHeader(sign);
        if (headerSplit.length != 2) {
            return false;
        }
        String suffix = headerSplit[1];
        return suffix.equalsIgnoreCase(this.suffixOff) || suffix.equalsIgnoreCase(this.suffixOn);
    }

    protected String[] getSplitHeader(final AbstractSignListener.SignWrapper sign) {
        String header = PlainTextComponentSerializer.plainText().serialize(sign.line(0));
        if (header.isBlank()) {
            return null;
        }
        return header.split(":");
    }

    protected boolean isOnOrOff(AbstractSignListener.SignWrapper sign) {
        String[] headerSplit = this.getSplitHeader(sign);
        if (headerSplit.length != 2) {
            return false;
        }
        String suffix = headerSplit[1];
        return suffix.equalsIgnoreCase(this.suffixOn);
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        return false;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Craft craft) {
        boolean isOn = this.isOnOrOff(sign);
        boolean willBeOn = !isOn;
        if (willBeOn) {
            CruiseDirection direction = this.getCruiseDirection(sign);
            this.setCraftCruising(player, direction);
        } else {
           craft.setCruising(false);
        }

        // Update sign
        sign.line(0, buildHeader(willBeOn));
        sign.block().update(true);
        // TODO: What to replace this with?
        craft.resetSigns(sign.block());

        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        return true;
    }

    @Override
    public void onCraftDetect(CraftDetectEvent event, AbstractSignListener.SignWrapper sign) {
        Player p = null;
        if (event.getCraft() instanceof PilotedCraft pc) {
            p = pc.getPilot();
        }

        if (this.isSignValid(Action.PHYSICAL, sign, p)) {
            sign.line(0, buildHeader(false));
        } else {
            // TODO: Error? React in any way?
            sign.line(0, buildHeader(false));
        }
    }

    protected Component buildHeader(boolean on) {
        return on ? buildHeaderOn() : buildHeaderOff();
    }

    protected Component buildHeaderOn() {
        return Component.text(this.ident).append(Component.text(": ")).append(Component.text(this.suffixOn, Style.style(TextColor.color(0, 255, 0))));
    }

    protected Component buildHeaderOff() {
        return Component.text(this.ident).append(Component.text(": ")).append(Component.text(this.suffixOff, Style.style(TextColor.color(255, 0, 0))));
    }

    protected abstract void setCraftCruising(Player player, CruiseDirection direction);
    // TODO: Rework cruise direction to vectors => Vector defines the skip distance and the direction
    protected abstract CruiseDirection getCruiseDirection(AbstractSignListener.SignWrapper sign);
}
