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

        if (args.length != 2) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("At least a base craft and a ignore craft must be given!")));
            return true;
        }

        Craft baseCraft;
        Craft ignoredCraft;

        try {
            UUID baseUUID = UUID.fromString(args[0]);
            UUID ignoreUUID = UUID.fromString(args[1]);

            baseCraft = Craft.getCraftByUUID(baseUUID);
            ignoredCraft = Craft.getCraftByUUID(ignoreUUID);
        } catch(IllegalArgumentException iae) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Argument 1 and 2 must be valid crafts!")));
            return true;
        }

        if (baseCraft == null || ignoredCraft == null) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Argument 1 and 2 must be valid crafts!")));
            return true;
        }

        PlayerCraft playerCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if (playerCraft == null || !playerCraft.getUUID().equals(baseCraft.getUUID())) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("You cant modify the ignore list of a craft that is not yours!")));
            return true;
        }

        baseCraft.getDataTag(ContactsManager.IGNORED_CRAFTS).add(ignoredCraft.getUUID());

        return true;
    }
}
