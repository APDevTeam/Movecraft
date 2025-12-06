package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SerializableAs("ConfiguredSound")
public record ConfiguredSound(
        String sound,
        SoundCategory category,
        float minVolume,
        float maxVolume,
        float minPitch,
        float maxPitch
) implements ConfigurationSerializable {

    @Nullable
    public static ConfiguredSound deserialize(Map<String, Object> rawData) {
        try {
            String sound = (String) rawData.getOrDefault("Sound", Sound.BLOCK_ANVIL_USE.toString());
            SoundCategory category = SoundCategory.valueOf((String) rawData.getOrDefault("Category", SoundCategory.PLAYERS.toString()));
            float minVolumeRaw = NumberConversions.toFloat(rawData.getOrDefault("MinVolume", 1.0F));
            float maxVolumeRaw = NumberConversions.toFloat(rawData.getOrDefault("MaxVolume", 1.0F));
            float minPitchRaw = NumberConversions.toFloat(rawData.getOrDefault("MinPitch", 1.0F));
            float maxPitchRaw = NumberConversions.toFloat(rawData.getOrDefault("MaxPitch", 1.0F));

            return new ConfiguredSound(
                    sound,
                    category,
                    Math.min(minVolumeRaw, maxVolumeRaw),
                    Math.max(minVolumeRaw, maxVolumeRaw),
                    Math.min(minPitchRaw, maxPitchRaw),
                    Math.max(minPitchRaw, maxPitchRaw)
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to deserialize sound " + rawData.toString());
        }
    }

    public void play(Location location) {
        location.getWorld().playSound(
                location,
                this.sound(),
                this.category(),
                MathUtils.randomBetween(minVolume(), maxVolume()),
                MathUtils.randomBetween(minPitch(), maxPitch())
        );
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
                "Sound", sound(),
                "Category", category(),
                "MinVolume", minVolume(),
                "MaxVolume", maxVolume(),
                "MinPitch", minPitch(),
                "MaxPitch", maxPitch()
        );
    }
}
