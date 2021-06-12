package net.countercraft.movecraft.compat.v1_17_R1;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.MathUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.NextTickListEntry;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkSection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class IWorldHandler extends WorldHandler {
    private static final EnumBlockRotation ROTATION[];
    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.a;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.b;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.d;
    }
    private final NextTickProvider tickProvider = new NextTickProvider();
    private MethodHandle internalTeleportMH;

    public IWorldHandler() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method teleport = null;
        try {
            teleport = PlayerConnection.class.getDeclaredMethod("a", double.class, double.class, double.class, float.class, float.class, Set.class);
            teleport.setAccessible(true);
            internalTeleportMH = lookup.unreflect(teleport);

        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addPlayerLocation(Player player, double x, double y, double z, float yaw, float pitch){
        EntityPlayer ePlayer = ((CraftPlayer) player).getHandle();
        if(internalTeleportMH == null) {
            //something went wrong
            super.addPlayerLocation(player, x, y, z, yaw, pitch);
            return;
        }
        try {
            internalTeleportMH.invoke(ePlayer.b, x, y, z, yaw, pitch, EnumSet.allOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originPoint, @NotNull Rotation rotation) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        HashMap<BlockPosition,BlockPosition> rotatedPositions = new HashMap<>();
        Rotation counterRotation = rotation == Rotation.CLOCKWISE ? Rotation.ANTICLOCKWISE : Rotation.CLOCKWISE;
        for(MovecraftLocation newLocation : craft.getHitBox()){
            rotatedPositions.put(locationToPosition(MathUtils.rotateVec(counterRotation, newLocation.subtract(originPoint)).add(originPoint)),locationToPosition(newLocation));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        WorldServer nativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : rotatedPositions.keySet()){
            //TileEntity tile = nativeWorld.removeTileEntity(position);
            TileEntity tile = removeTileEntity(nativeWorld,position);
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
        HashMap<BlockPosition, IBlockData> blockData = new HashMap<>();
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
            final long currentTime = nativeWorld.E.getTime();
            nativeWorld.getBlockTickList().a(rotatedPositions.get(tileHolder.getNextTick().a), (Block)tileHolder.getNextTick().b(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        Collection<BlockPosition> deletePositions =  CollectionUtils.filter(rotatedPositions.keySet(),rotatedPositions.values());
        for(BlockPosition position : deletePositions){
            setBlockFast(nativeWorld, position, Blocks.a.getBlockData());
        }
    }

    @Override
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull org.bukkit.World world) {
        //TODO: Add support for rotations
        //A craftTranslateCommand should only occur if the craft is moving to a valid position
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        BlockPosition translateVector = locationToPosition(displacement);
        List<BlockPosition> positions = new ArrayList<>(craft.getHitBox().size());
        craft.getHitBox().forEach((movecraftLocation) -> positions.add(locationToPosition((movecraftLocation)).e(translateVector)));
        WorldServer oldNativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        WorldServer nativeWorld = ((CraftWorld) world).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPosition position = positions.get(i);
            if (oldNativeWorld.getType(position) == Blocks.a.getBlockData())
                continue;
            //TileEntity tile = nativeWorld.removeTileEntity(position);
            TileEntity tile = removeTileEntity(oldNativeWorld, position);
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
        List<IBlockData> blockData = new ArrayList<>();
        List<BlockPosition> newPositions = new ArrayList<>();
        for (int i = 0, positionsSize = positions.size(); i < positionsSize; i++) {
            BlockPosition position = positions.get(i);
            blockData.add(oldNativeWorld.getType(position));
            newPositions.add(position.f(translateVector));
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
            moveTileEntity(nativeWorld, tileHolder.getTilePosition().f(translateVector), tileHolder.getTile());
            if (tileHolder.getNextTick() == null)
                continue;
            final long currentTime = nativeWorld.E.getTime();
            nativeWorld.getBlockTickList().a(tileHolder.getNextTick().a.f(translateVector), (Block) tileHolder.getNextTick().b(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
        }
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        List<BlockPosition> deletePositions = positions;
        if (oldNativeWorld == nativeWorld) deletePositions = CollectionUtils.filter(positions,newPositions);
        for (int i = 0, deletePositionsSize = deletePositions.size(); i < deletePositionsSize; i++) {
            BlockPosition position = deletePositions.get(i);
            setBlockFast(oldNativeWorld, position, Blocks.a.getBlockData());
        }
    }

    @Nullable
    private TileEntity removeTileEntity(@NotNull World world, @NotNull BlockPosition position){
        return world.getChunkAtWorldCoords(position).l.remove(position);
    }

    @NotNull
    private BlockPosition locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
    }

    private void setBlockFast(@NotNull World world, @NotNull BlockPosition position,@NotNull IBlockData data) {
        Chunk chunk = world.getChunkAtWorldCoords(position);
        ChunkSection chunkSection = chunk.getSections()[position.getY()>>4];
        if (chunkSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.setType(position, Blocks.au.getBlockData(), false);
            chunkSection = chunk.getSections()[position.getY() >> 4];
        }
        if(chunkSection.getType(position.getX()&15, position.getY()&15, position.getZ()&15).equals(data)){
            //Block is already of correct type and data, don't overwrite
            return;
        }

        chunkSection.setType(position.getX()&15, position.getY()&15, position.getZ()&15, data);
        world.notify(position, data, data, 3);
        chunk.markDirty();
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull BlockData data){
        setBlockFast(location, Rotation.NONE, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull BlockData data) {
        IBlockData blockData;
        if(data instanceof CraftBlockData){
            blockData = ((CraftBlockData) data).getState();
        } else {
            blockData = (IBlockData) data;
        }
        blockData = blockData.a(ROTATION[rotation.ordinal()]);
        World world = ((CraftWorld)(location.getWorld())).getHandle();
        BlockPosition blockPosition = locationToPosition(bukkit2MovecraftLoc(location));
        setBlockFast(world,blockPosition,blockData);
    }

    @Override
    public void disableShadow(@NotNull Material type) {
        // Disabled
    }

    private static MovecraftLocation bukkit2MovecraftLoc(Location l) {
        return new MovecraftLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private void moveTileEntity(@NotNull World nativeWorld, @NotNull BlockPosition newPosition, @NotNull TileEntity tile){
        Chunk chunk = nativeWorld.getChunkAtWorldCoords(newPosition);
        try {
            var positionField = TileEntity.class.getDeclaredField("o");
            var modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(positionField, positionField.getModifiers() & ~Modifier.FINAL);
            positionField.set(tile, newPosition);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if(nativeWorld.captureBlockStates) {
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        chunk.l.put(newPosition, tile);
    }

    private class TileHolder{
        @NotNull private final TileEntity tile;
        @Nullable
        private final NextTickListEntry<?> nextTick;
        @NotNull private final BlockPosition tilePosition;

        public TileHolder(@NotNull TileEntity tile, @Nullable NextTickListEntry<?> nextTick, @NotNull BlockPosition tilePosition){
            this.tile = tile;
            this.nextTick = nextTick;
            this.tilePosition = tilePosition;
        }


        @NotNull
        public TileEntity getTile() {
            return tile;
        }

        @Nullable
        public NextTickListEntry<?> getNextTick() {
            return nextTick;
        }

        @NotNull
        public BlockPosition getTilePosition() {
            return tilePosition;
        }
    }
}