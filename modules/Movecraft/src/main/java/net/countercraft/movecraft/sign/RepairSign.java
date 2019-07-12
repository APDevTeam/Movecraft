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
import net.countercraft.movecraft.repair.RepairUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private final String HEADER = "Repair:";
    private static final ArrayList<Character> ILLEGAL_CHARACTERS = new ArrayList<>();//{};
    private final HashMap<UUID, Long> playerInteractTimeMap = new HashMap<>();//Players must be assigned by the UUID, or NullPointerExceptions are thrown
    static {
        ILLEGAL_CHARACTERS.add('/');
        ILLEGAL_CHARACTERS.add('\\');
        ILLEGAL_CHARACTERS.add(':');
        ILLEGAL_CHARACTERS.add('*');
        ILLEGAL_CHARACTERS.add('?');
        ILLEGAL_CHARACTERS.add('\"');
        ILLEGAL_CHARACTERS.add('<');
        ILLEGAL_CHARACTERS.add('>');
        ILLEGAL_CHARACTERS.add('|');
    }
    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase(HEADER)){
            return;
        }
        //Clear the repair sign if second line is empty
        if (event.getLine(1).isEmpty()){
            event.getPlayer().sendMessage("You must specify a repair state name on second line");
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        String signText = ChatColor.stripColor(sign.getLine(0));
        if (signText == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            signRightClick(event);
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            signLeftClick(event);
        }

    }
    //
    private void signLeftClick(PlayerInteractEvent event){
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER) || sign.getLine(0) == null){
            return;
        }
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
        event.setCancelled(true);
        if (movecraftRepair.saveCraftRepairState(pCraft, sign)) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("State saved"));
            return;
        }
        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Could not save file"));
    }
    private void signRightClick(PlayerInteractEvent event){
        Sign sign = (Sign) event.getClickedBlock().getState();
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
        repairName += ChatColor.stripColor(sign.getLine(1));
        MovecraftRepair movecraftRepair = MovecraftRepair.getInstance();
        Clipboard clipboard = movecraftRepair.loadCraftRepairStateClipboard(pCraft, sign);


        if (clipboard == null){
            p.sendMessage(I18nSupport.getInternationalisedString("REPAIR STATE NOT FOUND"));
            return;
        } //if clipboard is not null
        long numDifferentBlocks = movecraftRepair.getNumDiffBlocks(repairName);
        boolean secondClick = false;
        if (!playerInteractTimeMap.isEmpty()) {
            if (System.currentTimeMillis() - playerInteractTimeMap.get(p.getUniqueId()) < 5000) {
                    secondClick = true;
            }
        }
        HashMap<ImmutablePair<Material, Integer>, Double> numMissingItems = movecraftRepair.getMissingBlocks(repairName);
        ArrayDeque<ImmutablePair<Vector, Vector>> locMissingBlocks = movecraftRepair.getMissingBlockLocations(repairName);
        int totalSize = locMissingBlocks.size() + pCraft.getHitBox().size();
        if (secondClick){
            // check all the chests for materials for the repair
            HashMap<ImmutablePair<Material, Integer>, ArrayList<InventoryHolder>> chestsToTakeFrom = new HashMap<>(); // typeid, list of chest inventories
            boolean enoughMaterial = true;
            for (ImmutablePair<Material, Integer> type : numMissingItems.keySet()) {
                long longRemQty = Math.round(numMissingItems.get(type));
                int remainingQty = (int) longRemQty;
                ArrayList<InventoryHolder> chests = new ArrayList<>();
                boolean requireSpecific = Settings.RepairRequireSpecificMaterials.containsKey(type.getLeft()) && Settings.RepairRequireSpecificMaterials.get(type.getLeft()).contains(type.getRight());
                for (MovecraftLocation loc : pCraft.getHitBox()) {
                    Block b = pCraft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
                    if ((b.getType() == Material.CHEST) || (b.getType() == Material.TRAPPED_CHEST)) {
                        InventoryHolder inventoryHolder = (InventoryHolder) b.getState();

                        if (inventoryHolder.getInventory().contains(type.getLeft()) && remainingQty > 0) {
                            HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(type.getLeft());
                            // count how many were in the chest
                            int numfound = 0;

                            for (ItemStack istack : foundItems.values()) {
                                if (requireSpecific && istack.getData().getData() != type.getRight()){
                                    continue;
                                }
                                numfound += istack.getAmount();
                            }
                            remainingQty -= numfound;
                            chests.add(inventoryHolder);
                        }
                    }
                }
                if (remainingQty > 0) {
                    event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Need more of material") + ": %s - %d", requireSpecific ? RepairUtils.specificName(type.getLeft(), type.getRight()) : type.getLeft().name().toLowerCase().replace("_", " "), remainingQty));
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
                    event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Economy - Not Enough Money"));
                    enoughMaterial = false;
                }
            }
            if (enoughMaterial) {
                // we know we have enough materials to make the repairs, so remove the materials from the chests
                for (ImmutablePair<Material, Integer> type : numMissingItems.keySet()) {
                    boolean requireSpecific = Settings.RepairRequireSpecificMaterials.containsKey(type.getLeft()) && Settings.RepairRequireSpecificMaterials.get(type.getLeft()).contains(type.getRight());
                    int remainingQty = (int) Math.round(numMissingItems.get(type));
                    for (InventoryHolder inventoryHolder : chestsToTakeFrom.get(type)) {
                        HashMap<Integer, ? extends ItemStack> foundItems = inventoryHolder.getInventory().all(type.getLeft());

                        for (ItemStack istack : foundItems.values()) {
                            if (requireSpecific && istack.getData().getData() != type.getRight()){
                                continue;
                            }
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

                double cost = numDifferentBlocks * Settings.RepairMoneyPerBlock;
                Movecraft.getInstance().getLogger().info(String.format(I18nSupport.getInternationalisedString("Repair - Repair Has Begun"),event.getPlayer().getName(),cost));
                final LinkedList<UpdateCommand> updateCommands = new LinkedList<>();
                final LinkedList<UpdateCommand> updateCommandsFragileBlocks = new LinkedList<>();

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
                if (!updateCommands.isEmpty() || !updateCommandsFragileBlocks.isEmpty()) {
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
                p.sendMessage(I18nSupport.getInternationalisedString("Repair - Failed Craft Too Damaged"));
                return;
            }
            if (numDifferentBlocks != 0) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("SUPPLIES NEEDED"));
                for (ImmutablePair<Material, Integer> blockType : numMissingItems.keySet()) {
                    boolean requireSpecific = Settings.RepairRequireSpecificMaterials.containsKey(blockType.getLeft()) && Settings.RepairRequireSpecificMaterials.get(blockType.getLeft()).contains(blockType.getRight());
                    event.getPlayer().sendMessage(String.format("%s : %d",requireSpecific ? RepairUtils.specificName(blockType.getLeft(), blockType.getRight()) : blockType.getLeft().name().replace("_", " ").toLowerCase(), Math.round(numMissingItems.get(blockType))));
                }
                long durationInSeconds = numDifferentBlocks * Settings.RepairTicksPerBlock / 20;
                event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Seconds to complete repair") + ": %d", durationInSeconds));
                int moneyCost = (int) (numDifferentBlocks * Settings.RepairMoneyPerBlock);
                event.getPlayer().sendMessage(String.format(I18nSupport.getInternationalisedString("Money to complete repair") + ": %d", moneyCost));
                playerInteractTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    private boolean fragileBlock(Material type){
        return type.name().endsWith("BUTTON")
                || type.name().endsWith("DOOR_BLOCK")
                || type.name().startsWith("DIODE")
                || type.name().startsWith("REDSTONE_COMPARATOR")
                || type.equals(Material.LEVER)
                || type.equals(Material.WALL_SIGN)
                || type.equals(Material.WALL_BANNER)
                || type.equals(Material.REDSTONE_WIRE)
                || type.equals(Material.LADDER)
                || type.equals(Material.BED_BLOCK)
                || type.equals(Material.TRIPWIRE_HOOK);
    }
}
