package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SignTranslateEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final List<MovecraftLocation> locations;
    @NotNull private String[] lines;

    public SignTranslateEvent(@NotNull Craft craft, @NotNull String[] lines, @NotNull List<MovecraftLocation> locations) throws IndexOutOfBoundsException{
        super(craft);
        this.locations = locations;
        if(lines.length!=4)
            throw new IndexOutOfBoundsException();
        this.lines=lines;
    }

    @NotNull
    public String[] getLines() {
        return lines;
    }

    public String getLine(int index) throws IndexOutOfBoundsException{
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        return lines[index];
    }

    public void setLine(int index, String line){
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        lines[index]=line;
    }

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
}
