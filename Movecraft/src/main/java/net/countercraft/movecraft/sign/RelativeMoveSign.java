package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;

public class RelativeMoveSign extends MoveSign {

    public RelativeMoveSign() {
        super();
    }

    @Override
    protected boolean translateCraft(byte signDataRaw, int dxRaw, int dyRaw, int dzRaw, Craft craft, SignListener.SignWrapper signWrapper) {
        final int maxMove = craft.getType().getIntProperty(CraftType.MAX_STATIC_MOVE);

        // X: Left/Right
        // Y: Up/Down
        // Z: Forward/Backward

        if (dxRaw > maxMove)
            dxRaw = maxMove;
        if (dxRaw < -maxMove)
            dxRaw = -maxMove;
        if (dyRaw > maxMove)
            dyRaw = maxMove;
        if (dyRaw < -maxMove)
            dyRaw = -maxMove;
        if (dzRaw > maxMove)
            dzRaw = maxMove;
        if (dzRaw < -maxMove)
            dzRaw = -maxMove;
        int dx = 0;
        int dz = 0;
        switch (signDataRaw) {
            case 0x3:
                // North
                dx = dxRaw;
                dz = -dzRaw;
                break;
            case 0x2:
                // South
                dx = -dxRaw;
                dz = dzRaw;
                break;
            case 0x4:
                // East
                dx = dzRaw;
                dz = dxRaw;
                break;
            case 0x5:
                // West
                dx = -dzRaw;
                dz = -dxRaw;
                break;
        }

        craft.translate(dx, dyRaw, dz);
        //timeMap.put(event.getPlayer(), System.currentTimeMillis());
        craft.setLastCruiseUpdate(System.currentTimeMillis());

        return true;
    }
}
