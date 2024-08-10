package net.countercraft.movecraft.compat.v1_21;

import net.countercraft.movecraft.sign.AbstractSignListener;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

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
    protected SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent) {
        @NotNull Side side = signChangeEvent.getSide();
        return this.createFromSide(sign, side);
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent) {
        @NotNull SignSide side = sign.getTargetSide(interactEvent.getPlayer());
        return this.createFromSide(sign, side, sign.getInteractableSideFor(interactEvent.getPlayer()));
    }
}
