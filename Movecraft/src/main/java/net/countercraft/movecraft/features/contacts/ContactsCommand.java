package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ContactsCommand implements CommandExecutor {
    private final ContactsManager contactsManager;

    public ContactsCommand(ContactsManager contactsManager) {
        this.contactsManager = contactsManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("contacts"))
            return false;

        if(!(commandSender instanceof Player player)) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.errorPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Contacts - Must Be Player")));
            return true;
        }

        if (CraftManager.getInstance().getCraftByPlayer(player) == null) {
            player.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("You must be piloting a craft")));
            return true;
        }

        int page;
        try {
            if (args.length == 0) {
                page = 1;
            }
            else {
                page = Integer.parseInt(args[0]);
            }
        } catch(NumberFormatException e) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Paginator - Invalid Page"))
                    .append(Component.text(" \""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return true;
        }

        Craft base = CraftManager.getInstance().getCraftByPlayer(player);
        if (base == null)
            return true;

        ComponentPaginator paginator = new ComponentPaginator(
                I18nSupport.getInternationalisedComponent("Contacts"),
                (pageNumber) -> "/contacts " + pageNumber);
        for (Craft target : contactsManager.get(base)) {
            if (target.getHitBox().isEmpty())
                continue;

            Component notification = ContactsManager.contactMessage(false, base, target);
            paginator.addLine(notification);
        }
        if (paginator.isEmpty()) {
            player.sendMessage(ChatUtils.commandPrefix().append(I18nSupport.getInternationalisedComponent("Contacts - None Found")));
            return true;
        }
        if (!paginator.isInBounds(page)){
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Paginator - Invalid page"))
                    .append(Component.text(" \""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return true;
        }
        for (Component line : paginator.getPage(page)) {
            commandSender.sendMessage(line);
        }
        return true;
    }
}
