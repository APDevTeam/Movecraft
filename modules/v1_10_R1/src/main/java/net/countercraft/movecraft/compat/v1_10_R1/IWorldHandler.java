package net.countercraft.movecraft.compat.v1_10_R1;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.CollectionUtils;
import net.countercraft.movecraft.utils.MathUtils;
import net.minecraft.server.v1_10_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class IWorldHandler extends WorldHandler {
    private static final EnumBlockRotation ROTATION[];
    private final HashMap<World,List<TileEntity>> bMap = new HashMap<>();
    private MethodHandle internalTeleportMH;
    static {
        ROTATION = new EnumBlockRotation[3];
        ROTATION[Rotation.NONE.ordinal()] = EnumBlockRotation.NONE;
        ROTATION[Rotation.CLOCKWISE.ordinal()] = EnumBlockRotation.CLOCKWISE_90;
        ROTATION[Rotation.ANTICLOCKWISE.ordinal()] = EnumBlockRotation.COUNTERCLOCKWISE_90;
    }
    private final NextTickProvider tickProvider = new NextTickProvider();

    public IWorldHandler() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Method teleport = null;
        try {
            teleport = PlayerConnection.class.getDeclaredMethod("internalTeleport", double.class, double.class, double.class, float.class, float.class, Set.class);
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
            internalTeleportMH.invoke(ePlayer.playerConnection, x, y, z, yaw, pitch, EnumSet.allOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class));
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
        WorldServer nativeWorld = ((CraftWorld) craft.getW()).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        List<TileHolder> tiles = getTiles(rotatedPositions.keySet(), rotation, nativeWorld);

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
            if(tileHolder.getTile() != null)
                moveTileEntity(nativeWorld, rotatedPositions.get(tileHolder.getTilePosition()),tileHolder.getTile(), tileHolder.isCaptured);
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = nativeWorld.worldData.getTime();
            if(!rotatedPositions.containsKey(tileHolder.getNextTick().a)){
                nativeWorld.b(tileHolder.getNextTick().a, tileHolder.getNextTick().a(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
            } else {
                nativeWorld.b(rotatedPositions.get(tileHolder.getNextTick().a), tileHolder.getNextTick().a(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
            }

        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        Collection<BlockPosition> deletePositions =  CollectionUtils.filter(rotatedPositions.keySet(),rotatedPositions.values());
        setAir(deletePositions, nativeWorld);
        //*******************************************
        //*   Step six: Process fire spread         *
        //*******************************************
        for (BlockPosition position : rotatedPositions.values()) {
            IBlockData type = nativeWorld.getType(position);
            if (!(type.getBlock() instanceof BlockFire)) {
                continue;
            }
            BlockFire fire = (BlockFire) type.getBlock();
            fire.b(nativeWorld, position, type, nativeWorld.random);
        }
    }

    @Override
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull org.bukkit.World world) {
        //TODO: Add supourt for rotations
        //A craftTranslateCommand should only occur if the craft is moving to a valid position
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        BlockPosition translateVector = locationToPosition(displacement);
        List<BlockPosition> positions = new ArrayList<>();
        for(MovecraftLocation movecraftLocation : craft.getHitBox()) {
            positions.add(locationToPosition((movecraftLocation)).b(translateVector));
        }
        WorldServer oldNativeWorld = ((CraftWorld) craft.getW()).getHandle();
        WorldServer nativeWorld = ((CraftWorld) world).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        Set<BlockPosition> validTickLocations = new HashSet<>(positions);
        List<TileHolder> tiles = getTiles(positions, oldNativeWorld);
        final long currentTime = oldNativeWorld.worldData.getTime();
        for(TileHolder tile : tiles){
            if(tile.getNextTick() != null && !validTickLocations.contains(tile.getNextTick().a)){
                NextTickListEntry tick = tile.getNextTick();
                oldNativeWorld.b(tick.a, tick.a(), (int) (tick.b - currentTime), tick.c);
                tile.nextTick = null;
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
        List<IBlockData> blockData = new ArrayList<>();
        List<BlockPosition> newPositions = new ArrayList<>();
        for(BlockPosition position : positions){
            blockData.add(oldNativeWorld.getType(position));
            newPositions.add(position.a(translateVector));
        }
        //create the new block
        setBlocks(newPositions, blockData, nativeWorld);
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        processTiles(tiles, nativeWorld, translateVector);
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        Collection<BlockPosition> deletePositions = positions;
        if (oldNativeWorld == nativeWorld) deletePositions = CollectionUtils.filter(positions,newPositions);
        setAir(deletePositions, oldNativeWorld);
        //*******************************************
        //*   Step six: Process fire spread         *
        //*******************************************
        processFireSpread(newPositions, nativeWorld);
    }

    private void setBlocks(List<BlockPosition> newPositions, List<IBlockData> blockData, World nativeWorld){
        for(int i = 0; i<newPositions.size(); i++) {
            setBlockFast(nativeWorld, newPositions.get(i), blockData.get(i));
        }
    }

    private List<TileHolder> getTiles(Iterable<BlockPosition> positions, WorldServer nativeWorld){
        return getTiles(positions, null, nativeWorld);
    }

    private List<TileHolder> getTiles(Iterable<BlockPosition> positions, Rotation rotation,  WorldServer nativeWorld){
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : positions){
            if(nativeWorld.getType(position) == Blocks.AIR.getBlockData())
                continue;
            TileHolder holder = removeTileEntity(nativeWorld,position);
            if(holder == null)
                continue;
            if(holder.tile != null && rotation != null){
                holder.tile.a(ROTATION[rotation.ordinal()]);
            }
            tiles.add(holder);

        }
        return tiles;
    }

    private void processTiles(Iterable<TileHolder> tiles, World world, BlockPosition translateVector){
        for(TileHolder tileHolder : tiles){
            if(tileHolder.getTile() != null)
                moveTileEntity(world, tileHolder.getTilePosition().a(translateVector),tileHolder.getTile(), tileHolder.isCaptured());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = world.worldData.getTime();
            NextTickListEntry tick = tileHolder.getNextTick();
            world.b(tick.a.a(translateVector), tick.a(), (int) (tick.b - currentTime), tick.c);
        }
    }

    private void processFireSpread(Iterable<BlockPosition> positions, World world){
        for (BlockPosition position : positions) {
            IBlockData type = world.getType(position);
            if (!(type.getBlock() instanceof BlockFire)) {
                continue;
            }
            BlockFire fire = (BlockFire) type.getBlock();
            fire.b(world, position, type, world.random);
        }
    }

    private void setAir(Iterable<BlockPosition> positions, World world){
        for(BlockPosition position : positions){
            setBlockFast(world, position, Blocks.AIR.getBlockData());
        }
    }

    @Nullable
    private TileHolder removeTileEntity(@NotNull World world, @NotNull BlockPosition position){
        TileEntity tile = world.getTileEntity(position);
        NextTickListEntry tick = tickProvider.getNextTick((WorldServer) world, position);
        if(tile == null && tick == null){
            return null;
        }
        boolean isCaptured = world.capturedTileEntities.remove(position) != null;
        world.getChunkAtWorldCoords(position).getTileEntities().remove(position);
        return new TileHolder(tile, tick, position, isCaptured);
    }

    @NotNull
    private BlockPosition locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPosition(loc.getX(), loc.getY(), loc.getZ());
    }

    private void setBlockFast(@NotNull World world, @NotNull BlockPosition position, @NotNull IBlockData data) {
        Chunk chunk = world.getChunkAtWorldCoords(position);
        ChunkSection chunkSection = chunk.getSections()[position.getY()>>4];
        if (chunkSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.a(position, Blocks.GLASS.getBlockData());
            chunkSection = chunk.getSections()[position.getY() >> 4];
        }
        if(chunkSection.getType(position.getX()&15, position.getY()&15, position.getZ()&15).equals(data)){
            //Block is already of correct type and data, don't overwrite
            return;
        }
        chunkSection.setType(position.getX()&15, position.getY()&15, position.getZ()&15, data);
        world.notify(position, data, data, 3);
        chunk.e(); // mark dirty
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
            method = Block.class.getDeclaredMethod("d", int.class);
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

    private void moveTileEntity(@NotNull World nativeWorld, @NotNull BlockPosition newPosition, @NotNull TileEntity tile, boolean captured){
        Chunk chunk = nativeWorld.getChunkAtWorldCoords(newPosition);
        if(captured){
            nativeWorld.capturedTileEntities.put(newPosition, tile);
        }
        tile.a(nativeWorld);
        tile.setPosition(newPosition);
        chunk.tileEntities.put(newPosition, tile);
    }

    private static class TileHolder{
        @Nullable private final TileEntity tile;
        @Nullable
        private NextTickListEntry nextTick;
        @NotNull private final BlockPosition tilePosition;
        private final boolean isCaptured;

        public TileHolder(@Nullable TileEntity tile, @Nullable NextTickListEntry nextTick, @NotNull BlockPosition tilePosition, boolean isCaptured){
            this.tile = tile;
            this.nextTick = nextTick;
            this.tilePosition = tilePosition;
            this.isCaptured = isCaptured;
        }


        @Nullable
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

        public boolean isCaptured() {
            return isCaptured;
        }
    }
}
