package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CruiseOnPilotCraft;
import net.countercraft.movecraft.craft.CruiseOnPilotSubCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.PlayerCraftImpl;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.CraftSupplier;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
        CraftType craftType = CraftManager.getInstance().getCraftTypeFromString(args[0]);
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
                world, player,
                Movecraft.getAdventure().player(player),
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
        for (CraftType type : CraftManager.getInstance().getCraftTypes())
            if (commandSender.hasPermission("movecraft." + type.getStringProperty(CraftType.NAME) + ".pilot"))
                completions.add(type.getStringProperty(CraftType.NAME));

        List<String> returnValues = new ArrayList<>();
        for (String completion : completions)
            if (completion.toLowerCase().startsWith(strings[strings.length-1].toLowerCase()))
                returnValues.add(completion);

        return returnValues;
    }
}
