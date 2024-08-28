package net.countercraft.movecraft.compat.v1_18;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.util.AffineTransformation;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.UnsafeUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftInventoryView;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "ForLoopReplaceableByForEach"})
public class IWorldHandler extends WorldHandler {
    private static final Rotation[] ROTATION = new Rotation[3];
    private static final Mirror[] MIRROR = new Mirror[3];

    static {
        ROTATION[MovecraftRotation.NONE.ordinal()] = Rotation.NONE;
        ROTATION[MovecraftRotation.CLOCKWISE.ordinal()] = Rotation.CLOCKWISE_90;
        ROTATION[MovecraftRotation.ANTICLOCKWISE.ordinal()] = Rotation.COUNTERCLOCKWISE_90;

        MIRROR[org.bukkit.block.structure.Mirror.NONE.ordinal()] = Mirror.NONE;
        MIRROR[org.bukkit.block.structure.Mirror.LEFT_RIGHT.ordinal()] = Mirror.LEFT_RIGHT;
        MIRROR[org.bukkit.block.structure.Mirror.FRONT_BACK.ordinal()] = Mirror.FRONT_BACK;
    }

    private final NextTickProvider tickProvider = new NextTickProvider();

    public IWorldHandler() {
        String version = Bukkit.getServer().getMinecraftVersion();
        if (!version.equals("1.18.2"))
            throw new IllegalStateException("Movecraft is not compatible with this version of Minecraft: " + version);
    }

    @Override
    public void transformHitBox(@NotNull HitBox hitbox, @NotNull AffineTransformation transformation, @NotNull World originWorld, @NotNull World destinationWorld) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        List<BlockPos> positions = hitbox
            .stream()
            .map(transformation::apply)
            .map(this::locationToPosition)
            .collect(Collectors.toList());
        ServerLevel originLevel = ((CraftWorld) originWorld).getHandle();
        ServerLevel destinationLevel = ((CraftWorld) destinationWorld).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        List<TileHolder> tiles = new ArrayList<>();
        List<TickHolder> ticks = new ArrayList<>();
        //get the tiles
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPos position = positions.get(i);
            if (originLevel.getBlockState(position) == Blocks.AIR.defaultBlockState())
                continue;
            BlockEntity tile = removeBlockEntity(originLevel, position);
            if (tile != null)
                tiles.add(new TileHolder(tile,position));

