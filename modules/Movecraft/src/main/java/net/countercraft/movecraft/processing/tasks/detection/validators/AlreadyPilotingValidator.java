package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class AlreadyPilotingValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> ignored, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player) {
        return player instanceof Player && CraftManager.getInstance().getCraftByPlayer((Player) player) != null ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Failed - Already commanding a craft")) : Result.succeed();
    }
}
