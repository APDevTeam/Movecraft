package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftScuttleEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ScuttleSign implements Listener {

    private static final String HEADER = "Scuttle";

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if(event.getClickedBlock() == null)
            return;

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }
        event.setCancelled(true);
        Craft craft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (craft == null) {
            if (!event.getPlayer().hasPermission("movecraft.commands.scuttle.others")) {
                event.getPlayer().sendMessage(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return;
            }
            craft = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(),
                    event.getClickedBlock().getLocation());
            if (craft == null)
                return;
        }
        scuttle(craft, event.getPlayer());
    }

    private void scuttle(Craft craft, CommandSender commandSender){
        if(craft instanceof SinkingCraft) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Scuttle - Craft Already Sinking"));
            return;
        }
        if(!commandSender.hasPermission("movecraft." + craft.getType().getStringProperty(CraftType.NAME)
                + ".scuttle")) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                    + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        CraftScuttleEvent e = new CraftScuttleEvent(craft, (Player) commandSender);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled())
            return;

        craft.setCruising(false);
        CraftManager.getInstance().sink(craft);
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX
                + I18nSupport.getInternationalisedString("Scuttle - Scuttle Activated"));
    }
}
