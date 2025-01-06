package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IgnoreContactCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("ignorecontact"))
            return false;

        if(!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.errorPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Contacts - Must Be Player")));
            return true;
        }

        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if (playerCraft == null) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("You must be piloting a craft")));
            return true;
        }

        // TODO: Run this async
        for (String arg : args) {
            try {
                UUID ignoredUUID = UUID.fromString(arg);
                if (Craft.getCraftByUUID(ignoredUUID) == null) {
                    continue;
                }

                playerCraft.getDataTag(ContactsManager.IGNORED_CRAFTS).add(ignoredUUID);
            } catch(IllegalArgumentException iae) {
                continue;
            }
        }

        return true;
    }
}
