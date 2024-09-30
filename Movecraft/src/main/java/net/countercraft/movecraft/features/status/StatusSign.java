package net.countercraft.movecraft.features.status;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.sign.AbstractInformationSign;
import net.countercraft.movecraft.sign.SignListener;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// TODO: Split this into multiple signs? Separate sign for fuel would make sense
public class StatusSign extends AbstractInformationSign {

    List<Component> displayComponents = new ObjectArrayList<>();

    protected static final int FUEL_LINE_INDEX = 3;
    protected static final int BLOCK_LINE_INDEX_TOP = 1;
    protected static final int BLOCK_LINE_INDEX_BOTTOM = 2;

    @Override
    public void onCraftDetect(CraftDetectEvent event, SignListener.SignWrapper sign) {
        // Icky hack to supply the craft with the status values
        long lastStatusUpdate = event.getCraft().getDataTag(StatusManager.LAST_STATUS_CHECK);
        if (lastStatusUpdate == System.currentTimeMillis()) {
            StatusManager.StatusUpdateTask updateTask = new StatusManager.StatusUpdateTask(event.getCraft());
            updateTask.get();
        }

        super.onCraftDetect(event, sign);
    }

    @Override
    protected @Nullable Component getUpdateString(int lineIndex, Component oldData, Craft craft) {
        switch(lineIndex) {
            case FUEL_LINE_INDEX:
                return calcFuel(craft);
            case BLOCK_LINE_INDEX_TOP:
                return displayComponents.get(0);
            case BLOCK_LINE_INDEX_BOTTOM:
                return displayComponents.get(1);
        }
        return oldData;
    }

    // Yes, trillion is ridiculous, anyway...
    final char[] NUMBER_SIZE_MARKERS = {'K', 'M', 'B', 'T'};

    protected Component calcFuel(Craft craft) {
        double fuel = craft.getDataTag(Craft.FUEL);
        int cruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, craft.getWorld());
        cruiseSkipBlocks++;
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        int fuelRange = (int) Math.round((fuel * (1 + cruiseSkipBlocks)) / fuelBurnRate);
        // DONE: Create constants in base class for style colors!
        Style style;
        if (fuelRange > 1000) {
            style = STYLE_COLOR_GREEN;
        } else if (fuelRange > 100) {
            style = STYLE_COLOR_YELLOW;
        } else {
            style = STYLE_COLOR_RED;
        }

        // Shorten for large numbers, or trim the number (e.g. 10k instead of 10.000)
        String fuelString = "" + fuelRange;
        int numberSuffixIndex = 0;
        while (numberSuffixIndex < NUMBER_SIZE_MARKERS.length && fuelRange > 1000) {
            fuelRange /= 1000;
            fuelString = "" + fuelRange + NUMBER_SIZE_MARKERS[numberSuffixIndex];
            numberSuffixIndex++;
        }
        return Component.text("Fuel: " + fuelString).style(style);
    }

    @Override
    protected @Nullable Component getDefaultString(int lineIndex, Component oldComponent) {
        return EMPTY;
    }

    @Override
    protected boolean refreshSign(@Nullable Craft craft, SignListener.SignWrapper sign, boolean fillDefault, REFRESH_CAUSE refreshCause) {
        // Calculate blocks and store them temporary, not pretty but works!
        calcdisplayComponents(craft);
        // Access violation prevention
        while(displayComponents.size() < 2) {
            displayComponents.add(EMPTY);
        }
        return super.refreshSign(craft, sign, fillDefault, refreshCause);
    }

    protected void calcdisplayComponents(@Nullable Craft craft) {
        displayComponents.clear();

        if (craft == null) {
            return;
        }

        int totalNonNegligibleBlocks = 0;
        int totalNonNegligibleWaterBlocks = 0;
        Counter<Material> materials = craft.getDataTag(Craft.MATERIALS);
        if (materials.isEmpty()) {
            return;
        }
        for (Material material : materials.getKeySet()) {
            if (material.equals(Material.FIRE) || material.isAir())
                continue;

            int add = materials.get(material);
            totalNonNegligibleBlocks += add;
            if (!Tags.WATER.contains(material)) {
                totalNonNegligibleWaterBlocks += add;
            }
        }

        Counter<RequiredBlockEntry> displayBlocks = new Counter<>();
        // TODO: Extend to allow definition of tags in craft file
        displayBlocks.add(craft.getDataTag(Craft.FLYBLOCKS));
        displayBlocks.add(craft.getDataTag(Craft.MOVEBLOCKS));

        // TODO: Refactor loop into own method
        int signLine = 1;
        int signColumn = 0;
        for (RequiredBlockEntry entry : displayBlocks.getKeySet()) {
            if (entry.getMin() == 0.0) {
                continue;
            }
            double percentPresent = (displayBlocks.get(entry) * 100D);
            if (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER)) {
                percentPresent /= totalNonNegligibleBlocks;
            } else {
                percentPresent /= totalNonNegligibleWaterBlocks;
            }

            String text = "";
            if (entry.getName() == null) {
                text += entry.materialsToString().toUpperCase().charAt(0);
            } else {
                text += entry.getName().toUpperCase().charAt(0);
            }
            text += " ";
            // Round to 2 digits to better reflect the actual situation
            text += String.format("%.2f", percentPresent);
            text += "/";
            text += (int) entry.getMin();

            Style style;
            if (percentPresent > entry.getMin() * 1.04) {
                style = STYLE_COLOR_GREEN;
            } else if (percentPresent > entry.getMin() * 1.02) {
                style = STYLE_COLOR_YELLOW;
            } else {
                style = STYLE_COLOR_YELLOW;
            }
            Component signText = Component.text(text).style(style);
            if (signColumn == 0) {
                displayComponents.add(signText);
                signColumn++;
                // TODO: Specific case for normal and hanging signs
                // Switch to the next line if the string is too long (will happen in every case now, especially on hanging signs => Always do this for hanging signs
                // For normal signs => Maybe discard the /xx suffix?
                // ACtually it can fit 10 chars, but if the values are like 1.XX / X it will think it fits
                if (text.length() >= 8) {
                    signLine++;
                    signColumn = 0;
                }
            } else if (signLine < 3) {
                Component existingLine = displayComponents.get(signLine - 1);
                existingLine = existingLine.append(Component.text("  ")).append(signText);
                displayComponents.set((signLine - 1), existingLine);
                
                signLine++;
                signColumn = 0;
            }
        }
    }

    @Override
    protected void performUpdate(Component[] newComponents, SignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {
        for (int i = 0; i < newComponents.length; i++) {
            Component newComp = newComponents[i];
            if (newComp != null) {
                sign.line(i, newComp);
            }
        }
        if (refreshCause != REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT && sign.block() != null) {
            sign.block().update(true);
        }
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }

}
