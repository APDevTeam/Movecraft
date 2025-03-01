package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PilotedCraft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.util.ChatUtils;
import net.countercraft.movecraft.util.ComponentPaginator;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CraftReportCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (commandSender.getName().equalsIgnoreCase("craftreport"))
            return false;

        if (!commandSender.hasPermission("movecraft.commands")
                || !commandSender.hasPermission("movecraft.commands.craftreport")) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.errorPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Insufficient Permissions")));
            return true;
        }
        int page;
        try {
            if (args.length == 0)
                page = 1;
            else
                page = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Paginator - Invalid Page"))
                    .append(Component.text("\""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return true;
        }
        if (CraftManager.getInstance().isEmpty()) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Craft Report - None Found")));
            return true;
        }
        ComponentPaginator paginator = new ComponentPaginator(
                I18nSupport.getInternationalisedComponent("Craft Report"),
                (pageNumber) -> "/craftreport " + pageNumber);

        for (Craft craft : CraftManager.getInstance()) {
            HitBox hitBox = craft.getHitBox();
            Component line = Component.empty();
            Component name = Component.text(craft.getType().getStringProperty(CraftType.NAME));
            if (craft instanceof SinkingCraft)
                name = name.color(NamedTextColor.RED);
            else if (craft.getDisabled())
                name = name.color(NamedTextColor.BLUE);
            line = line.append(name).append(Component.text(" "));
            if (craft instanceof PilotedCraft pilotedCraft)
                line = line.append(Component.text(pilotedCraft.getPilot() == null ? pilotedCraft.getPilotUUID().toString() : pilotedCraft.getPilot().getName()));
            else
                line = line.append(I18nSupport.getInternationalisedComponent("None"));
            line = line.append(Component.text(" "));
            line = line
                    .append(Component.text(hitBox.size()))
                    .append(Component.text(" @ "))
                    .append(Component.text(hitBox.getMinX()))
                    .append(Component.text(","))
                    .append(Component.text(hitBox.getMinY()))
                    .append(Component.text(","))
                    .append(Component.text(hitBox.getMinZ()))
                    .append(Component.text(" - "))
                    .append(Component.text(String.format("%.2f", 1000 * craft.getMeanCruiseTime())))
                    .append(Component.text("ms"));
            paginator.addLine(line);
        }
        if (!paginator.isInBounds(page)) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Paginator - Invalid page"))
                    .append(Component.text(" \""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return true;
        }
        for (Component line : paginator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }
}
