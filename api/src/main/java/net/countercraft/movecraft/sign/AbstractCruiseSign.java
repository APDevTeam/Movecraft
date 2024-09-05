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

/*
 * Base class for all cruise signs
 *
 * Has the relevant logic for the "state" suffix (on / off) as well as calling the relevant methods and setting the craft to cruising
 *
 */
public abstract class AbstractCruiseSign extends AbstractCraftSign {

    private final String suffixOn;
    private final String suffixOff;
    private final String ident;
    private final Component headerOn = this.buildHeaderOn();
    private final Component headerOff = this.buildHeaderOff();

    public AbstractCruiseSign(boolean ignoreCraftIsBusy, final String ident, final String suffixOn, final String suffixOff) {
        this(null, ignoreCraftIsBusy, ident, suffixOn, suffixOff);
    }

    public AbstractCruiseSign(final String permission, boolean ignoreCraftIsBusy, final String ident,  final String suffixOn, final String suffixOff) {
        super(permission, ignoreCraftIsBusy);
        this.suffixOn = suffixOn;
        this.suffixOff = suffixOff;
        this.ident = ident;
    }

    // Checks if the header is empty, if yes, it quits early (unnecessary actually as if it was empty this would never be called)
    // Afterwards the header is validated, if it's splitted variant doesn't have exactly 2 entries it is invalid
    // Finally, the "state" (second part of the header) isn't matching suffixOn or suffixOff, it is invalid
    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
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
    protected static String[] getSplitHeader(final AbstractSignListener.SignWrapper sign) {
        String header = PlainTextComponentSerializer.plainText().serialize(sign.line(0));
        if (header.isBlank()) {
            return null;
        }
        return header.split(":");
    }

    // If the suffix matches the suffixOn field it will returnt true
    // calls getSplitHeader() to retrieve the raw header string
    protected boolean isOnOrOff(AbstractSignListener.SignWrapper sign) {
        String[] headerSplit = getSplitHeader(sign);
        if (headerSplit == null || headerSplit.length != 2) {
            return false;
        }
        String suffix = headerSplit[1].trim();
        return suffix.equalsIgnoreCase(this.suffixOn);
    }

    // By default, cancel the event if the processing was successful, or the invoker was not sneaking => Allows breaking signs while sneaking
    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        return processingSuccessful || !sneaking;
    }

    // Hook to do stuff that run after stopping to cruise
    protected void onAfterStoppingCruise(Craft craft, AbstractSignListener.SignWrapper signWrapper, Player player) {

    }

    // Hook to do stuff that run after starting to cruise
    protected void onAfterStartingCruise(Craft craft, AbstractSignListener.SignWrapper signWrapper, Player player) {

    }

    // Actual processing, determines wether the sign will switch to on or off
    // If it will be on, the CruiseDirection is retrieved and then setCraftCruising() is called
    // Otherwise, the craft will stop cruising
    // Then the sign is updated and the block resetted
    // Finally, the relevant hooks are called
    // This always returns true
    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, AbstractSignListener.SignWrapper sign, Craft craft, Player player) {
        boolean isOn = this.isOnOrOff(sign);
        boolean willBeOn = !isOn;
        if (willBeOn) {
            CruiseDirection direction = this.getCruiseDirection(sign);
            this.setCraftCruising(player, direction, craft);
        } else {
           craft.setCruising(false);
        }

        // Update sign
        sign.line(0, buildHeader(willBeOn));
        sign.block().update(true);
        // TODO: What to replace this with?
        craft.resetSigns(sign.block());

        if (willBeOn) {
            this.onAfterStartingCruise(craft, sign, player);
        } else {
            this.onAfterStoppingCruise(craft, sign, player);
        }

        return true;
    }

    // On sign placement, if the entered header is the same as our ident, it will append the off-suffix automatically
    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        String header = sign.getRaw(0).trim();
        if (header.equalsIgnoreCase(this.ident)) {
            sign.line(0, buildHeaderOff());
        }
        return true;
    }

    // On craft detection, we set all the headers to the "off" header
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

    // Helper method to build the headline for on or off state
    protected Component buildHeader(boolean on) {
        return on ? this.headerOn : this.headerOff;
    }

    protected Component buildHeaderOn() {
        return Component.text(this.ident).append(this.ident.endsWith(":") ? Component.empty() : Component.text(": ")).append(Component.text(this.suffixOn, Style.style(TextColor.color(0, 255, 0))));
    }

    protected Component buildHeaderOff() {
        return Component.text(this.ident).append(this.ident.endsWith(":") ? Component.empty() : Component.text(": ")).append(Component.text(this.suffixOff, Style.style(TextColor.color(255, 0, 0))));
    }

    // Should call the craft's relevant methods to start cruising
    protected abstract void setCraftCruising(Player player, CruiseDirection direction, Craft craft);

    // TODO: Rework cruise direction to vectors => Vector defines the skip distance and the direction
    // Returns the direction in which the craft should cruise
    protected abstract CruiseDirection getCruiseDirection(AbstractSignListener.SignWrapper sign);
}
