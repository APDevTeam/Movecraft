package net.countercraft.movecraft.compat.v1_12_R1;

import net.countercraft.movecraft.api.MathUtils;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.Rotation;
import net.countercraft.movecraft.api.WorldHandler;
import net.countercraft.movecraft.api.craft.Craft;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.EnumBlockRotation;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.NextTickListEntry;
import net.minecraft.server.v1_12_R1.StructureBoundingBox;
import net.minecraft.server.v1_12_R1.TileEntity;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IWorldHandler extends WorldHandler {
    private static EnumBlockRotation ROTATION[];
    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }

    @Override
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull Rotation rotation) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        HashMap<BlockPosition,BlockPosition> rotatedPositions = new HashMap<>();
        MovecraftLocation originPoint = new MovecraftLocation(
                (craft.getMaxX()+craft.getMinX())/2,
                (craft.getMaxY()+craft.getMinY())/2,
                (craft.getMaxZ()+craft.getMinX())/2);
        Rotation counterRotation = rotation == Rotation.CLOCKWISE ? Rotation.ANTICLOCKWISE : Rotation.CLOCKWISE;
        for(MovecraftLocation newLocation : craft.getBlockList()){
            rotatedPositions.put(locationToPosition(MathUtils.rotateVec(counterRotation, newLocation).add(originPoint)),locationToPosition(newLocation));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        World nativeWorld = ((CraftWorld) craft.getW()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : rotatedPositions.keySet()){
            TileEntity tile = nativeWorld.getTileEntity(position);
            if(tile == null)
                continue;
            //get the nextTick to move with the tile
            StructureBoundingBox srcBoundingBox = new StructureBoundingBox(position.getX(), position.getY(), position.getZ(), position.getX() + 1, position.getY() + 1, position.getZ() + 1);
            List<NextTickListEntry> originalEntries = nativeWorld.a(srcBoundingBox, true);
            if ( nativeWorld.capturedTileEntities.containsKey(position)) {
                nativeWorld.capturedTileEntities.remove(position);
            }
            nativeWorld.getChunkAtWorldCoords(position).getTileEntities().remove(position);
            if (originalEntries == null) {
                tiles.add(new TileHolder(tile, null, position));
                continue;
            }
            NextTickListEntry entry = originalEntries.get(0);
            tiles.add(new TileHolder(tile, originalEntries.get(0), position));
            originalEntries.remove(originalEntries.get(0));

        }
        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and rotate them
        HashMap<BlockPosition,IBlockData> blockData = new HashMap<>();
        for(BlockPosition position : rotatedPositions.keySet()){
            blockData.put(position,nativeWorld.getType(position).a(ROTATION[rotation.ordinal()]));
        }
        //create the new block
        for(Map.Entry<BlockPosition,IBlockData> entry : blockData.entrySet()) {
            setBlockFast(nativeWorld, rotatedPositions.get(entry.getKey()), entry.getValue());
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for(TileHolder tileHolder : tiles){
            moveTileEntity(nativeWorld, rotatedPositions.get(tileHolder.getTilePosition()),tileHolder.getTile());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = nativeWorld.worldData.getTime();
            nativeWorld.b(rotatedPositions.get(tileHolder.getNextTick().a), tileHolder.getNextTick().a(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        List<BlockPosition> deletePositions = new ArrayList<>(rotatedPositions.keySet());
        deletePositions.removeAll(rotatedPositions.values());
        for(BlockPosition position : deletePositions){
            setBlockFast(nativeWorld, position, Blocks.AIR.getBlockData());
        }

        //*******************************************
        //*       Step six: Update the blocks       *
        //*******************************************
        org.bukkit.World bukkitWorld = craft.getW();
        for(BlockPosition newPosition : rotatedPositions.values()) {
            bukkitWorld.getBlockAt(newPosition.getX(), newPosition.getY(), newPosition.getZ()).getState().update(false, false);
        }
        for(BlockPosition deletedPosition : deletePositions){
            bukkitWorld.getBlockAt(deletedPosition.getX(), deletedPosition.getY(), deletedPosition.getZ()).getState().update(false, false);
        }
        //*******************************************
        //*       Step seven: Send to players       *
        //*******************************************
        List<Chunk> chunks = new ArrayList<>();
        for(BlockPosition position : rotatedPositions.values()){
            Chunk chunk = nativeWorld.getChunkAtWorldCoords(position);
            if(!chunks.contains(chunk)){
                chunks.add(chunk);
            }
        }
        for(BlockPosition position : deletePositions){
            Chunk chunk = nativeWorld.getChunkAtWorldCoords(position);
            if(!chunks.contains(chunk)){
                chunks.add(chunk);
            }
        }
        //sendToPlayers(chunks.toArray(new Chunk[0]));
    }

    @Override
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement) {
        //TODO: Add supourt for rotations
        //A craftTranslateCommand should only occur if the craft is moving to a valid position
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        BlockPosition translateVector = locationToPosition(displacement);
        List<BlockPosition> positions = new ArrayList<>();
        for(MovecraftLocation movecraftLocation : craft.getBlockList()) {
            positions.add(locationToPosition((movecraftLocation)).b(translateVector));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        World nativeWorld = ((CraftWorld) craft.getW()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : positions){
            TileEntity tile = nativeWorld.getTileEntity(position);
            if(tile == null)
                continue;
            //get the nextTick to move with the tile
            StructureBoundingBox srcBoundingBox = new StructureBoundingBox(position.getX(), position.getY(), position.getZ(), position.getX() + 1, position.getY() + 1, position.getZ() + 1);
            List<NextTickListEntry> originalEntries = nativeWorld.a(srcBoundingBox, true);
            if ( nativeWorld.capturedTileEntities.containsKey(position)) {
                nativeWorld.capturedTileEntities.remove(position);
            }
            nativeWorld.getChunkAtWorldCoords(position).getTileEntities().remove(position);
            if (originalEntries == null) {
                tiles.add(new TileHolder(tile, null, position));
                continue;
            }
            NextTickListEntry entry = originalEntries.get(0);
            tiles.add(new TileHolder(tile, originalEntries.get(0), position));
            originalEntries.remove(originalEntries.get(0));

        }
        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks
        List<IBlockData> blockData = new ArrayList<>();
        for(BlockPosition position : positions){
            blockData.add(nativeWorld.getType(position));
        }
        //translate the positions
        List<BlockPosition> newPositions = new ArrayList<>();
        for(BlockPosition position : positions){
            newPositions.add(position.a(translateVector));
        }
        //create the new block
        for(int i = 0; i<newPositions.size(); i++) {
            setBlockFast(nativeWorld, newPositions.get(i), blockData.get(i));
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for(TileHolder tileHolder : tiles){
            moveTileEntity(nativeWorld, tileHolder.getTilePosition().a(translateVector),tileHolder.getTile());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = nativeWorld.worldData.getTime();
            nativeWorld.b(tileHolder.getNextTick().a.a(translateVector), tileHolder.getNextTick().a(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        List<BlockPosition> deletePositions = new ArrayList<>(positions);
        deletePositions.removeAll(newPositions);
        for(BlockPosition position : deletePositions){
            setBlockFast(nativeWorld, position, Blocks.AIR.getBlockData());
        }

        //*******************************************
        //*       Step six: Update the blocks       *
        //*******************************************
        org.bukkit.World bukkitWorld = craft.getW();
        for(BlockPosition newPosition : newPositions) {
            bukkitWorld.getBlockAt(newPosition.getX(), newPosition.getY(), newPosition.getZ()).getState().update(false, false);
        }
        for(BlockPosition deletedPosition : deletePositions){
            bukkitWorld.getBlockAt(deletedPosition.getX(), deletedPosition.getY(), deletedPosition.getZ()).getState().update(false, false);
        }
        //*******************************************
        //*       Step seven: Send to players       *
        //*******************************************
        List<Chunk> chunks = new ArrayList<>();
        for(BlockPosition position : newPositions){
            Chunk chunk = nativeWorld.getChunkAtWorldCoords(position);
            if(!chunks.contains(chunk)){
                chunks.add(chunk);
            }
        }
        for(BlockPosition position : deletePositions){
            Chunk chunk = nativeWorld.getChunkAtWorldCoords(position);
            if(!chunks.contains(chunk)){
                chunks.add(chunk);
            }
        }
        //sendToPlayers(chunks.toArray(new Chunk[0]));
    }

    @NotNull
    private BlockPosition locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
    }



    private void setBlockFast(@NotNull World world, @NotNull BlockPosition position,@NotNull IBlockData data) {
        Chunk chunk = world.getChunkAtWorldCoords(position);
        chunk.a(position, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull Material material, byte data){
        setBlockFast(location, Rotation.NONE, material, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull Material material, byte data) {
        IBlockData blockData =  CraftMagicNumbers.getBlock(material).fromLegacyData(data);
        blockData = blockData.a(ROTATION[rotation.ordinal()]);
        World world = ((CraftWorld)(location.getWorld())).getHandle();
        BlockPosition blockPosition = locationToPosition(bukkit2MovecraftLoc(location));
        setBlockFast(world,blockPosition,blockData);
    }

    @Override
    public void disableShadow(@NotNull Material type) {
        Method method;
        try {
            Block tempBlock = CraftMagicNumbers.getBlock(type.getId());
            method = Block.class.getDeclaredMethod("e", int.class);
            method.setAccessible(true);
            method.invoke(tempBlock, 0);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException | SecurityException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private static MovecraftLocation bukkit2MovecraftLoc(Location l) {
        return new MovecraftLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private void moveTileEntity(@NotNull World nativeWorld, @NotNull BlockPosition newPosition, @NotNull TileEntity tile){
        Chunk chunk = nativeWorld.getChunkAtWorldCoords(newPosition);
        if(nativeWorld.captureBlockStates) {
            tile.a(nativeWorld);
            tile.setPosition(newPosition);
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        tile.setPosition(newPosition);
        chunk.tileEntities.put(newPosition, tile);
    }

    private class TileHolder{
        @NotNull private final TileEntity tile;
        @Nullable
        private final NextTickListEntry nextTick;
        @NotNull private final BlockPosition tilePosition;

        public TileHolder(@NotNull TileEntity tile, @Nullable NextTickListEntry nextTick, @NotNull BlockPosition tilePosition){
            this.tile = tile;
            this.nextTick = nextTick;
            this.tilePosition = tilePosition;
        }


        @NotNull
        public TileEntity getTile() {
            return tile;
        }

        @Nullable
        public NextTickListEntry getNextTick() {
            return nextTick;
        }

        @NotNull
        public BlockPosition getTilePosition() {
            return tilePosition;
        }
    }
}
