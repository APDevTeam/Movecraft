package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;

public class MoveSign extends AbstractCraftSign {

    public MoveSign() {
        super(null, false);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        player.sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {

    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        String[] numbers = sign.getRaw(1).split(",");
        if (numbers.length != 3) {
            return false;
        }
        for (String s : numbers) {
            try {
                Integer.parseInt(s);
            } catch(NumberFormatException nfe) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        return false;
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        if (!super.canPlayerUseSignOn(player, craft)) {
            return false;
        }
        if (!player.hasPermission("movecraft." + craft.getCraftProperties().getName().toLowerCase() + ".move")) {
            player.sendMessage(
                    I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return false;
        }
        return true;
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        if (!craft.getCraftProperties().get(PropertyKeys.CAN_STATIC_MOVE)) {
            return false;
        }

        String[] numbers = sign.getRaw(1).split(",");
        int dx = Integer.parseInt(numbers[0]);
        int dy = Integer.parseInt(numbers[1]);
        int dz = Integer.parseInt(numbers[2]);

        return translateCraft(sign.block().getRawData(), dx, dy, dz, craft, sign);
    }

    protected boolean translateCraft(final byte signDataRaw, int dxRaw, int dyRaw, int dzRaw, Craft craft, SignListener.SignWrapper signWrapper) {
        int maxMove = craft.getCraftProperties().get(PropertyKeys.MAX_STATIC_MOVE);

        if (dxRaw > maxMove)
            dxRaw = maxMove;
        if (dxRaw < 0 - maxMove)
            dxRaw = 0 - maxMove;
        if (dyRaw > maxMove)
            dyRaw = maxMove;
        if (dyRaw < 0 - maxMove)
            dyRaw = 0 - maxMove;
        if (dzRaw > maxMove)
            dzRaw = maxMove;
        if (dzRaw < 0 - maxMove)
            dzRaw = 0 - maxMove;

        craft.translate(dxRaw, dyRaw, dzRaw);
        //timeMap.put(event.getPlayer(), System.currentTimeMillis());
        craft.setLastCruiseUpdate(System.currentTimeMillis());

        return true;
    }
}
