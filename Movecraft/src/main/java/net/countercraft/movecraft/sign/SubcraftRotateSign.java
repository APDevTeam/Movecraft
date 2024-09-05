package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class SubcraftRotateSign extends AbstractSubcraftSign {

    public SubcraftRotateSign(Function<String, @Nullable CraftType> craftTypeRetrievalFunction, Supplier<Plugin> plugin) {
        super(craftTypeRetrievalFunction, plugin);
    }

    @Override
    protected void runDetectTask(Action clickType, CraftType subcraftType, Craft parentcraft, World world, Player player, MovecraftLocation startPoint) {
        final MovecraftRotation rotation = MovecraftRotation.fromAction(clickType);
        if (rotation == MovecraftRotation.NONE) {
            return;
        }

        CraftManager.getInstance().detect(
                startPoint,
                subcraftType, (type, w, p, parents) -> {
                    if (parents.size() > 1)
                        return new Pair<>(Result.failWithMessage(I18nSupport.getInternationalisedString(
                                "Detection - Failed - Already commanding a craft")), null);
                    if (parents.size() < 1)
                        return new Pair<>(Result.succeed(), new SubcraftRotateCraft(type, w, p));

                    Craft parent = parents.iterator().next();
                    return new Pair<>(Result.succeed(), new SubCraftImpl(type, w, parent));
                },
                world, player, player,
                subcraft -> () -> {
                    Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(subcraft, CraftPilotEvent.Reason.SUB_CRAFT));
                    if (subcraft instanceof SubCraft) { // Subtract craft from the parent
                        Craft parent = ((SubCraft) subcraft).getParent();
                        var newHitbox = parent.getHitBox().difference(subcraft.getHitBox());;
                        parent.setHitBox(newHitbox);
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            subcraft.rotate(rotation, startPoint, true);
                            if (subcraft instanceof SubCraft) {
                                Craft parent = ((SubCraft) subcraft).getParent();
                                var newHitbox = parent.getHitBox().union(subcraft.getHitBox());
                                parent.setHitBox(newHitbox);
                            }
                            CraftManager.getInstance().release(subcraft, CraftReleaseEvent.Reason.SUB_CRAFT, false);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 3);
                }
        );
    }

    @Override
    protected boolean isActionAllowed(String action) {
        return action.toUpperCase().equalsIgnoreCase("ROTATE");
    }

    @Override
    protected void onActionAlreadyInProgress(Player player) {
        player.sendMessage(I18nSupport.getInternationalisedString("Rotation - Already Rotating"));
    }

    static final Component DEFAULT_LINE_3 = Component.text("_\\ / _");
    static final Component DEFAULT_LINE_4 = Component.text("/ \\");

    @Override
    protected Component getDefaultTextFor(int line) {
        switch (line) {
            case 2:
                return DEFAULT_LINE_3;
            case 3:
                return DEFAULT_LINE_4;
            default:
                return null;
        }
    }

    @Override
    protected boolean canPlayerUseSignForCraftType(Action clickType, AbstractSignListener.SignWrapper sign, Player player, CraftType subcraftType) {
        final String craftTypeStr = subcraftType.getStringProperty(CraftType.NAME).toLowerCase();
        if (!player.hasPermission("movecraft." + craftTypeStr + ".rotate")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return false;
        }
        return true;
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        player.sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {
        // Ignored
    }
}
