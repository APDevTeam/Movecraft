package net.countercraft.movecraft.util;

import net.countercraft.movecraft.localisation.I18nSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.bukkit.util.ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT;

public class TopicPaginator {
    private String title;
    private List<String> lines = new ArrayList<>();
    private final boolean sorted;

    public TopicPaginator(String title, boolean sorted){
        this.title = title;
        this.sorted = sorted;
    }

    public TopicPaginator(String title){
        this(title, true);
    }

    public boolean addLine(String line){
        boolean result = lines.add(line);
        if(sorted)
            Collections.sort(lines);
        return result;
    }

    /**
     * Page numbers begin at 1
     * @param pageNumber
     * @return An array of lines to send as a page
     */
    public String[] getPage(int pageNumber){
        if(!isInBounds(pageNumber))
            throw new IndexOutOfBoundsException(I18nSupport.getInternationalisedString("Paginator - Page Number")+ " " + pageNumber + " "+ I18nSupport.getInternationalisedString("Paginator - Exceeds Bounds") + "<1, " + getPageCount() + ">");
        String[] tempLines = new String[pageNumber == getPageCount() ? (lines.size()%(CLOSED_CHAT_PAGE_HEIGHT-1)) + 1 : CLOSED_CHAT_PAGE_HEIGHT];
        tempLines[0] = "§e§l--- §r§6" + title +" §e§l-- §r§6page §c" + pageNumber + "§e/§c" + getPageCount() + " §e§l---";
        for(int i = 1; i< tempLines.length; i++)
            tempLines[i] = lines.get(((CLOSED_CHAT_PAGE_HEIGHT-1) * (pageNumber-1)) + i - 1);
        return tempLines;
    }

    public int getPageCount(){
        return (int)Math.ceil(((double)lines.size())/(CLOSED_CHAT_PAGE_HEIGHT-1));
    }

    public boolean isInBounds(int pageNumber){
        return pageNumber > 0 && pageNumber <= getPageCount();
    }

    public boolean isEmpty(){
        return lines.isEmpty();
    }
}