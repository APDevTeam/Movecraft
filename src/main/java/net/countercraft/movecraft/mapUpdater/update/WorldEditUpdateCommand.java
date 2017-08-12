package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.DispenserBlock;
import com.sk89q.worldedit.blocks.SignBlock;
import net.countercraft.movecraft.utils.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

public class WorldEditUpdateCommand extends UpdateCommand {
    private final BaseBlock worldEditBaseBlock;
    private World world;
    private MovecraftLocation location;
    private Material type;
    private byte data;

    public WorldEditUpdateCommand(BaseBlock worldEditBaseBlock, World world, MovecraftLocation location, Material type, byte data) {
        this.worldEditBaseBlock = worldEditBaseBlock;
        this.world = world;
        this.location = location;
        this.type = type;
        this.data = data;
    }

    @Override
    public void doUpdate() {
        world.getBlockAt(location.getX(), location.getY(), location.getZ()).setType(type);
        world.getBlockAt(location.getX(), location.getY(), location.getZ()).setData(data);
        // put inventory into dispensers if its a repair
        if (type == Material.DISPENSER) {
            DispenserBlock dispBlock = new DispenserBlock(worldEditBaseBlock.getData());
            dispBlock.setNbtData(worldEditBaseBlock.getNbtData());
            int numFireCharges = 0;
            int numTNT = 0;
            int numWater = 0;
            for (BaseItemStack bi : dispBlock.getItems()) {
                if (bi != null) {
                    if (bi.getType() == 46)
                        numTNT += bi.getAmount();
                    if (bi.getType() == 385)
                        numFireCharges += bi.getAmount();
                    if (bi.getType() == 326)
                        numWater += bi.getAmount();
                }
            }
            Dispenser disp = (Dispenser) world.getBlockAt(location.getX(), location.getY(), location.getZ()).getState();
            if (numFireCharges > 0) {
                ItemStack fireItems = new ItemStack(Material.FIREBALL, numFireCharges);
                disp.getInventory().addItem(fireItems);
            }
            if (numTNT > 0) {
                ItemStack TNTItems = new ItemStack(Material.TNT, numTNT);
                disp.getInventory().addItem(TNTItems);
            }
            if (numWater > 0) {
                ItemStack WaterItems = new ItemStack(Material.WATER_BUCKET, numWater);
                disp.getInventory().addItem(WaterItems);
            }
        }
        if (worldEditBaseBlock instanceof SignBlock) {
            BlockState state = world.getBlockAt(location.getX(), location.getY(), location.getZ()).getState();
            if (state instanceof Sign) {
                Sign s = (Sign) state;
                SignBlock signBlock = (SignBlock) worldEditBaseBlock;
                for (int line = 0; line < signBlock.getText().length; line++) {
                    s.setLine(line, signBlock.getText()[line]);
                }
                s.update(false, false);
            }
        }
        //might have issues due to repair order
    }
}
