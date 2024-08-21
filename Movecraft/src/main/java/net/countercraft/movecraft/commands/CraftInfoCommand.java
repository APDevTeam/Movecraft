package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.TopicPaginator;
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
    @Syntax("[player|self] <page>")
    @Description("Get information on a piloted craft")
    @CommandCompletion("@players")
    public static void onCommand(Player player, OnlinePlayer subject, @Default("1") int page) {
        var craft = CraftManager.getInstance().getCraftByPlayer(subject.getPlayer());
        if (craft == null) {
            //maybe no craft found would be more correct
            player.sendMessage("No player found");
            return;
        }

        craftInfo(player, craft, page);
    }

    @Subcommand("self")
    @Syntax("<page>")
    public static void selfCraftInfo(Player player, @Default("1") int page) {
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if(craft == null){
            player.sendMessage("You must be piloting a craft.");
            return;
        }

        craftInfo(player, craft, page);
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
}
