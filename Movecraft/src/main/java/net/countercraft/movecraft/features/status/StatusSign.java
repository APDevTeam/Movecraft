package net.countercraft.movecraft.features.status;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.countercraft.movecraft.async.FuelBurnRunnable;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.sign.AbstractInformationSign;
import net.countercraft.movecraft.sign.SignListener;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
            event.getCraft().setDataTag(StatusManager.LAST_STATUS_CHECK, 0l);
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
    static final Style STYLE_COLOR_GRAY = Style.style(TextColor.color(69, 69, 69));
    static final Style STYLE_COLOR_BLACK = Style.style(TextColor.color(0, 0, 0));
    static final Component FUEL_PREFIX = Component.text("[").style(STYLE_COLOR_GRAY);
    static final Component FUEL_SUFFIX = Component.text("]").style(STYLE_COLOR_GRAY);
    static final Component FUEL_EMPTY = Component.text("-EMPTY-").style(STYLE_COLOR_RED.decorate(TextDecoration.BOLD));
    static final int CELL_COUNT = 20;

    protected Component calcFuel(Craft craft) {
        // If we dont burn any fuel, we can quit early
        if (!FuelBurnRunnable.doesBurnFuel(craft)) {
            return null;
        }

        // Since our fuel burn rate varies, we will just display how full our tank is instead...
        final double fuelLevel = craft.getDataTag(FuelBurnRunnable.FUEL_PERCENTAGE);
        int cells = (int) Math.round(fuelLevel * ((double) CELL_COUNT));
        Component result = Component.text("Fuel: ");
        result = result.append(FUEL_PREFIX);
        if (cells > 0) {
            Style style;
            if (cells > 6) {
                style = STYLE_COLOR_GREEN;
            } else if (cells > 3) {
                style = STYLE_COLOR_YELLOW;
            } else {
                style = STYLE_COLOR_RED;
            }
            // Create component that represents the fuel level (in 5% steps!)
            // Necessary, sign class only updates sign if the raw strings are different!
            String stringTmp = "";
            char charTmp = '|';
            for (int i = 0; i < CELL_COUNT; i++) {
                stringTmp += charTmp;
               if (i + 1 == cells && (CELL_COUNT != cells)) {
                   result = result.append(Component.text(stringTmp).style(style));
                   style = STYLE_COLOR_BLACK;
                   stringTmp = "";
                   charTmp = '¦';
               }
            }
            result = result.append(Component.text(stringTmp).style(style));
        } else {
            result = result.append(FUEL_EMPTY);
        }
        result = result.append(FUEL_SUFFIX);

        return result;

        // Old logic
//        double fuel = craft.getDataTag(Craft.FUEL);
//        int cruiseSkipBlocks = craft.getCraftProperties().get(PropertyKeys.CRUISE_SKIP_BLOCKS, craft.getWorld());
//        cruiseSkipBlocks++;
//        double fuelBurnRate = craft.getCraftProperties().get(PropertyKeys.FUEL_BURN_RATE, craft.getWorld());
//        int fuelRange = (int) Math.round((fuel * (1 + cruiseSkipBlocks)) / fuelBurnRate);
//        // DONE: Create constants in base class for style colors!
//        Style style;
//        if (fuelRange > 1000) {
//            style = STYLE_COLOR_GREEN;
//        } else if (fuelRange > 100) {
//            style = STYLE_COLOR_YELLOW;
//        } else {
//            style = STYLE_COLOR_RED;
//        }
//
//        // Shorten for large numbers, or trim the number (e.g. 10k instead of 10.000)
//        String fuelString = "" + fuelRange;
//        int numberSuffixIndex = 0;
//        while (numberSuffixIndex < NUMBER_SIZE_MARKERS.length && fuelRange > 1000) {
//            fuelRange /= 1000;
//            fuelString = "" + fuelRange + NUMBER_SIZE_MARKERS[numberSuffixIndex];
//            numberSuffixIndex++;
//        }
//        return Component.text("Fuel: " + fuelString).style(style);
    }

    @Override
    protected @Nullable Component getDefaultString(int lineIndex, Component oldComponent) {
        return EMPTY;
    }

    @Override
    protected boolean refreshSign(@Nullable Craft craft, SignListener.SignWrapper sign, boolean fillDefault, REFRESH_CAUSE refreshCause) {
        // Calculate blocks and store them temporary, not pretty but works!
        calcDisplayComponents(craft);
        // Access violation prevention
        while(displayComponents.size() < 2) {
            displayComponents.add(EMPTY);
        }
        return super.refreshSign(craft, sign, fillDefault, refreshCause);
    }

    protected void calcDisplayComponents(@Nullable Craft craft) {
        displayComponents.clear();

        if (craft == null) {
            return;
        }

        int totalNonNegligibleBlocks = 0;
        int totalNonNegligibleWaterBlocks = 0;
        Counter<NamespacedKey> materials = craft.getDataTag(Craft.BLOCKS);
        if (materials.isEmpty()) {
            return;
        }
        for (NamespacedKey namespacedKey : materials.getKeySet()) {
            Material material = Material.matchMaterial(namespacedKey.toString());
            if (material != null && (material.equals(Material.FIRE) || material.isAir()))
                continue;

            int add = materials.get(namespacedKey);
            totalNonNegligibleBlocks += add;
            if (material != null && !Tags.WATER.contains(material)) {
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
            if (craft.getCraftProperties().get(PropertyKeys.BLOCKED_BY_WATER)) {
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
                style = STYLE_COLOR_RED;
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
