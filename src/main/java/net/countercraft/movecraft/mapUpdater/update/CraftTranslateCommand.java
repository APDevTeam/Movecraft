package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.EntityPlayer;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.NextTickListEntry;
import net.minecraft.server.v1_10_R1.Packet;
import net.minecraft.server.v1_10_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_10_R1.PlayerChunk;
import net.minecraft.server.v1_10_R1.StructureBoundingBox;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class CraftTranslateCommand extends UpdateCommand {
    private Logger logger = Movecraft.getInstance().getLogger();
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;
    @NotNull private final Rotation rotation;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement) {
        this(craft,displacement, Rotation.NONE);
    }

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull Rotation rotation){
        this.craft = craft;
        this.displacement = displacement;
        this.rotation = rotation;
    }

    @Override
    public void doUpdate() {
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
            moveTileEntity(tileHolder.getTilePosition().a(translateVector),tileHolder.getTile());
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
        /*ChunkSection newSection = chunk.getSections()[position.getY() >> 4];
        if (newSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.a(position, Blocks.GLASS.getBlockData());
            newSection = chunk.getSections()[position.getY() >> 4];
        }
        newSection.setType(position.getX() & 15, position.getY() & 15, position.getZ() & 15, CraftMagicNumbers.getBlock(Material.WOOL).getBlockData());*/
    }

    private void moveTileEntity(@NotNull BlockPosition newPosition, @NotNull TileEntity tile){
        logger.info("get the world");
        World nativeWorld = ((CraftWorld) craft.getW()).getHandle();
        logger.info("get the chunk");
        Chunk chunk = nativeWorld.getChunkAtWorldCoords(newPosition);
        if(nativeWorld.captureBlockStates) {
            logger.info("capturing states");
            tile.a(nativeWorld);
            tile.setPosition(newPosition);
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        logger.info("setting position");
        tile.setPosition(newPosition);
        logger.info("putting tiles");
        chunk.tileEntities.put(newPosition, tile);
    }

    @NotNull
    public Craft getCraft(){
        return craft;
    }

    private void sendToPlayers(Chunk... chunks){
        for(Chunk chunk : chunks) {
            if(craft.getW().unloadChunk(chunk.locX, chunk.locZ, true, true)) // nobody there? dont bother, save memory.
                continue;
            Collection<TileEntity> lti = chunk.tileEntities.values();

            PacketPlayOutMapChunk pmc = new PacketPlayOutMapChunk(chunk, 65535);
            //PacketPlayOutUnloadChunk puc = new PacketPlayOutUnloadChunk(cu.x,cu.z); // may be needed in some situations or for some clients
            ArrayList<Packet<?>> te = new ArrayList<Packet<?>>(16);

            for (TileEntity ti : lti) {
                Packet<?> p = ti.getUpdatePacket();
                if (p != null)
                    te.add(p);
            }

            // get the playerchunk to see what players have the chunk loaded, more effective that trying to guess with view distance?
            PlayerChunk pc = ((net.minecraft.server.v1_10_R1.WorldServer) (craft.getW())).getPlayerChunkMap().getChunk(chunk.locX, chunk.locZ);

            if (pc == null) {
                //cu.w.getWorld().unloadChunk(cu.x, cu.z, true, false);
                logger.warning("FastBlockChanger.sendToPlayers: No player in range to send " + chunk.locX + "," + chunk.locZ + "  chunk unloaded=" + (craft.getW().unloadChunk(chunk.locX, chunk.locZ, true, false)));
                continue;
            }
            ArrayList<EntityPlayer> pl = new ArrayList<EntityPlayer>(pc.c.size());
            if( pl.size() == 0) {
                logger.warning("FastBlockChanger.sendToPlayers: No player in range to send " + chunk.locX + "," + chunk.locZ + "  chunk unloaded=" + (craft.getW().unloadChunk(chunk.locX, chunk.locZ, true, false)));
                continue;
            }
            pl.addAll(pc.c);
            for (EntityPlayer p : pl) {
                p.playerConnection.sendPacket(pmc); // send the chunk
                for (Packet<?> packet : te) {
                    p.playerConnection.sendPacket(packet); // send the tile entities
                }
            }

        }

    }

    private class TileHolder{
        @NotNull private final TileEntity tile;
        @Nullable private final NextTickListEntry nextTick;
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
