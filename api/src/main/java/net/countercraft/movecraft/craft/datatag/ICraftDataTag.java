package net.countercraft.movecraft.craft.datatag;

import net.countercraft.movecraft.craft.Craft;

public interface ICraftDataTag {

    public void onRotate(final Craft craft);
    public void onSwitchWorld(final Craft craft);
    public void onRelease(final Craft craft);
    public void onAssembly(final Craft craft);
    public void onTranslate(final Craft craft);
}
