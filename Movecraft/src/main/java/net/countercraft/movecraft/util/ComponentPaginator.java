package net.countercraft.movecraft.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.bukkit.util.ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT;

public class ComponentPaginator {
    private final Component title;
    private final List<Component> lines = new ArrayList<>();
    private final Function<Integer, String> pageConsumer;

    public ComponentPaginator(@NotNull Component title, @NotNull Function<Integer, String> pageConsumer) {
        this.title = title.color(NamedTextColor.GOLD);
        this.pageConsumer = pageConsumer;
    }

    public void addLine(Component line){
        lines.add(line);
    }

    /**
     * Page numbers begin at 1
     * @param pageNumber
     * @return An array of lines to send as a page
     */
    public Component[] getPage(int pageNumber) {
        if (!isInBounds(pageNumber))
            throw new IndexOutOfBoundsException();

        Component[] tempLines = new Component[pageNumber == getPageCount() ? (lines.size() % (CLOSED_CHAT_PAGE_HEIGHT - 1)) + 1 : CLOSED_CHAT_PAGE_HEIGHT];

        tempLines[0] = Component.text("--- ", NamedTextColor.YELLOW);
        if (isInBounds(pageNumber - 1)) {
            tempLines[0] = tempLines[0].append(Component.text("[<] ")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(
                            pageConsumer.apply(pageNumber - 1))));
        }
        tempLines[0] = tempLines[0]
                .append(title)
                .append(Component.text(" -- ", NamedTextColor.YELLOW))
                .append(Component.text("page ", NamedTextColor.GOLD))
                .append(Component.text(pageNumber, NamedTextColor.RED))
                .append(Component.text("/", NamedTextColor.YELLOW))
                .append(Component.text(getPageCount(), NamedTextColor.RED));
        if (isInBounds(pageNumber + 1)) {
            tempLines[0] = tempLines[0].append(Component.text(" [>]")
                    .color(NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(
                            pageConsumer.apply(pageNumber + 1))));
        }
        tempLines[0] = tempLines[0]
                .append(Component.text(" ---", NamedTextColor.YELLOW));

        for (int i = 1; i < tempLines.length; i++) {
            tempLines[i] = lines.get(((CLOSED_CHAT_PAGE_HEIGHT - 1) * (pageNumber - 1)) + i - 1);
        }
        return tempLines;
    }

    public int getPageCount() {
        return (int) Math.ceil(((double) lines.size()) / (CLOSED_CHAT_PAGE_HEIGHT - 1));
    }

    public boolean isInBounds(int pageNumber) {
        return pageNumber > 0 && pageNumber <= getPageCount();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
