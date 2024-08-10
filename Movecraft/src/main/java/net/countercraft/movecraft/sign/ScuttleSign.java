package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftScuttleEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ScuttleSign extends AbstractCraftSign {

    public ScuttleSign() {
        super("movecraft.commands.scuttle.others", true);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {
        player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("You must be piloting a craft"));
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        if (processingSuccessful) {
            return true;
        }
        return !sneaking;
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        return false;
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        if(craft instanceof SinkingCraft) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Scuttle - Craft Already Sinking"));
            return false;
        }
        if(!player.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME)
                + ".scuttle")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return false;
        }
        if (craft instanceof PilotedCraft pc) {
            if (player == pc.getPilot()) {
                return true;
            }
        }
        // Check for "can scuttle others" permission
        if (this.optPermission.isPresent()) {
            if (!player.hasPermission(this.optPermission.get())) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            }
        }
        return true;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Craft craft) {
        CraftScuttleEvent e = new CraftScuttleEvent(craft, player);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled())
            return false;

        craft.setCruising(false);
        CraftManager.getInstance().sink(craft);
        player.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Scuttle - Scuttle Activated"));
        return true;
    }
}
