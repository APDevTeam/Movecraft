package net.countercraft.movecraft.compat.v1_18_R1;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.ScheduledTick;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public IWorldHandler() {}

//    @Override
//    public void addPlayerLocation(Player player, double x, double y, double z, float yaw, float pitch){
//        ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//        ePlayer.connection.teleport(x, y, z, yaw, pitch, EnumSet.allOf(ClientboundPlayerPositionPacket.RelativeArgument.class));
//    }

    @Override
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originPoint, @NotNull MovecraftRotation rotation) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        HashMap<BlockPos,BlockPos> rotatedPositions = new HashMap<>();
        MovecraftRotation counterRotation = rotation == MovecraftRotation.CLOCKWISE ? MovecraftRotation.ANTICLOCKWISE : MovecraftRotation.CLOCKWISE;
        for(MovecraftLocation newLocation : craft.getHitBox()){
            rotatedPositions.put(locationToPosition(MathUtils.rotateVec(counterRotation, newLocation.subtract(originPoint)).add(originPoint)),locationToPosition(newLocation));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        ServerLevel nativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPos position : rotatedPositions.keySet()){
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(nativeWorld,position);
            if(tile == null)
                continue;
//            tile.a(ROTATION[rotation.ordinal()]);
            //get the nextTick to move with the tile
            tiles.add(new TileHolder(tile, tickProvider.getNextTick(nativeWorld,position), position));
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
        for(BlockPos position : rotatedPositions.keySet()){
            blockData.put(position,nativeWorld.getBlockState(position).rotate(ROTATION[rotation.ordinal()]));
        }
        //create the new block
        for(Map.Entry<BlockPos,BlockState> entry : blockData.entrySet()) {
            setBlockFast(nativeWorld, rotatedPositions.get(entry.getKey()), entry.getValue());
        }


        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for(TileHolder tileHolder : tiles){
            moveBlockEntity(nativeWorld, rotatedPositions.get(tileHolder.getTilePosition()),tileHolder.getTile());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = nativeWorld.N.getGameTime();
            nativeWorld.getBlockTicks().schedule( new ScheduledTick<>((Block) tileHolder.getNextTick().type(), rotatedPositions.get(tileHolder.getNextTick().pos()), tileHolder.getNextTick().triggerTick() - currentTime, tileHolder.getNextTick().priority(), tileHolder.getNextTick().subTickOrder()));
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        Collection<BlockPos> deletePositions =  CollectionUtils.filter(rotatedPositions.keySet(),rotatedPositions.values());
        for(BlockPos position : deletePositions){
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
        //get the tiles
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPos position = positions.get(i);
            if (oldNativeWorld.getBlockState(position) == Blocks.AIR.defaultBlockState())
                continue;
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(oldNativeWorld, position);
            if (tile == null)
                continue;
            //get the nextTick to move with the tile

            //nativeWorld.capturedTileEntities.remove(position);
            //nativeWorld.getChunkAtWorldCoords(position).getTileEntities().remove(position);
            tiles.add(new TileHolder(tile, tickProvider.getNextTick(oldNativeWorld, position), position));

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
        for(int i = 0, positionSize = newPositions.size(); i<positionSize; i++) {
            setBlockFast(nativeWorld, newPositions.get(i), blockData.get(i));
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for (int i = 0, tilesSize = tiles.size(); i < tilesSize; i++) {
            TileHolder tileHolder = tiles.get(i);
            moveBlockEntity(nativeWorld, tileHolder.getTilePosition().offset(translateVector), tileHolder.getTile());
            if (tileHolder.getNextTick() == null)
                continue;
            final long currentTime = nativeWorld.getGameTime();
            nativeWorld.getBlockTicks().schedule( new ScheduledTick<>((Block) tileHolder.getNextTick().type(), tileHolder.getTilePosition().offset(translateVector), tileHolder.getNextTick().triggerTick() - currentTime, tileHolder.getNextTick().priority(), tileHolder.getNextTick().subTickOrder()));
        }
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        List<BlockPos> deletePositions = positions;
        if (oldNativeWorld == nativeWorld) deletePositions = CollectionUtils.filter(positions,newPositions);
        for (int i = 0, deletePositionsSize = deletePositions.size(); i < deletePositionsSize; i++) {
            BlockPos position = deletePositions.get(i);
            setBlockFast(oldNativeWorld, position, Blocks.AIR.defaultBlockState());
        }
    }

    @Nullable
    private BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position){
        return world.getChunkAt(position).blockEntities.remove(position);
    }

    @NotNull
    private BlockPos locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }

    private void setBlockFast(@NotNull Level world, @NotNull BlockPos position,@NotNull BlockState data) {
        LevelChunk chunk = world.getChunkAt(position);
        LevelChunkSection LevelChunkSection = chunk.getSections()[position.getY()>>4];
        if (LevelChunkSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.setBlockState(position, Blocks.GLASS.defaultBlockState(), false);
            LevelChunkSection = chunk.getSections()[position.getY() >> 4];
        }
        if(LevelChunkSection.getBlockState(position.getX()&15, position.getY()&15, position.getZ()&15).equals(data)){
            //Block is already of correct type and data, don't overwrite
            return;
        }
        LevelChunkSection.setBlockState(position.getX()&15, position.getY()&15, position.getZ()&15, data);
        world.sendBlockUpdated(position, data, data, 3);
        world.getLightEngine().checkBlock(position); // boolean corresponds to if chunk section empty
        chunk.setUnsaved(true);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull BlockData data){
        setBlockFast(location, MovecraftRotation.NONE, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data) {
        BlockState blockData;
        if(data instanceof CraftBlockData){
            blockData = ((CraftBlockData) data).getState();
        } else {
            blockData = (BlockState) data;
        }
        blockData = blockData.rotate(ROTATION[rotation.ordinal()]);
        Level world = ((CraftWorld)(location.getWorld())).getHandle();
        BlockPos BlockPos = locationToPosition(MathUtils.bukkit2MovecraftLoc(location));
        setBlockFast(world,BlockPos,blockData);
    }

    @Override
    public void disableShadow(@NotNull Material type) {
        // Disabled
    }

    private void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull BlockEntity tile){
        LevelChunk chunk = nativeWorld.getChunkAt(newPosition);
        try {
            var positionField = BlockEntity.class.getDeclaredField("o"); // o is obfuscated worldPosition
            UnsafeUtils.setField(positionField, tile, newPosition);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        tile.setLevel(nativeWorld);
        tile.clearRemoved();
        if(nativeWorld.captureBlockStates) {
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        chunk.setBlockEntity(tile);
        chunk.blockEntities.put(newPosition, tile);
    }

    private static class TileHolder{
        @NotNull private final BlockEntity tile;
        @Nullable
        private final ScheduledTick<?> nextTick;
        @NotNull private final BlockPos tilePosition;

        public TileHolder(@NotNull BlockEntity tile, @Nullable ScheduledTick<?> nextTick, @NotNull BlockPos tilePosition){
            this.tile = tile;
            this.nextTick = nextTick;
            this.tilePosition = tilePosition;
        }


        @NotNull
        public BlockEntity getTile() {
            return tile;
        }

        @Nullable
        public ScheduledTick<?> getNextTick() {
            return nextTick;
        }

        @NotNull
        public BlockPos getTilePosition() {
            return tilePosition;
        }
    }
}