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

public abstract class AbstractToggleSign extends AbstractCraftSign {

    private final String suffixOn;
    private final String suffixOff;
    private final String ident;
    private final Component headerOn;
    private final Component headerOff;

    public AbstractToggleSign(boolean ignoreCraftIsBusy, final String ident, final String suffixOn, final String suffixOff) {
        this(null, ignoreCraftIsBusy, ident, suffixOn, suffixOff);
    }

    public AbstractToggleSign(final String permission, boolean ignoreCraftIsBusy, final String ident,  final String suffixOn, final String suffixOff) {
        super(permission, ignoreCraftIsBusy);
        this.suffixOn = suffixOn;
        this.suffixOff = suffixOff;
        this.ident = ident;

        this.headerOn = this.buildHeaderOn();
        this.headerOff = this.buildHeaderOff();
    }

    // Checks if the header is empty, if yes, it quits early (unnecessary actually as if it was empty this would never be called)
    // Afterwards the header is validated, if it's splitted variant doesn't have exactly 2 entries it is invalid
    // Finally, the "state" (second part of the header) isn't matching suffixOn or suffixOff, it is invalid
    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        if (PlainTextComponentSerializer.plainText().serialize(sign.line(0)).isBlank()) {
            return false;
        }
        String[] headerSplit = getSplitHeader(sign);
        if (headerSplit.length != 2) {
            return false;
        }
        String suffix = headerSplit[1].trim();
        return suffix.equalsIgnoreCase(this.suffixOff) || suffix.equalsIgnoreCase(this.suffixOn);
    }

    // Returns the raw header, which should consist of the ident and either the suffixOn or suffixOff value
    // Returns null if the header is blank
    @Nullable
    protected static String[] getSplitHeader(final SignListener.SignWrapper sign) {
        String header = PlainTextComponentSerializer.plainText().serialize(sign.line(0));
        if (header.isBlank()) {
            return null;
        }
        return header.split(":");
    }

    // If the suffix matches the suffixOn field it will returnt true
    // calls getSplitHeader() to retrieve the raw header string
    protected boolean isOnOrOff(SignListener.SignWrapper sign) {
        String[] headerSplit = getSplitHeader(sign);
        if (headerSplit == null || headerSplit.length != 2) {
            return false;
        }
        String suffix = headerSplit[1].trim();
        return suffix.equalsIgnoreCase(this.suffixOn);
    }

    protected abstract void onAfterToggle(Craft craft, SignListener.SignWrapper signWrapper, Player player, boolean toggledToOn);
    protected abstract void onBeforeToggle(Craft craft, SignListener.SignWrapper signWrapper, Player player, boolean willBeOn);

    // Actual processing, determines wether the sign will switch to on or off
    // If it will be on, the CruiseDirection is retrieved and then setCraftCruising() is called
    // Otherwise, the craft will stop cruising
    // Then the sign is updated and the block resetted
    // Finally, the relevant hooks are called
    // This always returns true
    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        boolean isOn = this.isOnOrOff(sign);
        boolean willBeOn = !isOn;

        this.onBeforeToggle(craft, sign, player, willBeOn);

        // Update sign
        sign.line(0, buildHeader(willBeOn));
        sign.block().update(true);
        craft.resetSigns(sign.block());

        this.onAfterToggle(craft, sign, player, willBeOn);

        return true;
    }

    // On sign placement, if the entered header is the same as our ident, it will append the off-suffix automatically
    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        String header = sign.getRaw(0).trim();
        if (header.equalsIgnoreCase(this.ident)) {
            sign.line(0, buildHeaderOff());
        }
        return true;
    }

    // On craft detection, we set all the headers to the "off" header
    @Override
    public void onCraftDetect(CraftDetectEvent event, SignListener.SignWrapper sign) {
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

    // Helper method to build the headline for on or off state
    protected Component buildHeader(boolean on) {
        return on ? this.headerOn : this.headerOff;
    }

    protected Component buildHeaderOn() {
        return Component.text(this.ident).append(this.ident.endsWith(":") ? Component.text(" ") : Component.text(": ")).append(Component.text(this.suffixOn, Style.style(TextColor.color(0, 255, 0))));
    }

    protected Component buildHeaderOff() {
        return Component.text(this.ident).append(this.ident.endsWith(":") ? Component.text(" ") : Component.text(": ")).append(Component.text(this.suffixOff, Style.style(TextColor.color(255, 0, 0))));
    }

}