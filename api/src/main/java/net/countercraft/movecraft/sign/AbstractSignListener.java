package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/*
 * As soon as 1.18 support is dropped, the adapter system will be dropped too
 */
@Deprecated(forRemoval = true)
public abstract class AbstractSignListener implements Listener {

    public static AbstractSignListener INSTANCE;

    public AbstractSignListener() {
        INSTANCE = this;
    }

    /*
     * As soon as 1.18 support is dropped, the adapter system will be dropped too
     */
    @Deprecated(forRemoval = true)
    public record SignWrapper(
            @Nullable Sign block,
            Function<Integer, Component> getLine,
            List<Component> lines,
            BiConsumer<Integer, Component> setLine,
            BlockFace facing
    ) {
        public Component line(int index) {
            if (index >= lines.size() || index < 0) {
                throw new IndexOutOfBoundsException();
            }
            return getLine().apply(index);
        }

        public void line(int index, Component component) {
            setLine.accept(index, component);
        }

        public String getRaw(int index) {
            return PlainTextComponentSerializer.plainText().serialize(line(index));
        }

        public String[] rawLines() {
            String[] result = new String[this.lines.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = this.getRaw(i);
            }
            return result;
        }

        public boolean isEmpty() {
            for(String s : this.rawLines()) {
                if (s.trim().isEmpty() || s.trim().isBlank()) {
                    continue;
                }
                else {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj instanceof SignWrapper other) {
                return areSignsEqual(other);
            }
            return false;
        }

        public boolean areSignsEqual(SignWrapper other) {
            return areSignsEqual(this, other);
        }

        public static boolean areSignsEqualIgnoreFace(SignWrapper a, SignWrapper b) {
            return areSignsEqual(a, b, true);
        }

        public static boolean areSignsEqual(SignWrapper a, SignWrapper b) {
            return areSignsEqual(a, b, false);
        }

        public static boolean areSignsEqual(SignWrapper a, SignWrapper b, boolean ignoreFace) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            String[] aLines = a.rawLines();
            String[] bLines = b.rawLines();

            if (aLines.length != bLines.length) {
                return false;
            }

            for (int i = 0; i < aLines.length; i++) {
                String aLine = aLines[i].trim();
                String bLine = bLines[i].trim();

                if (!aLine.equalsIgnoreCase(bLine)) {
                    return false;
                }
            }

            // Now check the facing too!
            return ignoreFace || a.facing().equals(b.facing());
        }

        public static boolean areSignsEqual(SignWrapper[] a, SignWrapper[] b) {
            if (a == null || b == null) {
                return false;
            }
            if (a.length != b.length) {
                return false;
            }

            for (int i = 0; i < a.length; i++) {
                SignWrapper aWrap = a[i];
                SignWrapper bWrap = b[i];
                if (!areSignsEqual(aWrap, bWrap)) {
                    return false;
                }
            }
            return true;
        }

        public void copyContent(SignWrapper other) {
            this.copyContent(other::line, (i) -> i < other.lines().size());
        }

        public void copyContent(Function<Integer, Component> retrievalFunction, Function<Integer, Boolean> indexValidator) {
            for (int i = 0; i < this.lines().size() && indexValidator.apply(i); i++) {
                this.line(i, retrievalFunction.apply(i));
            }
        }

    }

