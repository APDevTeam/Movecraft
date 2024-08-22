package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Optional;

@CommandAlias("crafttype")
@CommandPermission("movecraft.commands")
public class CraftTypeCommand extends BaseCommand {

    private static final Field[] craftTypeFields;

    static {
        craftTypeFields = CraftType.class.getDeclaredFields();
        for (var field : craftTypeFields) {
            field.setAccessible(true);
        }
    }

    @Syntax("<page>")
    @Subcommand("list")
    public static void listCrafts(CommandSender commandSender, @Default("1") int page) {
        sendTypeListPage(page, commandSender);
    }

    @Syntax("<page>")
    @Subcommand("self")
    public static void selfCommand(Player player, @Default("1") int page) {
        Optional<CraftType> typeQuery = tryGetCraftFromPlayer(player);
        if (typeQuery.isEmpty()) {
            player.sendMessage("You aren't piloting any craft!");
            return;
        }

        sendTypePage(typeQuery.get(), page, player);
    }

    @Default
    @Syntax("[list|self|CRAFTTYPE] <page>")
    @Description("Get information on a specific craft type")
    @CommandCompletion("@crafttypes")
    public static void onCommand(@NotNull CommandSender commandSender, CraftType type, @Default("1") int page) {

        if (!commandSender.hasPermission("movecraft." + type.getStringProperty(CraftType.NAME) + ".pilot")) {
            commandSender.sendMessage("You don't have permission for that craft type!");
            return;
        }

        sendTypePage(type, page, commandSender);
    }

    private static void sendTypePage(@NotNull CraftType type, int page, @NotNull CommandSender commandSender) {
        TopicPaginator paginator = new TopicPaginator("Type Info");
        for (var field : craftTypeFields) {
            if (field.getName().equals("data")) { // don't include the backing data object
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
            if (repr.length() > ChatPaginator.GUARANTEED_NO_WRAP_CHAT_PAGE_WIDTH) {
                paginator.addLine(field.getName() + ": too long");
            } else {
                paginator.addLine(field.getName() + ": " + value);
            }
        }
        if (!paginator.isInBounds(page)) {
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for (String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    private static void sendTypeListPage(int page, @NotNull CommandSender commandSender) {
        TopicPaginator paginator = new TopicPaginator("Type Info");
        for (var entry : CraftManager.getInstance().getCraftTypes()) {
            paginator.addLine(entry.getStringProperty(CraftType.NAME));
        }
        if (!paginator.isInBounds(page)) {
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for (String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    @NotNull
    private static Optional<CraftType> tryGetCraftFromPlayer(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            return Optional.empty();
        }
        Craft c = CraftManager.getInstance().getCraftByPlayer((Player) commandSender);
        if (c == null) {
            return Optional.empty();
        }
        return Optional.of(c.getType());
    }
}
