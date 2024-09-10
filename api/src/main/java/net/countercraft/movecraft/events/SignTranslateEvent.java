package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.sign.AbstractSignListener;
import net.kyori.adventure.text.Component;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Rewrite to use the adventure API
public class SignTranslateEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final List<MovecraftLocation> locations;
    @NotNull private final AbstractSignListener.SignWrapper backing;
    private boolean updated = false;

    @Deprecated(forRemoval = true)
    public SignTranslateEvent(@NotNull Craft craft, @NotNull String[] lines, @NotNull List<MovecraftLocation> locations) throws IndexOutOfBoundsException{
        super(craft);
        this.locations = locations;
        List<Component> components = new ArrayList<>();
        for (String s : lines) {
            components.add(Component.text(s));
        }
        this.backing = new AbstractSignListener.SignWrapper(null, components::get, components, components::set, BlockFace.SELF);
    }

    public SignTranslateEvent(@NotNull Craft craft, @NotNull AbstractSignListener.SignWrapper backing, @NotNull List<MovecraftLocation> locations) throws IndexOutOfBoundsException{
        super(craft);
        this.locations = locations;
        this.backing = backing;
    }

    @NotNull
    @Deprecated(forRemoval = true)
    public String[] getLines() {
        // Why does this set it to updated? This is just reading...
        // => Lines can be updated externally. We need to mark all signs as updated so it displays properly on clients
        this.updated = true;
        return backing.rawLines();
    }

    @Deprecated(forRemoval = true)
    public String getLine(int index) throws IndexOutOfBoundsException{
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        return backing.getRaw(index);
    }

    @Deprecated(forRemoval = true)
    public void setLine(int index, String line){
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        this.updated = true;
        backing.line(index, Component.text(line));
    }

    public Component line(int index) {
        return backing.line(index);
    }

    public void line(int index, Component component) {
        this.updated = true;
        backing.line(index, component);
    }

    public String getRaw(int index) {
        return backing.getRaw(index);
    }

    public String[] rawLines() {
        return backing.rawLines();
    }

    public BlockFace facing() {
        return backing.facing();
    }

    public List<Component> lines() {
        return backing.lines();
    }

    // Bukkit
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public List<MovecraftLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public boolean isUpdated() {
        return updated;
    }
}
