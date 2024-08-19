package net.countercraft.movecraft.features.status;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.sign.AbstractInformationSign;
import net.countercraft.movecraft.sign.AbstractSignListener;
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

    Object2IntMap<RequiredBlockEntry> displayBlocks = new Object2IntOpenHashMap<>();
    List<Component> displayComponents = new ObjectArrayList<>();
    int totalNonNegligibleBlocks = 0;
    int totalNonNegligibleWaterBlocks = 0;

    public static final Component EMPTY = Component.text("");

    protected static final int FUEL_LINE_INDEX = 3;
    protected static final int BLOCK_LINE_INDEX_TOP = 1;
    protected static final int BLOCK_LINE_INDEX_BOTTOM = 2;

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

        return Component.text("Fuel range: " + fuelRange).style(style);
    }

    @Override
    protected @Nullable Component getDefaultString(int lineIndex, Component oldComponent) {
        return EMPTY;
    }

    @Override
    protected void refreshSign(@Nullable Craft craft, AbstractSignListener.SignWrapper sign, boolean fillDefault, REFRESH_CAUSE refreshCause) {
        // Calculate blocks and store them temporary, not pretty but works!
        calcDisplayBlocks(craft);
        calcdisplayComponents(craft);
        super.refreshSign(craft, sign, fillDefault, refreshCause);
    }

    protected void calcDisplayBlocks(Craft craft) {
        displayBlocks.clear();
        displayComponents.clear();

        totalNonNegligibleBlocks = 0;
        totalNonNegligibleWaterBlocks = 0;
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
        Object2IntMap<RequiredBlockEntry> displayBlocks = new Object2IntOpenHashMap<>();
        for (RequiredBlockEntry entry : craft.getType().getRequiredBlockProperty(CraftType.FLY_BLOCKS)) {
            int total = 0;
            for (Material material : entry.getMaterials()) {
                if (materials.getKeySet().contains(material)) {
                    total += materials.get(material);
                }
            }
            displayBlocks.putIfAbsent(entry, total);
        }
        for (RequiredBlockEntry entry : craft.getType().getRequiredBlockProperty(CraftType.MOVE_BLOCKS)) {
            int total = 0;
            for (Material material : entry.getMaterials()) {
                if (materials.getKeySet().contains(material)) {
                    total += materials.get(material);
                }
            }
            displayBlocks.putIfAbsent(entry, total);
        }
    }

    protected void calcdisplayComponents(Craft craft) {
        displayComponents.set(0, EMPTY);
        displayComponents.set(1, EMPTY);
        int signLine = 0;
        int signColumn = 0;
        for (RequiredBlockEntry entry : displayBlocks.keySet()) {
            if (entry.getMin() == 0.0) {
                continue;
            }
            double percentPresent = (displayBlocks.get(entry) * 100D);
            if (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER)) {
                percentPresent /= totalNonNegligibleBlocks;
            } else {
                percentPresent /= totalNonNegligibleWaterBlocks;
            }
            Component signText = EMPTY;
            Style style;
            if (percentPresent > entry.getMin() * 1.04) {
                style = STYLE_COLOR_GREEN;
            } else if (percentPresent > entry.getMin() * 1.02) {
                style = STYLE_COLOR_YELLOW;
            } else {
                style = STYLE_COLOR_RED;
            }
            if (entry.getName() == null) {
                signText = Component.text(entry.materialsToString().toUpperCase().charAt(0));
            } else {
                signText = Component.text(entry.getName().toUpperCase().charAt(0));
            }
            signText = signText.append(Component.text(" " + (int)percentPresent + "/" + (int)entry.getMin() + " "));
            signText = signText.style(style);
            if (signColumn == 0) {
                displayComponents.set(signLine, signText);
                signColumn++;
            } else if (signLine < 2) {
                Component existingLine = displayComponents.get(signLine);
                existingLine = existingLine.append(signText);
                displayComponents.set(signLine, existingLine);
                signLine++;
                signColumn = 0;
            }
        }
    }

    @Override
    protected void performUpdate(Component[] newComponents, AbstractSignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {
        if (refreshCause != REFRESH_CAUSE.SIGN_MOVED_BY_CRAFT) {
            sign.block().update(true);
        }
    }

    @Override
    protected void sendUpdatePacket(Craft craft, AbstractSignListener.SignWrapper sign, REFRESH_CAUSE refreshCause) {

    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }
}
