package net.countercraft.movecraft.features.contacts;

import org.bukkit.block.BlockFace;

import java.util.Objects;
import java.util.UUID;

public class ContactEntry {

    final UUID craftUUID;
    long lastContactTime;
    BlockFace direction;
    int distance;

    public ContactEntry(UUID craftUUID) {
        this.craftUUID = craftUUID;
        this.lastContactTime = System.currentTimeMillis();
    }

    public ContactEntry at(BlockFace direction, int distance) {
        this.direction = direction;
        this.distance = distance;
        this.lastContactTime = System.currentTimeMillis();

        return this;
    }

    public UUID getContactUUID() {
        return this.craftUUID;
    }

    public long getLastContactTime() {
        return this.lastContactTime;
    }

    public BlockFace getDirection() {
        return this.direction;
    }

    public int getDistance() {
        return this.distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactEntry that)) return false;
        return Objects.equals(craftUUID, that.craftUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(craftUUID);
    }
}
