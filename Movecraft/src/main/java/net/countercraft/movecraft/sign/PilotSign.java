package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// TODO: Replace PilotSignValidator with this?
public class PilotSign extends AbstractMovecraftSign {

    public PilotSign() {
        super(null);
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        return processingSuccessful || !sneaking;
    }

    // Pilot signs are pretty much always valid
    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Optional<Craft> craft) {
        // Nothing to do here
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        boolean foundSome = false;
        for (int i = 1; i < sign.lines().size(); i++) {
            String data = null;
            try {
                data = sign.getRaw(i);
            } catch (IndexOutOfBoundsException ioob) {
                // Ignore
            }
            if (data != null) {
                foundSome = !data.isBlank();
                if (foundSome) {
                    break;
                }
            }
        }
        if (!foundSome) {
            sign.line(1, event.getPlayer().name());
        }
        return true;
    }
}
