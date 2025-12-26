package net.countercraft.movecraft.sign;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftStopCruiseEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SignListener implements Listener {

    public static SignListener INSTANCE;

    public SignListener() {
        INSTANCE = this;
    }

    // Keep this, it is good to abstract away the sign
    public record SignWrapper(
            @Nullable Sign block,
            @Nullable SignSide side,
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

    protected SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent) {
        return this.getSignWrapper(sign, interactEvent.getPlayer());
    }

    protected final SignWrapper createFromSide(final Sign sign, final Side side) {
        SignSide signSide = sign.getSide(side);
        return createFromSide(sign, signSide, side);
    }

    protected final SignWrapper createForEditEvent(final Sign sign, final SignChangeEvent event) {
        BlockData blockData = sign.getBlock().getBlockData();
        BlockFace face;
        if (blockData instanceof Directional directional) {
            face = directional.getFacing();
        } else if (blockData instanceof Rotatable rotatable) {
            face = rotatable.getRotation();
        }
        else {
            face = BlockFace.SELF;
        }

        if (event.getSide() == Side.BACK) {
            face = face.getOppositeFace();
        }
        SignWrapper wrapper = new SignWrapper(
                sign,
                sign.getSide(event.getSide()),
                event::line,
                event.lines(),
                event::line,
                face
        );
        return wrapper;
    }

    protected final SignWrapper createFromSide(final Sign sign, final SignSide signSide, Side side) {
        BlockData blockData = sign.getBlock().getBlockData();
        BlockFace face;
        if (blockData instanceof Directional directional) {
            face = directional.getFacing();
        } else if (blockData instanceof Rotatable rotatable) {
            face = rotatable.getRotation();
        }
        else {
            face = BlockFace.SELF;
        }

        if (side == Side.BACK) {
            face = face.getOppositeFace();
        }
        SignWrapper wrapper = new SignWrapper(
                sign,
                signSide,
                signSide::line,
                signSide.lines(),
                signSide::line,
                face
        );
        return wrapper;
    }

    public SignWrapper[] getSignWrappers(Sign sign, boolean ignoreEmpty) {
        List<SignWrapper> wrappers = new ArrayList<>();
        for (Side side : Side.values()) {
            SignSide signSide = sign.getSide(side);
            SignWrapper wrapper = this.createFromSide(sign, signSide, side);
            wrappers.add(wrapper);
        }
        if (ignoreEmpty)
            wrappers.removeIf(SignWrapper::isEmpty);
        return wrappers.toArray(new SignWrapper[wrappers.size()]);
    }

    protected SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent) {
        @NotNull Side side = signChangeEvent.getSide();

        BlockData blockData = sign.getBlock().getBlockData();
        BlockFace face;
        if (blockData instanceof Directional directional) {
            face = directional.getFacing();
        } else if (blockData instanceof Rotatable rotatable) {
            face = rotatable.getRotation();
        }
        else {
            face = BlockFace.SELF;
        }

        if (side == Side.BACK) {
            face = face.getOppositeFace();
        }
        SignWrapper wrapper = new SignWrapper(
                sign,
                sign.getSide(side),
                signChangeEvent::line,
                signChangeEvent.lines(),
                signChangeEvent::line,
                face
        );
        return wrapper;
    }

    protected SignWrapper getSignWrapper(Sign sign, Player player) {
        @NotNull SignSide side = sign.getTargetSide(player);
        return this.createFromSide(sign, side, sign.getInteractableSideFor(player));
    }

    public void processSignTranslation(final Craft craft, boolean checkEventIsUpdated) {
        // Ignore facing value here and directly store the associated wrappers in the list
        Object2ObjectMap<SignWrapper, List<SignWrapper>> signs = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<SignWrapper>() {
            @Override
            public int hashCode(SignWrapper strings) {
                return Arrays.hashCode(strings.rawLines());
            }

            @Override
            public boolean equals(SignWrapper a, SignWrapper b) {
                return SignWrapper.areSignsEqualIgnoreFace(a, b);
            }
        });
        // Remember the locations for the event!
        Map<SignWrapper, List<MovecraftLocation>> wrapperToLocs = new HashMap<>();

        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = location.toBukkit(craft.getWorld()).getBlock();
            if(!Tag.ALL_SIGNS.isTagged(block.getType())){
                continue;
            }
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                SignWrapper[] wrappersAtLoc = this.getSignWrappers(sign, true);
                if (wrappersAtLoc == null || wrappersAtLoc.length == 0) {
                    continue;
                }
                for (SignWrapper wrapper : wrappersAtLoc) {
                    List<SignWrapper> values = signs.computeIfAbsent(wrapper, (w) -> new ArrayList<>());
                    values.add(wrapper);
                    wrapperToLocs.computeIfAbsent(wrapper, (w) -> new ArrayList<>()).add(location);
                }
            }
        }
        Set<Sign> signsToUpdate = new HashSet<>();
        for(Map.Entry<SignWrapper, List<SignWrapper>> entry : signs.entrySet()){
            final List<Component> components = new ArrayList<>(entry.getKey().lines());
            SignWrapper backingForEvent = new SignWrapper(null, null, components::get, components, components::set, entry.getKey().facing());
            // if(!event.isUpdated()){
            //     continue;
            // }
            // TODO: This is implemented only to fix client caching
            //  ideally we wouldn't do the update and would instead fake it out to the player

            /*System.out.println("New lines: ");
            for (String s : event.rawLines()) {
                System.out.println(" - " + s);
            }
            System.out.println("Old lines: ");
            for (String s : entry.getKey().rawLines()) {
                System.out.println(" - " + s);
            }*/
            AbstractMovecraftSign ams = MovecraftSignRegistry.INSTANCE.getCraftSign(backingForEvent.line(0));
            if (ams != null && ams instanceof AbstractCraftSign acs) {
                if (acs.processSignTranslation(craft, backingForEvent, wrapperToLocs.get(entry.getKey())) && (!checkEventIsUpdated || !SignWrapper.areSignsEqualIgnoreFace(backingForEvent, entry.getKey()))) {
                    // Values get changed definitely, but perhaps it does not get applied to the sign after all?
                    for(SignWrapper wrapperTmp : entry.getValue()){
                        wrapperTmp.copyContent(backingForEvent::line, (i) -> i < backingForEvent.lines().size());
                        if (wrapperTmp.block() != null) {
                            //signsToUpdate.add(wrapperTmp.block());
                            acs.sendUpdatePacket(craft, wrapperTmp, AbstractInformationSign.REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT);
                        }
                    }
                }
            }
            // Not a signhandler sing but it has content, so send an update anyway
            else if (ams != null) {
                for(SignWrapper wrapperTmp : entry.getValue()){
                    wrapperTmp.copyContent(backingForEvent::line, (i) -> i < backingForEvent.lines().size());
                    if (wrapperTmp.block() != null) {
                        //signsToUpdate.add(wrapperTmp.block());
                        ams.sendUpdatePacket(craft, wrapperTmp, AbstractInformationSign.REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT);
                    }
                }
            } else {
                entry.getValue().forEach(AbstractMovecraftSign::sendUpdatePacketRaw);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onCraftDetect(CraftDetectEvent event) {
        final World world = event.getCraft().getWorld();
        final Craft craft = event.getCraft();

        executeForAllCraftSigns(craft, (acs, wrapper) -> acs.onCraftDetect(event, wrapper));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignTranslate(SignTranslateEvent event) {
        // Would be one more readable line if using Optionals but Nullables were wanted
        AbstractCraftSign acs = MovecraftSignRegistry.INSTANCE.getCraftSign(event.line(0));
        if (acs != null) {
            acs.onSignMovedByCraft(event);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            // Create the wrapper from the event, not the side! Otherwise the line() methods from the wrapper or the event won't really do anything
            SignWrapper wrapper = this.createForEditEvent(sign, event);

            boolean onCraft = MathUtils.getCraftByPersistentBlockData(sign.getLocation()) != null;

            AbstractMovecraftSign.EventType eventType = wrapper.isEmpty() ? AbstractMovecraftSign.EventType.SIGN_CREATION : AbstractMovecraftSign.EventType.SIGN_EDIT;
            if (eventType == AbstractMovecraftSign.EventType.SIGN_EDIT && onCraft) {
                eventType = AbstractMovecraftSign.EventType.SIGN_EDIT_ON_CRAFT;
            }
            final AbstractMovecraftSign.EventType eventTypeTmp = eventType;


            // If the side is empty, which means the sign was cleared, we do not need to search for a different side!
            if (wrapper.isEmpty()) {
                return;
            }

            final AbstractMovecraftSign ams = MovecraftSignRegistry.INSTANCE.get(wrapper.line(0));

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
        if (!Tag.ALL_SIGNS.isTagged(block.getType())) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof Sign sign) {
            SignWrapper wrapper = this.getSignWrapper(sign, event);
            ItemStack heldItem = event.getItem();
            if (heldItem == null) {
                heldItem = event.getHand() == EquipmentSlot.HAND ? event.getPlayer().getInventory().getItem(EquipmentSlot.OFF_HAND) : event.getPlayer().getInventory().getItem(EquipmentSlot.HAND);
            }
            boolean onCraft = MathUtils.getCraftByPersistentBlockData(sign.getLocation()) != null;

            AbstractMovecraftSign ams = MovecraftSignRegistry.INSTANCE.get(wrapper.line(0));

            // If the side is empty, we should try a different side, like, the next side that is not empty and which has a signHandler
            // If we found no handler for that side, it is practically empty, so we need to check for the other side to avoid breaking stuff
            if (wrapper.isEmpty() || ams == null) {
                SignWrapper[] wrapps = this.getSignWrappers(sign, true);
                if (wrapps == null || wrapps.length == 0) {
                    // Nothing found
                    if (onCraft) {
                        event.setCancelled(true);
                    }
                    return;
                } else {
                    for (SignWrapper swTmp : wrapps) {
                        AbstractMovecraftSign amsTmp = MovecraftSignRegistry.INSTANCE.get(swTmp.line(0));
                        if (amsTmp != null) {
                            wrapper = swTmp;
                            ams = amsTmp;
                            break;
                        }
                    }
                }
            }

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
                if (Tags.SIGN_BYPASS_RIGHT_CLICK.contains(heldItem.getType()) && (event.getAction().isRightClick())) {
                    return;
                }
                if (sneaking && Tags.SIGN_BYPASS_LEFT_CLICK.contains(heldItem.getType()) && (event.getAction().isLeftClick())) {
                    return;
                }
            }

            if (ams == null) {
                ams = MovecraftSignRegistry.INSTANCE.get(wrapper.line(0));
            }

            // Would be one more readable line if using Optionals but Nullables were wanted
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

    public void onStatusUpdate(final Craft craft) {
        if (craft == null) {
            return;
        }

        executeForAllCraftSigns(craft, (acs, wrapper) -> acs.onCraftStatusUpdate(craft, wrapper));
    }

    @EventHandler
    public void onCraftStopCruising(final CraftStopCruiseEvent event) {
        if (event.getCraft() == null || (event.getCraft() instanceof SinkingCraft)) {
            return;
        }

        final Craft craft = event.getCraft();

        executeForAllCraftSigns(craft, (acs, wrapper) -> {
            acs.onCraftStopCruising(craft, wrapper, event.getReason());
        });
    }

    void executeForAllCraftSigns(final @NotNull Craft craft, BiConsumer<AbstractCraftSign, SignWrapper> functionToRun) {
        final World world = craft.getWorld();
        craft.getHitBox().forEach(
                (mloc) -> {
                    Block block = mloc.toBukkit(world).getBlock();
                    if (Tag.ALL_SIGNS.isTagged(block.getType())) {
                        BlockState state = block.getState();
                        if (state instanceof Sign sign) {
                            for (SignWrapper wrapper : this.getSignWrappers(sign)) {
                                AbstractCraftSign acs = MovecraftSignRegistry.INSTANCE.getCraftSign(wrapper.line(0));
                                if (acs != null) {
                                    functionToRun.accept(acs, wrapper);
                                }
                            }
                        }
                    }
                }
        );
    }
}
