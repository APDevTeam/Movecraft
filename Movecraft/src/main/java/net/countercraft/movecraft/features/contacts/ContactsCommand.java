package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.TopicPaginator;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ContactsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, Command command, @NotNull String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("contacts"))
            return false;

        if(!(commandSender instanceof Player player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Contacts - Must Be Player"));
            return true;
        }

        if (CraftManager.getInstance().getCraftByPlayer(player) == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
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
        } catch(NumberFormatException e){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid Page") +" \"" + args[0] + "\"");
            return true;
        }

        Craft base = CraftManager.getInstance().getCraftByPlayer(player);
        if (base == null)
            return true;
        ContactsManager.update(base);

        TopicPaginator paginator = new TopicPaginator(I18nSupport.getInternationalisedString("Contacts"));
        for (Craft target : base.getContacts()) {
            if (target.getHitBox().isEmpty())
                continue;

            Component notification = ContactsManager.contactMessage(false, base, target);
            paginator.addLine(notification);
        }
        if (paginator.isEmpty()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Contacts - None Found"));
            return true;
        }
        if (!paginator.isInBounds(page)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid page") + "\"" + page + "\"");
            return true;
        }
        for (String line : paginator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }
}
