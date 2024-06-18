package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class CraftTypeCommand implements TabExecutor {

    private static final Field[] craftTypeFields;
    static {
        craftTypeFields = CraftType.class.getDeclaredFields();
        for(var field : craftTypeFields){
            field.setAccessible(true);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CraftType type;
        int page;
        if(args.length == 0 || (args.length == 1 && MathUtils.parseInt(args[0]).isPresent())) {
            Optional<CraftType> typeQuery = tryGetCraftFromPlayer(commandSender);
            if(typeQuery.isEmpty()){
                commandSender.sendMessage("You must supply a craft type!");
                return true;
            }
            type = typeQuery.get();
            page = args.length == 0 ? 1 : MathUtils.parseInt(args[0]).getAsInt();
        }
        else {
            if(args.length > 1) {
                OptionalInt pageQuery = MathUtils.parseInt(args[1]);
                if(pageQuery.isEmpty()){
                    commandSender.sendMessage("Argument " + args[1] + " must be a page number");
                    return true;
                }
                page = pageQuery.getAsInt();
            }
            else {
                page = 1;
            }
            if(args[0].equalsIgnoreCase("list")) {
                sendTypeListPage(page, commandSender);
                return true;
            }
            type = CraftManager.getInstance().getCraftTypeFromString(args[0]);
        }
        if(type == null) {
            commandSender.sendMessage("You must supply a craft type!");
            return true;
        }
        if(!commandSender.hasPermission("movecraft." + type.getStringProperty(CraftType.NAME) + ".pilot")) {
            commandSender.sendMessage("You don't have permission for that craft type!");
            return true;
        }
        sendTypePage(type, page, commandSender);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length !=1 || !commandSender.hasPermission("movecraft.commands") || !commandSender.hasPermission("movecraft.commands.crafttype"))
            return Collections.emptyList();
        List<String> completions = new ArrayList<>();
        for(CraftType type : CraftManager.getInstance().getCraftTypes())
            if(commandSender.hasPermission("movecraft." + type.getStringProperty(CraftType.NAME) + ".pilot"))
                completions.add(type.getStringProperty(CraftType.NAME));
        completions.add("list");
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }

    private void sendTypePage(@NotNull CraftType type, int page, @NotNull  CommandSender commandSender){
        TopicPaginator paginator = new TopicPaginator("Type Info");
        for(var field : craftTypeFields){
            if(field.getName().equals("data")){ // don't include the backing data object
                continue;
            }
            Object value;
            try {
                value = field.get(type);
            } catch (IllegalAccessException e) {
                paginator.addLine(field.getName() + ": failed to access");
                continue;
            }
            var repr = field.getName() + ": " + value;
            if(repr.length() > ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH){
                paginator.addLine(field.getName() + ": too long");
            } else {
                paginator.addLine(field.getName() + ": " + value);
            }
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    private void sendTypeListPage(int page, @NotNull  CommandSender commandSender){
        TopicPaginator paginator = new TopicPaginator("Type Info");
        for(var entry : CraftManager.getInstance().getCraftTypes()){
            paginator.addLine(entry.getStringProperty(CraftType.NAME));
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    @NotNull
    private Optional<CraftType> tryGetCraftFromPlayer(CommandSender commandSender){
        if (!(commandSender instanceof Player)) {
            return Optional.empty();
        }
        Craft c = CraftManager.getInstance().getCraftByPlayer((Player) commandSender);
        if(c == null){
            return Optional.empty();
        }
        return Optional.of(c.getType());
    }
}
