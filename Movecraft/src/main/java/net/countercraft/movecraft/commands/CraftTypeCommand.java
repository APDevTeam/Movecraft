package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

// TODO Rewrite and open a virtual book instead
// Or use the new dialogues for this
public class CraftTypeCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        TypeSafeCraftType type;
        int page;
        if(args.length == 0 || (args.length == 1 && MathUtils.parseInt(args[0]).isPresent())) {
            Optional<TypeSafeCraftType> typeQuery = tryGetCraftFromPlayer(commandSender);
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
        if(!commandSender.hasPermission("movecraft." + type.getName().toLowerCase() + ".pilot")) {
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
        for(TypeSafeCraftType type : CraftManager.getInstance().getTypesafeCraftTypes())
            if(commandSender.hasPermission("movecraft." + type.getName().toLowerCase() + ".pilot"))
                completions.add(type.getName());
        completions.add("list");
        List<String> returnValues = new ArrayList<>();
        for(String completion : completions)
            if(completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);
        return returnValues;
    }

    private void sendTypePage(@NotNull TypeSafeCraftType type, int page, @NotNull  CommandSender commandSender){
        TopicPaginator paginator = new TopicPaginator("Type Info");

        for (var property : TypeSafeCraftType.PROPERTY_REGISTRY.getAllValues()) {
            if (type.hasInSelfOrAnyParent(property)) {
                var value = type.get(property);
                var repr = property.key().toString() + ": " + value;
                if(repr.length() > ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH){
                    paginator.addLine(property.key().toString() + ": too long");
                } else {
                    paginator.addLine(property.key().toString() + ": " + value);
                }
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
        for(var entry : CraftManager.getInstance().getTypesafeCraftTypes()){
            paginator.addLine(entry.getName());
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    @NotNull
    private Optional<TypeSafeCraftType> tryGetCraftFromPlayer(CommandSender commandSender){
        if (!(commandSender instanceof Player)) {
            return Optional.empty();
        }
        Craft c = CraftManager.getInstance().getCraftByPlayer((Player) commandSender);
        if(c == null){
            return Optional.empty();
        }
        return Optional.of(c.getCraftProperties());
    }
}