            //get the nextTick to move with the tile
            ScheduledTick<?> tickHere = tickProvider.getNextTick(destinationLevel, position);
            if (tickHere != null) {
                var levelTicks = ((LevelChunkTicks<Block>) destinationLevel.getChunkAt(position).getBlockTicks());
                levelTicks.removeIf(tickHere::equals);
                ticks.add(new TickHolder(tickHere, position));
            }
        }
        //*******************************************
        //*   Step three: Transform all the blocks  *
        //*******************************************
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and translate the positions
        List<BlockState> blockData = new ArrayList<>();
        List<BlockPos> newPositions = new ArrayList<>();
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPos position = positions.get(i);
            blockData.add(locallyTransformState(transformation, originLevel.getBlockState(position)));
            newPositions.add(transformPosition(transformation, position));
        }
        //create the new block
        for (int i = 0, positionSize = newPositions.size(); i < positionSize; i++) {
            setBlockFast(destinationLevel, newPositions.get(i), blockData.get(i));
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for (TileHolder tileHolder : tiles)
            moveBlockEntity(destinationLevel, transformPosition(transformation, tileHolder.tilePosition()), tileHolder.tile());
        for (TickHolder tickHolder : ticks) {
            final long currentTime = destinationLevel.getGameTime();
            destinationLevel.getBlockTicks().schedule(new ScheduledTick<>((Block) tickHolder.tick().type(), transformPosition(transformation, tickHolder.tickPosition()), tickHolder.tick().triggerTick() - currentTime, tickHolder.tick().priority(), tickHolder.tick().subTickOrder()));
        }
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        List<BlockPos> deletePositions = positions;
        if (originLevel == destinationLevel)
            deletePositions = CollectionUtils.filter(positions, newPositions);
        for (int i = 0, deletePositionsSize = deletePositions.size(); i < deletePositionsSize; i++) {
            BlockPos position = deletePositions.get(i);
            setBlockFast(originLevel, position, Blocks.AIR.defaultBlockState());
        }
    }

    @Nullable
    private BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position) {
        return world.getChunkAt(position).blockEntities.remove(position);
    }
    @NotNull
    private BlockState locallyTransformState(@NotNull AffineTransformation transformation, @NotNull BlockState state){
        return state
            .rotate(ROTATION[transformation.extractRotation().ordinal()])
            .mirror(MIRROR[transformation.extractMirror().ordinal()]);
    }

    @NotNull
    private BlockPos locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }

    @NotNull @Contract(pure = true)
    private BlockPos transformPosition(@NotNull AffineTransformation transformation, @NotNull BlockPos pos){
        return locationToPosition(transformation.apply(new MovecraftLocation(pos.getX(), pos.getY(), pos.getZ())));
    }

    private void setBlockFast(@NotNull Level world, @NotNull BlockPos position, @NotNull BlockState data) {
        LevelChunk chunk = world.getChunkAt(position);
        int chunkSection = (position.getY() >> 4) - chunk.getMinSection();
        LevelChunkSection section = chunk.getSections()[chunkSection];
        if (section == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.setBlockState(position, Blocks.GLASS.defaultBlockState(), false);
            section = chunk.getSections()[chunkSection];
        }
        if (section.getBlockState(position.getX() & 15, position.getY() & 15, position.getZ() & 15).equals(data)) {
            //Block is already of correct type and data, don't overwrite
            return;
        }
        section.setBlockState(position.getX() & 15, position.getY() & 15, position.getZ() & 15, data);
        world.sendBlockUpdated(position, data, data, 3);
        world.getLightEngine().checkBlock(position); // boolean corresponds to if chunk section empty
        chunk.setUnsaved(true);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull BlockData data) {
        setBlockFast(location, MovecraftRotation.NONE, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data) {
        BlockState blockData;
        if (data instanceof CraftBlockData) {
            blockData = ((CraftBlockData) data).getState();
        }
        else {
            blockData = (BlockState) data;
        }
        blockData = blockData.rotate(ROTATION[rotation.ordinal()]);
        Level world = ((CraftWorld) (location.getWorld())).getHandle();
        BlockPos BlockPos = locationToPosition(MathUtils.bukkit2MovecraftLoc(location));
        setBlockFast(world, BlockPos, blockData);
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable Location getAccessLocation(@NotNull InventoryView inventoryView) {
        AbstractContainerMenu menu = ((CraftInventoryView) inventoryView).getHandle();
        Field field = UnsafeUtils.getFieldOfType(ContainerLevelAccess.class, menu.getClass());
        if (field != null) {
            try {
                field.setAccessible(true);
                return ((ContainerLevelAccess) field.get(menu)).getLocation();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressWarnings("removal")
    @Override
    public void setAccessLocation(@NotNull InventoryView inventoryView, @NotNull Location location) {
        if (location.getWorld() == null)
            return;
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        BlockPos position = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ContainerLevelAccess access = ContainerLevelAccess.create(level, position);

        AbstractContainerMenu menu = ((CraftInventoryView) inventoryView).getHandle();
        UnsafeUtils.trySetFieldOfType(ContainerLevelAccess.class, menu, access);
    }

    private void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull BlockEntity tile) {
        LevelChunk chunk = nativeWorld.getChunkAt(newPosition);
        try {
            //noinspection JavaReflectionMemberAccess
            var positionField = BlockEntity.class.getDeclaredField("o"); // o is obfuscated worldPosition
            UnsafeUtils.setField(positionField, tile, newPosition);
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        tile.setLevel(nativeWorld);
        tile.clearRemoved();
        if (nativeWorld.captureBlockStates) {
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        chunk.setBlockEntity(tile);
        chunk.blockEntities.put(newPosition, tile);
    }

    private record TileHolder(@NotNull BlockEntity tile, @NotNull BlockPos tilePosition) { }

    private record TickHolder(@NotNull ScheduledTick<?> tick, @NotNull BlockPos tickPosition) { }
}