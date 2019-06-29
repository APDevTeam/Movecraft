package net.countercraft.movecraft.sign;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRepair;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.WorldEditUpdateCommand;
import net.countercraft.movecraft.repair.Repair;
import net.countercraft.movecraft.repair.RepairManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RepairSign implements Listener{
    private String HEADER = "Repair:";
    private HashMap<UUID, Long> playerInteractTimeMap = new HashMap<>();//Players must be assigned by the UUID, or NullPointerExceptions are thrown
    private final Material[] fragileBlocks = new Material[]{ Material.TORCH, Material.LADDER, Material.WALL_SIGN, Material.LEVER, Material.STONE_BUTTON, Material.TRAP_DOOR, Material.TRIPWIRE_HOOK, Material.WOOD_BUTTON, Material.IRON_TRAPDOOR, Material.DIODE_BLOCK_ON, Material.DIODE_BLOCK_OFF, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR_OFF, Material.REDSTONE_WIRE, Material.BED_BLOCK};

    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if (!event.getLine(0).equalsIgnoreCase(HEADER)){
            return;
        }
        if (event.getLine(1).isEmpty()){
            event.getPlayer().sendMessage("You must specify a repair state name on second line");
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event){
        String firstLine = "";
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) event.getClickedBlock().getState();
                String signText = ChatColor.stripColor(sign.getLine(0));

                if (signText == null) {
                    return;
                }
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Sign) {
                signRightClick(event);
            }
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            BlockState state = event.getClickedBlock().getState();
            if (state instanceof Sign) {
                signLeftClick(event);
            }
        }
    }
    //
    private void signLeftClick(PlayerInteractEvent event){

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equalsIgnoreCase(HEADER) || sign.getLine(0) == null){
            return;
        }
        String repairName = event.getPlayer().getName();
        repairName += "_";
        repairName += sign.getLine(1);
        if (Settings.RepairTicksPerBlock == 0) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Repair functionality is disabled or WorldEdit was not detected"));
            return;
        }
        Craft pCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (pCraft == null) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }

        MovecraftRepair movecraftRepair = MovecraftRepair.getInstance();
        if (movecraftRepair.saveCraftRepairState(pCraft, sign, repairName))
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("State saved"));
        else
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Could not save file"));
        event.setCancelled(true);

    }
    private void signRightClick(PlayerInteractEvent event){
        Sign sign = (Sign) event.getClickedBlock().getState();
        World world = sign.getWorld();
        Player p = event.getPlayer();
        Craft pCraft = CraftManager.getInstance().getCraftByPlayer(p);
        if (!sign.getLine(0).equalsIgnoreCase(HEADER)){
            return;
        }
        if (pCraft == null){
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return;
        }
        if (Settings.RepairTicksPerBlock == 0) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Repair functionality is disabled or WorldEdit was not detected"));
            return;
        }
        String repairName = event.getPlayer().getName();
        repairName += "_";
        repairName += sign.getLine(1);
        MovecraftRepair movecraftRepair = MovecraftRepair.getInstance();
        Clipboard clipboard = movecraftRepair.loadCraftRepairStateClipboard(pCraft, sign, repairName, world);


        if (clipboard == null){
            p.sendMessage(I18nSupport.getInternationalisedString("REPAIR STATE NOT FOUND"));
        } else { //if clipboard is not null
            long numDifferentBlocks = movecraftRepair.getNumDiffBlocks(repairName);
            boolean secondClick = false;
            if (!playerInteractTimeMap.isEmpty()) {
                if (System.currentTimeMillis() - playerInteractTimeMap.get(p.getUniqueId()) < 5000) {
                    secondClick = true;
                }
            }
            HashMap<Material, Double> numMissingItems = movecraftRepair.getMissingBlocks(repairName);
            ArrayDeque<ImmutablePair<Vector, Vector>> locMissingBlocks = movecraftRepair.getMissingBlockLocations(repairName);
            int totalSize = locMissingBlocks.size() + pCraft.getHitBox().size();

            if (secondClick){
                // check all the chests for materials for the repair
                HashMap<Material, ArrayList<InventoryHolder>> chestsToTakeFrom = new HashMap<>(); // typeid, list of chest inventories
                boolean enoughMaterial = true;
                for (Material type : numMissingItems.keySet()) {
                    long longRemQty = Math.round(numMissingItems.get(type));
                    int remainingQty = (int) longRemQty;
                    ArrayList<InventoryHolder> chests = new ArrayList<>();
                    for (MovecraftLocation loc : pCraft.getHitBox()) {
                        Block b = pCraft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
                        if ((b.getType() == Material.CHEST) || (b.getType() == Material.TRAPPED_CHEST)) {
                            InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                            if (inventoryHolder.getInventory().contains(type) && remainingQty > 0) {
                                HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(type);
                                // count how many were in the chest
                                int numfound = 0;
                                for (ItemStack istack : foundItems.values()) {
                                    numfound += istack.getAmount();
                                }
                                remainingQty -= numfound;
                                chests.add(inventoryHolder);
                            }
                        }
                    }
                    if (remainingQty > 0) {
                        event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Need more of material") + ": %s - %d", type.name().toLowerCase().replace("_", " "), remainingQty));
                        enoughMaterial = false;
                    } else {
                        chestsToTakeFrom.put(type, chests);
                    }
                }
                if (Movecraft.getInstance().getEconomy() != null && enoughMaterial) {
                    double moneyCost = numDifferentBlocks * Settings.RepairMoneyPerBlock;
                    if (Movecraft.getInstance().getEconomy().has(event.getPlayer(), moneyCost)) {
                        Movecraft.getInstance().getEconomy().withdrawPlayer(event.getPlayer(), moneyCost);
                    } else {
                        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("You do not have enough money"));
                        enoughMaterial = false;
                    }
                }
                if (enoughMaterial) {
                    // we know we have enough materials to make the repairs, so remove the materials from the chests
                    for (Material type : numMissingItems.keySet()) {
                        int remainingQty = (int) Math.round(numMissingItems.get(type));
                        for (InventoryHolder inventoryHolder : chestsToTakeFrom.get(type)) {
                            HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(type);
                            for (ItemStack istack : foundItems.values()) {
                                if (istack.getAmount() <= remainingQty) {
                                    remainingQty -= istack.getAmount();
                                    inventoryHolder.getInventory().removeItem(istack);
                                } else {
                                    istack.setAmount(istack.getAmount() - remainingQty);
                                    remainingQty = 0;
                                }
                            }

                        }
                    }

                    double Cost = numDifferentBlocks * Settings.RepairMoneyPerBlock;
                    Movecraft.getInstance().getLogger().info(event.getPlayer().getName() + " has begun a repair with the cost of " + Cost);
                    final LinkedList<UpdateCommand> updateCommands = new LinkedList<>();
                    final LinkedList<UpdateCommand> updateCommandsFragileBlocks = new LinkedList<>();
                    final Vector distance = movecraftRepair.getDistance(repairName);
                    while (!locMissingBlocks.isEmpty()){
                        ImmutablePair<Vector,Vector> locs = locMissingBlocks.pollFirst();
                        assert locs != null;
                        Vector cLoc = locs.getRight();
                        MovecraftLocation moveLoc = new MovecraftLocation(locs.getLeft().getBlockX(), locs.getLeft().getBlockY(), locs.getLeft().getBlockZ());
                        //To avoid any issues during the repair, keep certain blocks in different linked lists
                            BaseBlock baseBlock = clipboard.getBlock(new com.sk89q.worldedit.Vector(cLoc.getBlockX(),cLoc.getBlockY(),cLoc.getBlockZ()));
                            Material type =  Material.getMaterial(baseBlock.getType());
                            if (fragileBlock(type)) {
                                WorldEditUpdateCommand updateCommand = new WorldEditUpdateCommand(baseBlock, sign.getWorld(), moveLoc,type, (byte) baseBlock.getData());
                                updateCommandsFragileBlocks.add(updateCommand);
                            } else {
                                WorldEditUpdateCommand updateCommand = new WorldEditUpdateCommand(baseBlock, sign.getWorld(), moveLoc, type, (byte) baseBlock.getData());
                                updateCommands.add(updateCommand);
                            }

                    }
                    if (updateCommands.size() > 0) {
                        final Craft releaseCraft = pCraft;
                        CraftManager.getInstance().removePlayerFromCraft(pCraft);
                        RepairManager repairManager = Movecraft.getInstance().getRepairManager();
                        repairManager.getRepairs().add(new Repair(sign.getLine(1), releaseCraft, updateCommands, updateCommandsFragileBlocks,  p.getUniqueId(), numDifferentBlocks, sign.getLocation()));
                    }

                }

            } else {
                float percent = ((float) numDifferentBlocks / (float) totalSize) * 100;
                p.sendMessage(I18nSupport.getInternationalisedString("Total damaged blocks") + ": " + numDifferentBlocks);
                p.sendMessage(I18nSupport.getInternationalisedString("Percentage of craft") + ": " + percent);
                if (percent > Settings.RepairMaxPercent){
                    p.sendMessage(I18nSupport.getInternationalisedString("This craft is too damaged and can not be repaired"));
                    return;
                }
                if (numDifferentBlocks != 0) {
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SUPPLIES NEEDED"));
                    for (Material blockType : numMissingItems.keySet()) {
                        event.getPlayer().sendMessage(String.format("%s : %d", blockType.name().toLowerCase().replace("_", " "), Math.round(numMissingItems.get(blockType))));
                    }
                    long durationInSeconds = numDifferentBlocks * Settings.RepairTicksPerBlock / 20;
                    event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Seconds to complete repair") + ": %d", durationInSeconds));
                    int moneyCost = (int) (numDifferentBlocks * Settings.RepairMoneyPerBlock);
                    event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Money to complete repair") + ": %d", moneyCost));
                    playerInteractTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                }
            }
        }
    }

    private boolean fragileBlock(Material type){
        return type.name().endsWith("BUTTON") ||type.name().endsWith("DOOR") || Arrays.binarySearch(fragileBlocks,type) >= 0;
    }
}
