package net.countercraft.movecraft.api.events;

import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SignTranslateEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull
    private final Block block;
    @NotNull private String[] lines;

    public SignTranslateEvent(@NotNull Block block, @NotNull Craft craft, @NotNull String[] lines) throws IndexOutOfBoundsException{
        super(craft);
        this.block = block;
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
    public Block getBlock() {
        return block;
    }
}
