package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.TopicPaginator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

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
    @Syntax("<player> <page:integer>")
    @Description("Get information on a piloted craft")
    public static void onCommand(Player player, String[] args) {
        if(args.length == 0){
            Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
            if(craft == null){
                player.sendMessage("You must be piloting a craft.");
                return;
            }
            craftInfo(player, craft, 1);
            return;
        }

        //if first argument is an integer, get your own craft's info
        if (handleSelf(player, args[0]))
            return;

        handleOther(player, args);
    }

    private static boolean handleSelf(Player player, String page) {
        OptionalInt pageQuery = MathUtils.parseInt(page);

        if (pageQuery.isEmpty()) {
            return false;
        }

        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if(craft == null){
            player.sendMessage("You must be piloting a craft.");
            return true;
        }

        craftInfo(player, craft, pageQuery.getAsInt());
        return true;
    }

    private static void handleOther(Player player, String[] args) {
        OptionalInt pageQuery;
        final String lookupPlayer = args[0];

        var craft = CraftManager.getInstance().getCraftByPlayerName(lookupPlayer);
        if (craft == null) {
            //maybe no craft found would be more correct
            player.sendMessage("No player found");
            return;
        }

        if (args.length == 1) {
            pageQuery = OptionalInt.of(1);
            craftInfo(player, craft, pageQuery.getAsInt());
            return;
        }

        final String page = args[1];
        pageQuery = MathUtils.parseInt(page);
        if(pageQuery.isEmpty()){
            player.sendMessage("Parameter " + page + " must be a page number.");
            return;
        }

        craftInfo(player, craft, pageQuery.getAsInt());
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
