package net.countercraft.movecraft.compat.v1_18;

import net.countercraft.movecraft.sign.AbstractSignListener;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener extends AbstractSignListener {

    protected final SignWrapper createFromSign(final Sign sign) {
        BlockFace face = ((Directional) sign.getBlockData()).getFacing();
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
    public SignWrapper[] getSignWrappers(Sign sign) {
        SignWrapper wrapper = this.createFromSign(sign);
        return new SignWrapper[] {wrapper};
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, SignChangeEvent signChangeEvent) {
        return this.createFromSign(sign);
    }

    @Override
    protected SignWrapper getSignWrapper(Sign sign, PlayerInteractEvent interactEvent) {
        return this.createFromSign(sign);
    }
}
