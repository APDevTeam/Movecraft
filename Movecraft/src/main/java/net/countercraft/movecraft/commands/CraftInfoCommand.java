package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.*;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@CommandAlias("craftinfo")
public class CraftInfoCommand extends BaseCommand {
    private static final List<Function<Craft,? extends Iterable<String>>> providers = new ArrayList<>();
    static {
        registerMultiProvider(CraftInfoCommand::allowedBlockProvider);
        registerProvider((craft -> "Craft size: " + craft.getHitBox().size()));
        registerProvider((craft -> "Craft midpoint: " + craft.getHitBox().getMidPoint()));
        registerProvider((craft -> "Craft min bound: " + new MovecraftLocation(craft.getHitBox().getMinX(), craft.getHitBox().getMinY(), craft.getHitBox().getMinZ())));
        registerProvider((craft -> "Craft max bound: " + new MovecraftLocation(craft.getHitBox().getMaxX(), craft.getHitBox().getMaxY(), craft.getHitBox().getMaxZ())));
        registerProvider((craft -> "Craft world: " + craft.getWorld().getName()));
        registerProvider((craft -> "Craft type: " + craft.getType().getStringProperty(CraftType.NAME)));
        registerProvider((craft -> "Craft name: " + craft.getName()));
        registerProvider((craft -> "Is cruising: " + craft.getCruising()));
        registerProvider((craft -> "Cruise direction: " + craft.getCruiseDirection()));
        registerProvider((craft -> "Craft speed: " + craft.getSpeed()));
        registerProvider((craft -> "Mean cruise time: " + craft.getMeanCruiseTime()));
        registerProvider((craft -> "Is disabled: " + craft.getDisabled()));
        registerProvider((craft -> "Current gear: " + craft.getCurrentGear()));
    }

    private static @NotNull List<String> allowedBlockProvider(@NotNull Craft craft){
        return List.of();
    }

    public static void registerMultiProvider(@NotNull Function<Craft, ? extends Iterable<String>> provider){
        providers.add(provider);
    }

    public static void registerProvider(@NotNull Function<Craft, String> provider){
        providers.add(provider.andThen(List::of));
    }

    @Default
    @Syntax("[player] <page>")
    @Description("Get information on a piloted craft")
    @CommandCompletion("@players")
    public static void onOtherPlayer(CommandSender commandSender, String[] args) {
        if (args.length > 2) {
            throw new InvalidCommandArgument("Max allowed arguments: 2");
        }

        try {
            int page = args.length == 0 ? 1 : Integer.parseInt(args[0]);
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage("You can't run this command on yourself as console");
                return;
            }

            Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
            if (craft == null) {
                commandSender.sendMessage("No player craft found");
                return;
            }

            craftInfo(commandSender, craft, page);

        } catch (NumberFormatException e) {
            Player playerSubject = Bukkit.getPlayer(args[0]);
            if (playerSubject == null) {
                commandSender.sendMessage("First argument must be either a number or a player");
                return;
            }

            Craft craft = CraftManager.getInstance().getCraftByPlayer(playerSubject);
            if (craft == null) {
                commandSender.sendMessage("No player craft found");
                return;
            }

            craftInfo(commandSender, craft, parseDefaultInt(args[1], 1));
        }
    }

    public static void craftInfo(@NotNull CommandSender commandSender, @NotNull Craft craft, int page){
        TopicPaginator paginator = new TopicPaginator("Craft Info");
        for(var provider : providers){
            for(var line : provider.apply(craft)){
                paginator.addLine(line);
            }
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(String.format("Page %d is out of bounds.", page));
            return;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
    }

    public static int parseDefaultInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
