package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CraftReportCommand implements CommandExecutor{

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender.getName().equalsIgnoreCase("craftreport"))
            return false;
        if (!commandSender.hasPermission("movecraft.commands") && !commandSender.hasPermission("movecraft.commands.craftreport")) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        int page;
        try {
            if (args.length == 0)
                page = 1;
            else
                page = Integer.parseInt(args[0]);
        }catch(NumberFormatException e){
            commandSender.sendMessage(" Invalid page \"" + args[0] + "\"");
            return true;
        }
        if (CraftManager.getInstance().isEmpty()){
            commandSender.sendMessage("No crafts found");
            return true;
        }
        TopicPaginator paginator = new TopicPaginator("Craft Report");
        for (Craft craft : CraftManager.getInstance()) {
            HashHitBox hitBox = craft.getHitBox();
            if (craft.getNotificationPlayer() != null)
                paginator.addLine( craft.getType().getCraftName() + " " + craft.getNotificationPlayer().getName() + " " + hitBox.size() + " @ " + hitBox.getMinX() + "," + hitBox.getMinY() + "," + hitBox.getMinZ());
            else
                paginator.addLine( craft.getType().getCraftName() + " NULL " + hitBox.size() + " @ " + hitBox.getMinX() + "," + hitBox.getMinY() + "," + hitBox.getMinZ());
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
        return true;

    }
}
