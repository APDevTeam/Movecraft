package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.craft.type.TypeSafeCraftType;

/*
 * Base implementation for all craft pilot signs, does nothing but has the relevant CraftType instance backed
 */
public abstract class AbstractCraftPilotSign extends AbstractMovecraftSign {

    protected final TypeSafeCraftType craftType;

    public AbstractCraftPilotSign(final TypeSafeCraftType craftType) {
        super();
        this.craftType = craftType;
    }

    public TypeSafeCraftType getCraftType() {
        return this.craftType;
    }

}
