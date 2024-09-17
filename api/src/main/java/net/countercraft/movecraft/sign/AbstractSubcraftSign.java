package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractSubcraftSign extends AbstractCraftSign {

    // TODO: Replace by writing to the signs nbt data
    protected static final Set<MovecraftLocation> IN_USE = Collections.synchronizedSet(new HashSet<>());

    protected final Function<String, @Nullable CraftType> craftTypeRetrievalFunction;

    protected final Supplier<Plugin> pluginInstance;

    public AbstractSubcraftSign(Function<String, @Nullable CraftType> craftTypeRetrievalFunction, final Supplier<Plugin> plugin) {
        this(null, craftTypeRetrievalFunction, plugin);
    }

    public AbstractSubcraftSign(final String permission, Function<String, @Nullable CraftType> craftTypeRetrievalFunction, final Supplier<Plugin> plugin) {
        super(permission, false);
        this.craftTypeRetrievalFunction = craftTypeRetrievalFunction;
        this.pluginInstance = plugin;
    }

    @Override
    public boolean processSignClick(Action clickType, SignListener.SignWrapper sign, Player player) {
        if (!this.isSignValid(clickType, sign, player)) {
            return false;
        }
        if (!this.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }
        Craft craft = this.getCraft(sign);

        if (craft instanceof PlayerCraft pc) {
            if (!pc.isNotProcessing() && !this.ignoreCraftIsBusy) {
                this.onCraftIsBusy(player, craft);
                return false;
            }
        }

        return internalProcessSign(clickType, sign, player, craft);
    }

    @Override
    protected boolean internalProcessSign(Action clickType, SignListener.SignWrapper sign, Player player, Craft craft) {
        if (craft != null) {
            // TODO: Add property to crafts that they can use subcrafts?
            if (!this.canPlayerUseSignOn(player, craft)) {
                return false;
            }
        }
        return this.internalProcessSignWithCraft(clickType, sign, craft, player);
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        if (!this.isSignValid(Action.PHYSICAL, sign, event.getPlayer())) {
            for (int i = 0; i < sign.lines().size(); i++) {
                sign.line(i, Component.empty());
            }
            return false;
        }
        this.applyDefaultText(sign);
        return true;
    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        String[] headerSplit = sign.getRaw(0).split(" ");
        if (headerSplit.length != 2) {
            return false;
        }
        // TODO: Change to enums?
        String action = headerSplit[headerSplit.length - 1].toUpperCase();
        if (!this.isActionAllowed(action)) {
            return false;
        }
        return this.getCraftType(sign) != null;
    }

    @Override
    protected boolean canPlayerUseSign(Action clickType, SignListener.SignWrapper sign, Player player) {
        if (!super.canPlayerUseSign(clickType, sign, player)) {
            return false;
        }
        CraftType craftType = this.getCraftType(sign);
        if (craftType != null) {
            return player.hasPermission("movecraft." + craftType.getStringProperty(CraftType.NAME) + ".pilot") && this.canPlayerUseSignForCraftType(clickType, sign, player, craftType);
        }
        return false;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, @Nullable Craft craft, Player player) {
        CraftType subcraftType = this.getCraftType(sign);

        final Location signLoc = sign.block().getLocation();
        final MovecraftLocation startPoint = new MovecraftLocation(signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ());

        if (craft != null) {
            craft.setProcessing(true);
            // TODO: SOlve this more elegantly...
            new BukkitRunnable() {
                @Override
                public void run() {
                    craft.setProcessing(false);
                }
            }.runTaskLater(this.pluginInstance.get(), (10));
        }

        if (!IN_USE.add(startPoint)) {
            this.onActionAlreadyInProgress(player);
            return true;
        }

        this.applyDefaultText(sign);

        final World world = sign.block().getWorld();

        this.runDetectTask(clickType, subcraftType, craft, world, player, startPoint);

        // TODO: Change this, it is ugly, should be done by the detect task itself
        new BukkitRunnable() {
            @Override
            public void run() {
                IN_USE.remove(startPoint);
            }
        }.runTaskLater(this.pluginInstance.get(), 4);

        return true;
    }

    protected void applyDefaultText(SignListener.SignWrapper sign) {
        if (sign.getRaw(2).isBlank() && sign.getRaw(3).isBlank()) {
            Component l3 = this.getDefaultTextFor(2);
            Component l4 = this.getDefaultTextFor(3);
            if (l3 != null) {
                sign.line(2, l3);
            }
            if (l4 != null) {
                sign.line(3, l4);
            }
        }
    }

    @Nullable
    protected CraftType getCraftType(SignListener.SignWrapper wrapper) {
        String ident = wrapper.getRaw(1);
        if (ident.trim().isBlank()) {
            return null;
        }
        return this.craftTypeRetrievalFunction.apply(ident);
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking, EventType eventType) {
        boolean resultSuper = super.shouldCancelEvent(processingSuccessful, type, sneaking, eventType);
        if (!resultSuper) {
            return eventType == EventType.SIGN_CLICK_ON_CRAFT || eventType == EventType.SIGN_CLICK;
        }
        return resultSuper;
    }

    protected abstract void runDetectTask(Action clickType, CraftType subcraftType, Craft parentCraft, World world, Player player, MovecraftLocation startPoint);
    protected abstract boolean isActionAllowed(final String action);
    protected abstract void onActionAlreadyInProgress(Player player);
    protected abstract Component getDefaultTextFor(int line);
    protected abstract boolean canPlayerUseSignForCraftType(Action clickType, SignListener.SignWrapper sign, Player player, CraftType subCraftType);

}
