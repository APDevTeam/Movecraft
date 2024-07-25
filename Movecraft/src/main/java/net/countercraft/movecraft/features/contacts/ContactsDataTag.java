package net.countercraft.movecraft.features.contacts;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.datatag.ICraftDataTag;

import java.util.ArrayList;

public class ContactsDataTag extends ArrayList<Craft> implements ICraftDataTag {
    public ContactsDataTag(int size) {
        super(size);
    }

    @Override
    public void onRotate(Craft craft) {

    }

    @Override
    public void onSwitchWorld(Craft craft) {

    }

    @Override
    public void onRelease(Craft craft) {

    }

    @Override
    public void onAssembly(Craft craft) {

    }

    @Override
    public void onTranslate(Craft craft) {

    }
}
