package net.countercraft.movecraft.compat.v1_8_R3;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.CollectionUtils;
import net.countercraft.movecraft.utils.MathUtils;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class IWorldHandler extends WorldHandler {
    private final HashMap<World, List<TileEntity>> bMap = new HashMap<>();
    private MethodHandle internalTeleportMH;
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
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        World nativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : rotatedPositions.keySet()){
            //TileEntity tile = nativeWorld.removeTileEntity(position);
            TileEntity tile = removeTileEntity(nativeWorld,position);
            if(tile == null)
                continue;
            //tile.a(ROTATION[rotation.ordinal()]);
            //get the nextTick to move with the tile
            tiles.add(new TileHolder(tile, tickProvider.getNextTick((WorldServer)nativeWorld,position), position));
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
            IBlockData rotated = nativeWorld.getType(position);
            //rotate directional blocks
            if (rotated.a().contains(BlockStateDirection.of("facing"))) {
                blockData.put(position, rotated.set(BlockStateDirection.of("facing"), rotateBlock(rotation, rotated)));
                continue;
            }
            //rotate logs too
            if (rotated.getBlock() instanceof BlockLogAbstract) {
                BlockStateEnum<BlockLogAbstract.EnumLogRotation> axis = BlockStateEnum.of("axis", BlockLogAbstract.EnumLogRotation.class);
                BlockLogAbstract.EnumLogRotation logRotation = rotated.get(axis);
                if (logRotation != BlockLogAbstract.EnumLogRotation.NONE && logRotation != BlockLogAbstract.EnumLogRotation.Y) {
                    blockData.put(position, rotated.set(axis, logRotation == BlockLogAbstract.EnumLogRotation.Z ? BlockLogAbstract.EnumLogRotation.X : BlockLogAbstract.EnumLogRotation.Z));
                    continue;
                }
            }
            blockData.put(position, rotated);
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
        Collection<BlockPosition> deletePositions =  CollectionUtils.filter(rotatedPositions.keySet(),rotatedPositions.values());
        for(BlockPosition position : deletePositions){
            setBlockFast(nativeWorld, position, Blocks.AIR.getBlockData());
        }

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
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, org.bukkit.World world) {
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
        WorldServer oldNativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        WorldServer nativeWorld = ((CraftWorld) world).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************

        List<TileHolder> tiles = getTiles(positions, oldNativeWorld);
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
        Collection<BlockPosition> deletePositions = oldNativeWorld == nativeWorld ? CollectionUtils.filter(positions,newPositions) : positions;
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
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPosition position : positions){
            if(nativeWorld.getType(position) == Blocks.AIR.getBlockData())
                continue;
            //TileEntity tile = nativeWorld.removeTileEntity(position);
            TileEntity tile = removeTileEntity(nativeWorld,position);
            if(tile == null)
                continue;
            //get the nextTick to move with the tile

            //nativeWorld.capturedTileEntities.remove(position);
            //nativeWorld.getChunkAtWorldCoords(position).getTileEntities().remove(position);
            tiles.add(new TileHolder(tile, tickProvider.getNextTick(nativeWorld, position), position));

        }
        return tiles;
    }

    private void processTiles(Iterable<TileHolder> tiles, World world, BlockPosition translateVector){
        for(TileHolder tileHolder : tiles){
            moveTileEntity(world, tileHolder.getTilePosition().a(translateVector),tileHolder.getTile());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = world.worldData.getTime();
            world.b(tileHolder.getNextTick().a.a(translateVector), tileHolder.getNextTick().a(), (int) (tileHolder.getNextTick().b - currentTime), tileHolder.getNextTick().c);
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
    private TileEntity removeTileEntity(@NotNull World world, @NotNull BlockPosition position){
        TileEntity tile = world.getTileEntity(position);
        if(tile == null)
            return null;
        //cleanup
        world.capturedTileEntities.remove(position);
        world.getChunkAtWorldCoords(position).getTileEntities().remove(position);
        if(!Settings.IsPaper)
            world.tileEntityList.remove(tile);
        world.h.remove(tile);
        if(!bMap.containsKey(world)){
            try {
                Field bField = World.class.getDeclaredField("b");
                bField.setAccessible(true);
                bMap.put(world, (List<TileEntity>) bField.get(world));
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e1) {
                e1.printStackTrace();
            }
        }
        bMap.get(world).remove(tile);
        return tile;
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
            chunk.a(position, Blocks.GLASS.getBlockData());
            chunkSection = chunk.getSections()[position.getY() >> 4];
        }

        chunkSection.setType(position.getX()&15, position.getY()&15, position.getZ()&15, data);
        world.notify(position);
        world.c(EnumSkyBlock.BLOCK, position);
        chunk.e();
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull Material material, Object data){
        setBlockFast(location, Rotation.NONE, material, data);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull Rotation rotation, @NotNull Material material, Object data) {
        if (!(data instanceof Byte)) {
            throw new IllegalArgumentException("data must be byte value for v1_8_R3");
        }
        IBlockData blockData =  CraftMagicNumbers.getBlock(material).fromLegacyData((byte) data);
        //blockData = blockData.a(ROTATION[rotation.ordinal()]);
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
        tile.E();
        if(nativeWorld.captureBlockStates) {
            tile.a(nativeWorld);
            tile.a(newPosition);
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        tile.a(newPosition);
        chunk.tileEntities.put(newPosition, tile);
    }

    private EnumDirection rotateBlock(Rotation rotation, IBlockData blockData) {
        final EnumDirection eDir = blockData.get(BlockStateDirection.of("facing"));
        if (rotation == Rotation.CLOCKWISE) {
            switch (eDir) {
                case EAST:
                    return EnumDirection.SOUTH;
                case SOUTH:
                    return EnumDirection.WEST;
                case WEST:
                    return EnumDirection.NORTH;
                case NORTH:
                    return EnumDirection.EAST;
            }
        } else if (rotation == Rotation.ANTICLOCKWISE) {
            switch (eDir) {
                case EAST:
                    return EnumDirection.NORTH;
                case SOUTH:
                    return EnumDirection.EAST;
                case WEST:
                    return EnumDirection.SOUTH;
                case NORTH:
                    return EnumDirection.WEST;
            }
        }
        return eDir;
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
