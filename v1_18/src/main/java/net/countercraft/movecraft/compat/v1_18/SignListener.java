package net.countercraft.movecraft.compat.v1_18;

import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.sign.AbstractSignListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

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
    public SignWrapper[] getSignWrappers(Sign sign, SignTranslateEvent event) {
        SignWrapper[] wrappers = new SignWrapper[1];
        for (int i = 0; i < wrappers.length; i++) {
            BlockFace face = ((Directional) sign.getBlockData()).getFacing();
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
        BlockFace face = ((Directional) sign.getBlockData()).getFacing();
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
}
