package net.countercraft.movecraft.craft.controller.rotation;

import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.controller.AbstractRotationController;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.sign.SignListener;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// Standard implementation with instant 90° turns
public class DefaultRotationController extends AbstractRotationController {

    public DefaultRotationController() {
        super(Map.of());
    }

    public DefaultRotationController(Map<String, Object> rawData) {
        super(rawData);
    }

    @Override
    public boolean onHelmInteraction(Craft craft, SignListener.SignWrapper signWrapper, Action clickType, final Player interactor) {
        if (!super.onHelmInteraction(craft, signWrapper, clickType, interactor)) {
            return false;
        }
        MovecraftRotation rotation;
        if (clickType == Action.RIGHT_CLICK_BLOCK) {
            rotation = MovecraftRotation.CLOCKWISE;
        }else if(clickType == Action.LEFT_CLICK_BLOCK){
            rotation = MovecraftRotation.ANTICLOCKWISE;
        }else{
            return false;
        }

        if (craft.getCraftProperties().get(PropertyKeys.ROTATE_AT_MIDPOINT) || signWrapper == null) {
            return craft.rotate(rotation, craft.getHitBox().getMidPoint());
        } else if (signWrapper.block() != null) {
            return craft.rotate(rotation, MathUtils.bukkit2MovecraftLoc(signWrapper.block().getLocation()));
        }
        return false;
    }

    @Override
    public @NotNull Map<String, Object> addToSerialize(@NotNull Map<String, Object> serialized) {
        return serialized;
    }

    @Override
    public AbstractRotationController clone() {
        return new DefaultRotationController(this.serialize());
    }

}
