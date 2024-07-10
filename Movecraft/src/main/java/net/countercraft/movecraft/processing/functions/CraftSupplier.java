package net.countercraft.movecraft.processing.functions;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.functions.QuadFunction;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents a function that accepts the outputs of detection and produces a Craft.
 * @see QuadFunction
 */
@FunctionalInterface
public interface CraftSupplier extends QuadFunction<CraftType, World, Player, Set<Craft>, Pair<Result, Craft>> {
    /**
     * Applies this function to the given arguments.
     *
     * @param type The type of the craft
     * @param w The world of the craft
     * @param player The player supplied to the detection task (may or may not become the pilot, may also be null)
     * @param parents The parent crafts of the craft
     * @return the result of construction and possibly a craft
     */
    @NotNull
    Pair<@NotNull Result, @Nullable Craft> apply(@NotNull CraftType type, @NotNull World w, @Nullable Player player, @NotNull Set<Craft> parents);
}