    public SignWrapper[] getSignWrappers(Sign sign) {
        return getSignWrappers(sign, false);
    };
    public abstract SignWrapper[] getSignWrappers(Sign sign, boolean ignoreEmpty);
    protected abstract SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent);
    protected SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent) {
        return this.getSignWrapper(sign, interactEvent.getPlayer());
    }
    protected abstract SignWrapper getSignWrapper(Sign sign, Player player);

    public abstract void processSignTranslation(final Craft craft, boolean checkEventIsUpdated);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onCraftDetect(CraftDetectEvent event) {
        final World world = event.getCraft().getWorld();
        event.getCraft().getHitBox().forEach(
                (mloc) -> {
                    Block block = mloc.toBukkit(world).getBlock();
                    BlockState state = block.getState();
                    if (state instanceof Sign sign) {
                        for (SignWrapper wrapper : this.getSignWrappers(sign)) {
                            // Would be one more readable line if using Optionals but Nullables were wanted
                            AbstractCraftSign acs = AbstractCraftSign.getCraftSign(wrapper.line(0));
                            if (acs != null) {
                                acs.onCraftDetect(event, wrapper);
                            }
                        }
                    }
                }
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignTranslate(SignTranslateEvent event) {
        // Would be one more readable line if using Optionals but Nullables were wanted
        AbstractCraftSign acs = AbstractCraftSign.getCraftSign(event.line(0));
        if (acs != null) {
            acs.onSignMovedByCraft(event);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            SignWrapper signWrapper = this.getSignWrapper(sign, event.getPlayer());
            SignWrapper wrapper = this.getSignWrapper(sign, event);

            AbstractMovecraftSign.EventType eventType = signWrapper.isEmpty() ? AbstractMovecraftSign.EventType.SIGN_CREATION : AbstractMovecraftSign.EventType.SIGN_EDIT;
            if (eventType == AbstractMovecraftSign.EventType.SIGN_EDIT && MathUtils.getCraftByPersistentBlockData(sign.getLocation()) != null) {
                eventType = AbstractMovecraftSign.EventType.SIGN_EDIT_ON_CRAFT;
            }
            final AbstractMovecraftSign.EventType eventTypeTmp = eventType;
            AbstractMovecraftSign ams = AbstractMovecraftSign.get(wrapper.line(0));
            // Would be one more readable line if using Optionals but Nullables were wanted
            if (ams != null) {
                if (!eventType.isOnCraft()) {
                    ItemStack heldItem = event.getPlayer().getActiveItem();
                    // Allow the usage of the colors and wax for things => Cleaner solution: Use NMS to check if the item is an instanceof of SignApplicator...
                    if (Tags.SIGN_EDIT_MATERIALS.contains(heldItem.getType())) {
                        return;
                    }

                    if (Tags.SIGN_BYPASS_RIGHT_CLICK.contains(heldItem.getType())) {
                        return;
                    }
                }

                boolean success = ams.processSignChange(event, wrapper);
                if (ams.shouldCancelEvent(success, null, event.getPlayer().isSneaking(), eventTypeTmp)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            SignWrapper wrapper = this.getSignWrapper(sign, event);
            ItemStack heldItem = event.getItem();
            boolean onCraft = MathUtils.getCraftByPersistentBlockData(sign.getLocation()) != null;

            // Always cancel if on craft => Avoid clicking empty sides and entering edit mode
            if (onCraft && wrapper.isEmpty()) {
                event.setCancelled(true);
            }

            boolean sneaking = event.getPlayer().isSneaking();
            // Allow editing and breaking signs with tools
            if (heldItem != null && !onCraft) {
                // Allow the usage of the colors and wax for things => Cleaner solution: Use NMS to check if the item is an instanceof of SignApplicator...
                if (Tags.SIGN_EDIT_MATERIALS.contains(heldItem.getType()) && event.getAction().isRightClick()) {
                    return;
                }
                if (sneaking) {
                    if (Tags.SIGN_BYPASS_RIGHT_CLICK.contains(heldItem.getType()) && (event.getAction().isRightClick())) {
                        return;
                    }

                    if (Tags.SIGN_BYPASS_LEFT_CLICK.contains(heldItem.getType()) && (event.getAction().isLeftClick())) {
                        return;
                    }
                }
            }

            // Would be one more readable line if using Optionals but Nullables were wanted
            AbstractMovecraftSign ams = AbstractMovecraftSign.get(wrapper.line(0));
            if (ams != null) {
                boolean success = ams.processSignClick(event.getAction(), wrapper, event.getPlayer());
                // Always cancel, regardless of the success
                event.setCancelled(true);
                if (ams.shouldCancelEvent(success, event.getAction(), sneaking, onCraft ? AbstractMovecraftSign.EventType.SIGN_CLICK_ON_CRAFT : AbstractMovecraftSign.EventType.SIGN_CLICK)) {
                    event.setCancelled(true);
                }
            }
        }
    }

}
