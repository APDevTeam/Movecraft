package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NameSign extends AbstractCraftSign {

    public static final String NAME_SIGN_PERMISSION = "movecraft.name.place";

    public NameSign() {
        super(NAME_SIGN_PERMISSION, true);
    }

    @Override
    protected boolean canPlayerUseSign(Action clickType, SignListener.SignWrapper sign, Player player) {
        return !Settings.RequireNamePerm || super.canPlayerUseSign(clickType, sign, player);
    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, SignListener.SignWrapper sign, Player player, @Nullable Craft craft) {
        return true;
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        if (this.canPlayerUseSign(Action.RIGHT_CLICK_BLOCK, null, event.getPlayer())) {
            // Nothing to do
            return true;
        } else {
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
            return false;
        }
    }

    @Override
    public void onCraftDetect(CraftDetectEvent event, SignListener.SignWrapper sign) {
        Craft craft = event.getCraft();

        // TODO: Check if this craft can be named! If not, cancel the event!
        
        tryApplyName(craft, sign);
    }

    public static void tryApplyName(final Craft craft, final SignListener.SignWrapper sign) {
        if (craft != null && craft instanceof PilotedCraft pc) {
            if (pc.getPilot() == null) {
                return;
            }
            if (Settings.RequireNamePerm && !pc.getPilot().hasPermission(NAME_SIGN_PERMISSION))
                return;
        }

        if (sign.isEmpty()) {
            return;
        }

        boolean foundSome = false;
        Component name = Component.empty();
        for (int i = 0; i < sign.lines().size(); i++) {
            if (i == 0) {
                continue;
            }

            String raw = sign.getRaw(i);
            if (raw.isBlank()) {
                continue;
            }

            foundSome |= true;

            name = name.append(sign.line(i));
        }

        if (foundSome) {
            craft.setName(name);
        }
    }
}
