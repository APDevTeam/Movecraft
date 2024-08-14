package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.type.CraftType;

public abstract class AbstractCraftPilotSign extends AbstractMovecraftSign {

    protected final CraftType craftType;

    public AbstractCraftPilotSign(final CraftType craftType) {
        super();
        this.craftType = craftType;
    }

}
