package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraftImpl;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class PilotCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("pilot"))
            return false;
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - Must Be Player"));
            return true;
        }
        Player player = (Player) commandSender;
        if (!player.hasPermission("movecraft.commands") || !player.hasPermission("movecraft.commands.pilot")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - No Craft Type"));
            return true;
        }
        if (!player.hasPermission("movecraft." + args[0] + ".pilot")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        TypeSafeCraftType craftType = CraftManager.getInstance().getCraftTypeByName(args[0]);
        if (craftType == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Pilot - Invalid Craft Type"));
            return true;
        }

        final World world = player.getWorld();
        MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());

        CraftManager.getInstance().detect(
                startPoint,
                craftType, (type, w, p, parents) -> {
                    assert p != null; // Note: This only passes in a non-null player.
                    if (parents.size() > 0)
                        return new Pair<>(Result.failWithMessage(I18nSupport.getInternationalisedString(
                                "Detection - Failed - Already commanding a craft")), null);

                    return new Pair<>(Result.succeed(),
                            new PlayerCraftImpl(type, w, p));
                },
                world, player, player,
                craft -> () -> {
                    // Release old craft if it exists
                    Craft oldCraft = CraftManager.getInstance().getCraftByPlayer(player);
                    if(oldCraft != null)
                        CraftManager.getInstance().release(oldCraft, CraftReleaseEvent.Reason.PLAYER, false);
                }
        );
       return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length != 1 || !commandSender.hasPermission("movecraft.commands")
                || !commandSender.hasPermission("movecraft.commands.pilot"))
            return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        for (TypeSafeCraftType type : CraftManager.getInstance().getTypesafeCraftTypes())
            if (commandSender.hasPermission("movecraft." + type.getName().toLowerCase() + ".pilot"))
                completions.add(type.getName());

        List<String> returnValues = new ArrayList<>();
        for (String completion : completions)
            if (completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);

        return returnValues;
    }
}
