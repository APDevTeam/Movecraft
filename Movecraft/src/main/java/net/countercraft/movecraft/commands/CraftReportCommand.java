package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
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
import org.bukkit.command.CommandSender;

@CommandAlias("craftreport")
@CommandPermission("movecraft.commands|movecraft.commands.craftreport")
public class CraftReportCommand extends BaseCommand {

    @Default
    @Syntax("<page>")
    @Description("Reports on all active craft")
    public static void onCommand(CommandSender commandSender, @Default("1") int page) {
        // TODO: This is ugly to read, maybe better make a component concatenator method in ChatUtils?
        // TODO: Should we keep the error for invalid page or just default to 1 in this case?
        /*
        try {
            if (args.length == 0)
                page = 1;
            else
                page = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Paginator - Invalid Page"))
                    .append(Component.text("\""))
                    .append(Component.text(args[0]))
                    .append(Component.text("\"")));
            return;
        }*/
        if (CraftManager.getInstance().isEmpty()) {
            commandSender.sendMessage(Component.empty()
                    .append(ChatUtils.commandPrefix())
                    .append(I18nSupport.getInternationalisedComponent("Craft Report - None Found")));
            return;
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
                line = line.append(Component.text(pilotedCraft.getPilot().getName()));
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
                    .append(Component.text(page))
                    .append(Component.text("\"")));
            return;
        }
        for (Component line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }
}
