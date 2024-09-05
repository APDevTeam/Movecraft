package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.type.CraftType;

/*
 * Base implementation for all craft pilot signs, does nothing but has the relevant CraftType instance backed
 */
public abstract class AbstractCraftPilotSign extends AbstractMovecraftSign {

    protected final CraftType craftType;

    public AbstractCraftPilotSign(final CraftType craftType) {
        super();
        this.craftType = craftType;
    }

}
