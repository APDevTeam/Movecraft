package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractCraftSign extends AbstractMovecraftSign {

    public static Optional<AbstractCraftSign> tryGetCraftSign(final String ident) {
        Optional<AbstractMovecraftSign> tmp = AbstractCraftSign.tryGet(ident);
        if (tmp.isPresent() && tmp.get() instanceof AbstractCraftSign acs) {
            return Optional.of(acs);
        }
        return Optional.empty();
    }

    private final boolean ignoreCraftIsBusy;

    public AbstractCraftSign(boolean ignoreCraftIsBusy) {
        this(null, ignoreCraftIsBusy);
    }

    public AbstractCraftSign(final String permission, boolean ignoreCraftIsBusy) {
        super(permission);
        this.ignoreCraftIsBusy = ignoreCraftIsBusy;
    }

    // Return true to cancel the event
    @Override
    public boolean processSignClick(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }
        Craft craft = this.getCraft(sign);
        if (craft == null) {
            this.onCraftNotFound(player, sign);
            return false;
        }

        if (craft instanceof PlayerCraft pc) {
            if (!pc.isNotProcessing() && !this.ignoreCraftIsBusy) {
                this.onCraftIsBusy(player, craft);
                return false;
            }
        }

        return internalProcessSign(clickType, sign, player, craft);
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @Nullable Craft craft) {
        if (craft == null) {
            throw new IllegalStateException("Somehow craft is not set here. It should always be present here!");
        }
        if (this.canPlayerUseSignOn(player, craft)) {
            return this.internalProcessSignWithCraft(clickType, sign, craft, player);
        }
        return false;
    }

    protected abstract void onCraftIsBusy(Player player, Craft craft);

    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (craft instanceof PilotedCraft pc) {
            return pc.getPilot() == player;
        }
        return true;
    }

    protected abstract void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign);

    public void onCraftDetect(CraftDetectEvent event, AbstractSignListener.SignWrapper sign) {
        // Do nothing by default
    }

    public void onSignMovedByCraft(SignTranslateEvent event) {
        // Do nothing by default
    }

    protected abstract boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player);

    // Gets called by internalProcessSign if a craft is found
    protected abstract boolean internalProcessSignWithCraft(Action clickType, AbstractSignListener.SignWrapper sign, Craft craft, Player player);

}
