package net.countercraft.movecraft.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraftImpl;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Locale;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

@CommandAlias("pilot")
@CommandPermission("movecraft.commands,movecraft.commands.pilot")
public class PilotCommand extends BaseCommand {

    @Default
    @CommandCompletion("@crafttypes")
    @Syntax("[CRAFTTYPE]")
    @Description("Pilots the craft at your feet")
    public static void onCommand(Player player, CraftType craftType) {
        if (!player.hasPermission("movecraft." + craftType.toString().toLowerCase(Locale.ROOT) + ".pilot")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
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
    }
}
