package net.countercraft.movecraft.compat.v1_20;

import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.sign.AbstractSignListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SignListener extends AbstractSignListener {

    protected final SignWrapper createFromSide(final Sign sign, final Side side) {
        SignSide signSide = sign.getSide(side);
        return createFromSide(sign, signSide, side);
    }

    protected final SignWrapper createFromSide(final Sign sign, final SignSide signSide, Side side) {
        BlockFace face = ((Directional) sign.getBlockData()).getFacing();
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
    public SignWrapper[] getSignWrappers(Sign sign) {
        Side[] sides = new Side[Side.values().length];
        SignWrapper[] wrappers = new SignWrapper[sides.length];
        for (int i = 0; i < sides.length; i++) {
            Side side = sides[i];
            SignSide signSide = sign.getSide(side);
            SignWrapper wrapper = this.createFromSide(sign, signSide, side);
            wrappers[i] = wrapper;
        }
        return wrappers;
    }

    @Override
    public SignWrapper[] getSignWrappers(Sign sign, SignTranslateEvent event) {
        Side[] sides = new Side[Side.values().length];
        SignWrapper[] wrappers = new SignWrapper[sides.length];
        for (int i = 0; i < sides.length; i++) {
            Side side = sides[i];
            SignSide signSide = sign.getSide(side);
            BlockFace face = ((Directional) sign.getBlockData()).getFacing();
            if (side == Side.BACK) {
                face = face.getOppositeFace();
            }
            List<Component> lines = new ArrayList<>();
            for (int j = 0; j < event.getLines().length; j++) {
                lines.add(Component.text(event.getLine(j)));
            }
            SignWrapper wrapper = new SignWrapper(
                    sign,
                    (k) -> {
                        String valTmp = event.getLine(k);
                        return Component.text(valTmp);
                    },
                    lines,
                    (k, component) -> {
                        event.setLine(k, PlainTextComponentSerializer.plainText().serialize(component));
                    },
                    face
            );
            wrappers[i] = wrapper;
        }
        return wrappers;
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent) {
        @NotNull Side side = signChangeEvent.getSide();
        BlockFace face = ((Directional) sign.getBlockData()).getFacing();
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
}
