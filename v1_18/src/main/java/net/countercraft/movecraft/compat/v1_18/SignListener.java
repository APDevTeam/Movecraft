package net.countercraft.movecraft.compat.v1_18;

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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

/*
 * As soon as 1.18 support is dropped, the adapter system will be dropped too
 */
@Deprecated(forRemoval = true)
public class SignListener extends AbstractSignListener {

    protected final SignWrapper createFromSign(final Sign sign) {
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
        SignWrapper wrapper = new SignWrapper(
                sign,
                sign::line,
                sign.lines(),
                sign::line,
                face
        );
        return wrapper;
    }

    @Override
    public SignWrapper[] getSignWrappers(Sign sign, boolean ignoreEmpty) {
        SignWrapper wrapper = this.createFromSign(sign);
        if (ignoreEmpty && wrapper.isEmpty()) {
            return new SignWrapper[] {};
        }
        return new SignWrapper[] {wrapper};
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent) {
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
        return this.createFromSign(sign);
    }

    @Override
    public void processSignTranslation(Craft craft, boolean checkEventIsUpdated) {
        Object2ObjectMap<SignWrapper, List<MovecraftLocation>> signs = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<SignWrapper>() {
            @Override
            public int hashCode(SignWrapper strings) {
                return Arrays.hashCode(strings.rawLines());
            }

            @Override
            public boolean equals(SignWrapper a, SignWrapper b) {
                return SignWrapper.areSignsEqual(a, b);
            }
        });
        Map<MovecraftLocation, SignWrapper[]> signStates = new HashMap<>();

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
                    List<MovecraftLocation> values = signs.computeIfAbsent(wrapper, (w) -> new ArrayList<>());
                    values.add(location);
                }
                signStates.put(location, wrappersAtLoc);
            }
        }
        // TODO: This is not good yet, this doesn't really care about a signs sides...
        for(Map.Entry<SignWrapper, List<MovecraftLocation>> entry : signs.entrySet()){
            final List<Component> components = new ArrayList<>(entry.getKey().lines());
            SignWrapper backingForEvent = new SignWrapper(null, components::get, components, components::set, entry.getKey().facing());
            SignTranslateEvent event = new SignTranslateEvent(craft, backingForEvent, entry.getValue());
            Bukkit.getServer().getPluginManager().callEvent(event);
            // if(!event.isUpdated()){
            //     continue;
            // }
            // TODO: This is implemented only to fix client caching
            //  ideally we wouldn't do the update and would instead fake it out to the player
            for(MovecraftLocation location : entry.getValue()){
                Block block = location.toBukkit(craft.getWorld()).getBlock();
                BlockState state = block.getState();
                if (!(state instanceof Sign)) {
                    continue;
                }
                SignWrapper[] signsAtLoc = signStates.get(location);
                if (signsAtLoc != null && signsAtLoc.length > 0) {
                    for (SignWrapper sw : signsAtLoc) {
                        if (!checkEventIsUpdated || event.isUpdated()) {
                            sw.copyContent(entry.getKey());
                        }
                    }
                    try {
                        ((Sign)location.toBukkit(craft.getWorld()).getBlock().getState()).update(false, false);
                    } catch(ClassCastException ex) {
                        // Ignore
                    }
                }
            }
        }
    }

}
