package net.countercraft.movecraft.compat.v1_21_4;

import ca.spottedleaf.moonrise.common.util.WorldUtil;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.UnsafeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class IWorldHandler extends WorldHandler {
    private static final Rotation ROTATION[];

    static {
        ROTATION = new Rotation[3];
        ROTATION[MovecraftRotation.NONE.ordinal()] = Rotation.NONE;
        ROTATION[MovecraftRotation.CLOCKWISE.ordinal()] = Rotation.CLOCKWISE_90;
        ROTATION[MovecraftRotation.ANTICLOCKWISE.ordinal()] = Rotation.COUNTERCLOCKWISE_90;
    }

    private final NextTickProvider tickProvider = new NextTickProvider();

    public IWorldHandler() {
        String version = Bukkit.getServer().getMinecraftVersion();
        if (!version.equals("1.21.4"))
            throw new IllegalStateException("Movecraft is not compatible with this version of Minecraft: " + version);
    }

    @Override
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originPoint, @NotNull MovecraftRotation rotation) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        HashMap<BlockPos, BlockPos> rotatedPositions = new HashMap<>();
        MovecraftRotation counterRotation = rotation == MovecraftRotation.CLOCKWISE ? MovecraftRotation.ANTICLOCKWISE : MovecraftRotation.CLOCKWISE;
        for (MovecraftLocation newLocation : craft.getHitBox()) {
            rotatedPositions.put(locationToPosition(MathUtils.rotateVec(counterRotation, newLocation.subtract(originPoint)).add(originPoint)), locationToPosition(newLocation));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        ServerLevel nativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        List<TickHolder> ticks = new ArrayList<>();
        //get the tiles
        for (BlockPos position : rotatedPositions.keySet()) {
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(nativeWorld, position);
            if (tile != null)
                tiles.add(new TileHolder(tile, position));

            //get the nextTick to move with the tile
            ScheduledTick tickHere = tickProvider.getNextTick(nativeWorld, position);
            if (tickHere != null) {
                ((LevelChunkTicks) nativeWorld.getChunkAt(position).getBlockTicks()).removeIf(
                        (Predicate<ScheduledTick>) scheduledTick -> scheduledTick.equals(tickHere));
                ticks.add(new TickHolder(tickHere, position));
            }
        }

        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and rotate them
        HashMap<BlockPos, BlockState> blockData = new HashMap<>();
        for (BlockPos position : rotatedPositions.keySet()) {
            blockData.put(position, nativeWorld.getBlockState(position).rotate(ROTATION[rotation.ordinal()]));
        }
        //create the new block
        for (Map.Entry<BlockPos, BlockState> entry : blockData.entrySet()) {
            setBlockFast(nativeWorld, rotatedPositions.get(entry.getKey()), entry.getValue());
        }


        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for (TileHolder tileHolder : tiles)
            moveBlockEntity(nativeWorld, rotatedPositions.get(tileHolder.getTilePosition()), tileHolder.getTile());
        for (TickHolder tickHolder : ticks) {
            final long currentTime = nativeWorld.serverLevelData.getGameTime();
            nativeWorld.getBlockTicks().schedule(new ScheduledTick<>(
                    (Block) tickHolder.getTick().type(),
                    rotatedPositions.get(tickHolder.getTick().pos()),
                    tickHolder.getTick().triggerTick() - currentTime,
                    tickHolder.getTick().priority(),
                    tickHolder.getTick().subTickOrder()));
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        Collection<BlockPos> deletePositions = CollectionUtils.filter(rotatedPositions.keySet(), rotatedPositions.values());
        for (BlockPos position : deletePositions) {
            setBlockFast(nativeWorld, position, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull org.bukkit.World world) {
        //TODO: Add support for rotations
        //A craftTranslateCommand should only occur if the craft is moving to a valid position
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        BlockPos translateVector = locationToPosition(displacement);
        List<BlockPos> positions = new ArrayList<>(craft.getHitBox().size());
        craft.getHitBox().forEach((movecraftLocation) -> positions.add(locationToPosition((movecraftLocation)).subtract(translateVector)));
        ServerLevel oldNativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        ServerLevel nativeWorld = ((CraftWorld) world).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        List<TileHolder> tiles = new ArrayList<>();
        List<TickHolder> ticks = new ArrayList<>();
        //get the tiles
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPos position = positions.get(i);
            if (oldNativeWorld.getBlockState(position) == Blocks.AIR.defaultBlockState())
                continue;
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(oldNativeWorld, position);
            if (tile != null)
                tiles.add(new TileHolder(tile,position));

            //get the nextTick to move with the tile
            ScheduledTick tickHere = tickProvider.getNextTick(nativeWorld, position);
            if (tickHere != null) {
                ((LevelChunkTicks) nativeWorld.getChunkAt(position).getBlockTicks()).removeIf(
                        (Predicate<ScheduledTick>) scheduledTick -> scheduledTick.equals(tickHere));
                ticks.add(new TickHolder(tickHere, position));
            }
        }
        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and translate the positions
        List<BlockState> blockData = new ArrayList<>();
        List<BlockPos> newPositions = new ArrayList<>();
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPos position = positions.get(i);
            blockData.add(oldNativeWorld.getBlockState(position));
            newPositions.add(position.offset(translateVector));
        }
        //create the new block
        for (int i = 0, positionSize = newPositions.size(); i < positionSize; i++) {
            setBlockFast(nativeWorld, newPositions.get(i), blockData.get(i));
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for (TileHolder tileHolder : tiles)
            moveBlockEntity(nativeWorld, tileHolder.getTilePosition().offset(translateVector), tileHolder.getTile());
        for (TickHolder tickHolder : ticks) {
            final long currentTime = nativeWorld.getGameTime();
            nativeWorld.getBlockTicks().schedule(new ScheduledTick<>((Block) tickHolder.getTick().type(), tickHolder.getTickPosition().offset(translateVector), tickHolder.getTick().triggerTick() - currentTime, tickHolder.getTick().priority(), tickHolder.getTick().subTickOrder()));
        }
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        List<BlockPos> deletePositions = positions;
        if (oldNativeWorld == nativeWorld)
            deletePositions = CollectionUtils.filter(positions, newPositions);
        for (int i = 0, deletePositionsSize = deletePositions.size(); i < deletePositionsSize; i++) {
            BlockPos position = deletePositions.get(i);
            setBlockFast(oldNativeWorld, position, Blocks.AIR.defaultBlockState());
        }
    }

    @Nullable
    private BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position) {
        BlockEntity testEntity = world.getChunkAt(position).getBlockEntity(position);
        //Prevents moving pistons by locking up by forcing their movement to finish
        if (testEntity instanceof PistonMovingBlockEntity)
        {
            BlockState oldState;
            if (((PistonMovingBlockEntity) testEntity).isSourcePiston() && testEntity.getBlockState().getBlock() instanceof PistonBaseBlock) {
                if (((PistonMovingBlockEntity) testEntity).getMovedState().is(Blocks.PISTON))
                    oldState = Blocks.PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
                else
                    oldState = Blocks.STICKY_PISTON.defaultBlockState()
                            .setValue(PistonBaseBlock.FACING, ((PistonMovingBlockEntity) testEntity).getMovedState().getValue(PistonBaseBlock.FACING));
            } else
                oldState = ((PistonMovingBlockEntity) testEntity).getMovedState();
            ((PistonMovingBlockEntity) testEntity).finalTick();
            setBlockFast(world, position, oldState);
            return world.getBlockEntity(position);
        }
        return world.getChunkAt(position).blockEntities.remove(position);
    }

    @NotNull
    private BlockPos locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }

    private void setBlockFast(@NotNull Level world, @NotNull BlockPos position, @NotNull BlockState data) {
        LevelChunk chunk = world.getChunkAt(position);
        int chunkSection = (position.getY() >> 4) - WorldUtil.getMinSection(world);
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
        chunk.markUnsaved();
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

    private void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull BlockEntity tile) {
        LevelChunk chunk = nativeWorld.getChunkAt(newPosition);
        try {
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

    private static class TileHolder {
        @NotNull
        private final BlockEntity tile;
        @NotNull
        private final BlockPos tilePosition;

        public TileHolder(@NotNull BlockEntity tile, @NotNull BlockPos tilePosition) {
            this.tile = tile;
            this.tilePosition = tilePosition;
        }


        @NotNull
        public BlockEntity getTile() {
            return tile;
        }

        @NotNull
        public BlockPos getTilePosition() {
            return tilePosition;
        }
    }

    private static class TickHolder {
        @NotNull
        private final ScheduledTick tick;
        @NotNull
        private final BlockPos tickPosition;

        public TickHolder(@NotNull ScheduledTick tick, @NotNull BlockPos tilePosition) {
            this.tick = tick;
            this.tickPosition = tilePosition;
        }


        @NotNull
        public ScheduledTick getTick() {
            return tick;
        }

        @NotNull
        public BlockPos getTickPosition() {
            return tickPosition;
        }
    }
}