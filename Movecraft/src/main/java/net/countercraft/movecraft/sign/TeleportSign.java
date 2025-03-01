package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public class TeleportSign extends MoveSign {

    public TeleportSign() {
        super();
    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        if (!super.isSignValid(clickType, sign, player)) {
            return false;
        }
        // Check world => If specified it has to exist!
        String w = sign.getRaw(2);
        if (!w.isBlank()) {
            World world = Bukkit.getWorld(w);
            return world != null;
        }
        return true;
    }

    @Override
    protected boolean translateCraft(byte signDataRaw, int dxRaw, int dyRaw, int dzRaw, Craft craft, SignListener.SignWrapper signWrapper) {
        World world = craft.getWorld();
        String w = signWrapper.getRaw(2);
        if (!w.isBlank()) {
            world = Bukkit.getWorld(w);
        }

        // Substract the signs location so we get a vector
        int dx = dxRaw - signWrapper.block().getX();
        int dy = dyRaw - signWrapper.block().getY();
        int dz = dzRaw - signWrapper.block().getZ();
        craft.translate(world, dx, dy, dz);
        craft.setLastTeleportTime(System.currentTimeMillis());

        return true;
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            if (craft.getType().getBoolProperty(CraftType.CAN_TELEPORT)) {
                long timeSinceLastTeleport = System.currentTimeMillis() - craft.getLastTeleportTime();
                if (craft.getType().getIntProperty(CraftType.TELEPORTATION_COOLDOWN) > 0 && timeSinceLastTeleport < craft.getType().getIntProperty(CraftType.TELEPORTATION_COOLDOWN)) {
                    player.sendMessage(String.format(I18nSupport.getInternationalisedString("Teleportation - Cooldown active"), timeSinceLastTeleport));
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
