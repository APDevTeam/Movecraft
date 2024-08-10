package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractSignListener implements Listener {

    public static AbstractSignListener INSTANCE;

    public AbstractSignListener() {
        INSTANCE = this;
    }

    public static record SignWrapper(
            Sign block,
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
        }

    }

    public abstract SignWrapper[] getSignWrappers(Sign sign);
    protected abstract SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent);
    protected abstract SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onCraftDetect(CraftDetectEvent event) {
        final World world = event.getCraft().getWorld();
        event.getCraft().getHitBox().forEach(
                (mloc) -> {
                    Block block = mloc.toBukkit(world).getBlock();
                    BlockState state = block.getState();
                    if (state instanceof Sign sign) {
                        for (SignWrapper wrapper : this.getSignWrappers(sign)) {
                            String ident = PlainTextComponentSerializer.plainText().serialize(wrapper.line(0));
                            AbstractCraftSign.tryGetCraftSign(ident).ifPresent(acs -> acs.onCraftDetect(event, wrapper));
                        }
                    }
                }
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignTranslate(SignTranslateEvent event) {
        String ident = event.getLine(0);
        AbstractCraftSign.tryGetCraftSign(ident).ifPresent(acs -> acs.onSignMovedByCraft(event));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            SignWrapper wrapper = this.getSignWrapper(sign, event);
            final String signHeader = PlainTextComponentSerializer.plainText().serialize(wrapper.line(0));
            AbstractMovecraftSign.tryGet(signHeader).ifPresent(ams -> {

                boolean success = ams.processSignChange(event, wrapper);
                if (ams.shouldCancelEvent(success, null, event.getPlayer().isSneaking())) {
                    event.setCancelled(true);
                }
            });
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
            final String signHeader = PlainTextComponentSerializer.plainText().serialize(wrapper.line(0));
            AbstractMovecraftSign.tryGet(signHeader).ifPresent(ams -> {
                boolean success = ams.processSignClick(event.getAction(), wrapper, event.getPlayer());
                if (ams.shouldCancelEvent(success, event.getAction(), event.getPlayer().isSneaking())) {
                    event.setCancelled(true);
                }
            });
        }
    }

}
