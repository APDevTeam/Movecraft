package net.countercraft.movecraft.compat.v1_21;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.sign.AbstractSignListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/*
 * As soon as 1.18 support is dropped, the adapter system will be dropped too
 */
@Deprecated(forRemoval = true)
public class SignListener extends AbstractSignListener {

    protected final SignWrapper createFromSide(final Sign sign, final Side side) {
        SignSide signSide = sign.getSide(side);
        return createFromSide(sign, signSide, side);
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
                signSide::line,
                signSide.lines(),
                signSide::line,
                face
        );
        return wrapper;
    }

    @Override
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

    @Override
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
                signChangeEvent::line,
                signChangeEvent.lines(),
                signChangeEvent::line,
                face
        );
        return wrapper;
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent) {
        @NotNull SignSide side = sign.getTargetSide(interactEvent.getPlayer());
        return this.createFromSide(sign, side, sign.getInteractableSideFor(interactEvent.getPlayer()));
    }

    @Override
    public void processSignTranslation(Craft craft, boolean checkEventIsUpdated) {
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
            if(!Tag.SIGNS.isTagged(block.getType())){
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
        Set<SignWrapper> keySet = new HashSet<>();
        for(Map.Entry<SignWrapper, List<SignWrapper>> entry : signs.entrySet()){
            final List<Component> components = new ArrayList<>(entry.getKey().lines());
            SignWrapper backingForEvent = new SignWrapper(null, components::get, components, components::set, entry.getKey().facing());
            SignTranslateEvent event = new SignTranslateEvent(craft, backingForEvent, wrapperToLocs.getOrDefault(entry.getKey(), new ArrayList<>()));
            Bukkit.getServer().getPluginManager().callEvent(event);
            // if(!event.isUpdated()){
            //     continue;
            // }
            // TODO: This is implemented only to fix client caching
            //  ideally we wouldn't do the update and would instead fake it out to the player

            System.out.println("New lines: ");
            for (String s : event.rawLines()) {
                System.out.println(" - " + s);
            }
            System.out.println("Old lines: ");
            for (String s : entry.getKey().rawLines()) {
                System.out.println(" - " + s);
            }
            // Values get changed definitely, but perhaps it does not get applied to the sign after all?
            for(SignWrapper wrapperTmp : entry.getValue()){
                /*Block block = location.toBukkit(craft.getWorld()).getBlock();
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    continue;
                }
                SignWrapper[] signsAtLoc = signStates.get(location);
                if (signsAtLoc != null && signsAtLoc.length > 0) {
                    boolean hadCorrectSide = false;
                    for (SignWrapper sw : signsAtLoc) {
                        // Important: Check if the wrapper faces the right way!
                        if (!sw.facing().equals(event.facing())) {
                            continue;
                        }
                        hadCorrectSide = true;
                        if (!checkEventIsUpdated || event.isUpdated()) {
                            sw.copyContent(event::line, (i) -> i < event.lines().size());
                        }
                    }
                    if (hadCorrectSide) {
                        try {
                            ((Sign)location.toBukkit(craft.getWorld()).getBlock()).update(false, false);
                        } catch(ClassCastException ex) {
                            // Ignore
                        }
                    }
                }*/
                if (!checkEventIsUpdated || event.isUpdated()) {
                    wrapperTmp.copyContent(event::line, (i) -> i < event.lines().size());
                    keySet.add(entry.getKey());
                }
            }
        }

        for (SignWrapper wrapperTmp : keySet) {
            for (MovecraftLocation mLoc : wrapperToLocs.getOrDefault(wrapperTmp, new ArrayList<>())) {
                Block block = mLoc.toBukkit(craft.getWorld()).getBlock();
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    continue;
                }
                ((Sign)state).update(false, false);
            }
        }
    }
}
