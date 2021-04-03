package net.countercraft.movecraft.support.v1_16_R3;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.support.AsyncChunk;
import net.countercraft.movecraft.support.MovecraftState;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.TileEntityBanner;
import net.minecraft.server.v1_16_R3.TileEntityBarrel;
import net.minecraft.server.v1_16_R3.TileEntityBeacon;
import net.minecraft.server.v1_16_R3.TileEntityBed;
import net.minecraft.server.v1_16_R3.TileEntityBeehive;
import net.minecraft.server.v1_16_R3.TileEntityBell;
import net.minecraft.server.v1_16_R3.TileEntityBlastFurnace;
import net.minecraft.server.v1_16_R3.TileEntityBrewingStand;
import net.minecraft.server.v1_16_R3.TileEntityCampfire;
import net.minecraft.server.v1_16_R3.TileEntityChest;
import net.minecraft.server.v1_16_R3.TileEntityCommand;
import net.minecraft.server.v1_16_R3.TileEntityComparator;
import net.minecraft.server.v1_16_R3.TileEntityConduit;
import net.minecraft.server.v1_16_R3.TileEntityDispenser;
import net.minecraft.server.v1_16_R3.TileEntityDropper;
import net.minecraft.server.v1_16_R3.TileEntityEnchantTable;
import net.minecraft.server.v1_16_R3.TileEntityEndGateway;
import net.minecraft.server.v1_16_R3.TileEntityEnderChest;
import net.minecraft.server.v1_16_R3.TileEntityFurnaceFurnace;
import net.minecraft.server.v1_16_R3.TileEntityHopper;
import net.minecraft.server.v1_16_R3.TileEntityJigsaw;
import net.minecraft.server.v1_16_R3.TileEntityJukeBox;
import net.minecraft.server.v1_16_R3.TileEntityLectern;
import net.minecraft.server.v1_16_R3.TileEntityLightDetector;
import net.minecraft.server.v1_16_R3.TileEntityMobSpawner;
import net.minecraft.server.v1_16_R3.TileEntityShulkerBox;
import net.minecraft.server.v1_16_R3.TileEntitySign;
import net.minecraft.server.v1_16_R3.TileEntitySkull;
import net.minecraft.server.v1_16_R3.TileEntitySmoker;
import net.minecraft.server.v1_16_R3.TileEntityStructure;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBanner;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBarrel;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBeacon;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBed;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBeehive;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBell;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlastFurnace;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftBrewingStand;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCampfire;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftChest;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftComparator;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftConduit;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftCreatureSpawner;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftDaylightDetector;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftDispenser;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftDropper;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftEnchantingTable;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftEndGateway;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftEnderChest;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftFurnaceFurnace;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftHopper;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftJigsaw;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftJukebox;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftLectern;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftShulkerBox;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftSign;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftSkull;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftSmoker;
import org.bukkit.craftbukkit.v1_16_R3.block.CraftStructureBlock;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class IAsyncChunk extends AsyncChunk<CraftChunk> {

    public IAsyncChunk(@NotNull Chunk chunk) {
        super(chunk);
    }

    @NotNull
    @Override
    protected CraftChunk adapt(@NotNull org.bukkit.Chunk chunk) {
        return (CraftChunk) chunk;
    }

    @NotNull
    @Override
    public BlockState getState(@NotNull MovecraftLocation location) {
        var material = getType(location);
        var block = chunk.getBlock(location.getX(), location.getY(), location.getZ());
        var worldPosition = ((CraftBlock) block).getPosition();
        var tile = chunk.getHandle().getTileEntity(new BlockPosition(location.getX(), location.getY(), location.getZ()));
        if(tile == null){
            return new MovecraftState(null, getData(location), material);
        }
        switch(material.ordinal()) {
            case 81:
                return new CraftDispenser(material, (TileEntityDispenser) tile);
            case 179:
                return new CraftCreatureSpawner(material, (TileEntityMobSpawner) tile);
            case 181:
            case 318:
                return new CraftChest(material, (TileEntityChest) tile);
            case 186:
                return new CraftFurnaceFurnace(material, (TileEntityFurnaceFurnace) tile);
            case 208:
                return new CraftJukebox(material, (TileEntityJukeBox) tile);
            case 270:
                return new CraftEnchantingTable(material, (TileEntityEnchantTable) tile);
            case 278:
                return new CraftEnderChest(material, (TileEntityEnderChest) tile);
            case 286:
            case 423:
            case 424:
                return new CraftCommandBlock(material, (TileEntityCommand) tile);
            case 287:
                return new CraftBeacon(material, (TileEntityBeacon) tile);
            case 321:
                return new CraftDaylightDetector(material, (TileEntityLightDetector) tile);
            case 324:
                return new CraftHopper(material, (TileEntityHopper) tile);
            case 331:
                return new CraftDropper(material, (TileEntityDropper) tile);
            case 432:
            case 433:
            case 434:
            case 435:
            case 436:
            case 437:
            case 438:
            case 439:
            case 440:
            case 441:
            case 442:
            case 443:
            case 444:
            case 445:
            case 446:
            case 447:
            case 448:
                return new CraftShulkerBox(material, (TileEntityShulkerBox) tile);
            case 529:
                return new CraftConduit(material, (TileEntityConduit) tile);
            case 568:
                return new CraftComparator(material, (TileEntityComparator) tile);
            case 569:
                return new CraftStructureBlock(material, (TileEntityStructure) tile);
            case 570:
                return new CraftJigsaw(material, (TileEntityJigsaw) tile);
            case 653:
            case 654:
            case 655:
            case 656:
            case 657:
            case 658:
            case 659:
            case 660:
            case 986:
            case 987:
            case 988:
            case 989:
            case 990:
            case 991:
            case 1072:
            case 1073:
                return new CraftSign(material, (TileEntitySign) tile);
            case 717:
            case 718:
            case 719:
            case 720:
            case 721:
            case 722:
            case 723:
            case 724:
            case 725:
            case 726:
            case 727:
            case 728:
            case 729:
            case 730:
            case 731:
            case 732:
                return new CraftBed(material, (TileEntityBed) tile);
            case 756:
                return new CraftBrewingStand(material, (TileEntityBrewingStand) tile);
            case 837:
            case 838:
            case 839:
            case 840:
            case 841:
            case 842:
            case 1028:
            case 1029:
            case 1030:
            case 1031:
            case 1032:
            case 1033:
                return new CraftSkull(material, (TileEntitySkull) tile);
            case 871:
            case 872:
            case 873:
            case 874:
            case 875:
            case 876:
            case 877:
            case 878:
            case 879:
            case 880:
            case 881:
            case 882:
            case 883:
            case 884:
            case 885:
            case 886:
            case 1034:
            case 1035:
            case 1036:
            case 1037:
            case 1038:
            case 1039:
            case 1040:
            case 1041:
            case 1042:
            case 1043:
            case 1044:
            case 1045:
            case 1046:
            case 1047:
            case 1048:
            case 1049:
                return new CraftBanner(material, (TileEntityBanner) tile);
            case 937:
                return new CraftBarrel(material, (TileEntityBarrel) tile);
            case 938:
                return new CraftSmoker(material, (TileEntitySmoker) tile);
            case 939:
                return new CraftBlastFurnace(material, (TileEntityBlastFurnace) tile);
            case 943:
                return new CraftLectern(material, (TileEntityLectern) tile);
            case 946:
                return new CraftBell(material, (TileEntityBell) tile);
            case 950:
            case 951:
                return new CraftCampfire(material, (TileEntityCampfire) tile);
            case 954:
            case 955:
                return new CraftBeehive(material, (TileEntityBeehive) tile);
            case 1051:
                return new CraftEndGateway(material, (TileEntityEndGateway) tile);
            default:
                Bukkit.getLogger().severe("Server may deadlock! Report this asap.");
                TileEntity tileEntity = chunk.getCraftWorld().getHandle().getTileEntity(worldPosition);
                return tileEntity != null ? new CraftBlockEntityState<>(material, tileEntity) : new MovecraftState(null, getData(location), material);
        }
    }

    @Override
    @NotNull
    public Material getType(@NotNull MovecraftLocation location){
        return CraftBlockData.fromData(chunk.getHandle().getType(new BlockPosition(location.getX(), location.getY(), location.getZ()))).getMaterial();
    }

    @Override
    @NotNull
    public BlockData getData(@NotNull MovecraftLocation location){
        IBlockData data = chunk.getHandle().getType(new BlockPosition(location.getX(), location.getY(), location.getZ()));
        return CraftBlockData.fromData(data);
    }

}
