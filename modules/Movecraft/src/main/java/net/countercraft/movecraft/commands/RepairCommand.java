package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.repair.Repair;
import net.countercraft.movecraft.utils.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class RepairCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("repair")){
            return false;
        }
        if (!sender.hasPermission("movecraft.commands.repair")){
            sender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Error, invalid permissions");
            return true;
        }
        if (args.length < 1){
            TopicPaginator paginator = new TopicPaginator("Repair");
            paginator.addLine("/repair list [playername:all:you] = Display list of ongoing repairs");
            paginator.addLine("/repair enable = Enables repair functionality.");
            paginator.addLine("/repair disable = Disables repair functionality.");
        }

        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("list")){
            if (args.length >= 1){
                TopicPaginator paginator = new TopicPaginator(String.format("Repairs by %s", player.getName()));
                for (Repair repair : Movecraft.getInstance().getRepairManager().getRepairs()){
                    if (repair.getPlayerUUID() == player.getUniqueId()){
                        if (repair.getRunning().get()){
                            paginator.addLine(String.format("%s - %d/%d", repair.getName(), (repair.getFragileBlockUpdateCommands().size() + repair.getUpdateCommands().size()), repair.getMissingBlocks()));
                        }
                    }
                }
                int page;
                try {
                    if (args.length == 1){
                        page = 1;
                    } else {
                        page = Integer.parseInt(args[1]);
                    }
                } catch (NumberFormatException e){
                    player.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Invalid page " + args[1]);
                    return true;
                }
                if (paginator.isEmpty()){
                    player.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You have no active repairs.");
                    return true;
                }
                for (String line : paginator.getPage(page)){
                    player.sendMessage(line);
                }
            } else if (args[1].equalsIgnoreCase("all")){

            } else {

            }
        }
        return false;
    }
}
