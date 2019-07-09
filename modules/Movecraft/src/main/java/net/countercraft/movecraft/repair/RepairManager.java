package net.countercraft.movecraft.repair;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class RepairManager extends BukkitRunnable {
    private List<Repair> repairs = new CopyOnWriteArrayList<>();
    public RepairManager(){

    }
    @Override
    public void run() {
        for (Repair repair : repairs){
            if (repair.getRunning().get()) {
                new RepairTask(repair).runTask(Movecraft.getInstance());
            }
        }
    }

    public void convertOldCraftRepairStates(){
        Map<UUID, ArrayList<File>> confirmedRepairStates = new HashMap<>();
        LinkedList<List<String>> confirmedSimilarPlayerNames = new LinkedList<>();
        //Check in the old RepairStates folder
        File repairStateDir = new File(Movecraft.getInstance().getDataFolder(),"RepairStates");
        if (!repairStateDir.exists()){
            return;
        }
        int convertedRepairStates = 0;
        int repairStatesWithUnknownOwner = 0;
        int failedConversions = 0;
        File[] repairStates = repairStateDir.listFiles();
        if (repairStateDir.exists() && (repairStates != null || repairStates.length > 0)){
            for (File rs : repairStates){
                List<String> similarPlayerNames = new ArrayList<>();
                if (!rs.getName().contains(".schematic")){
                    continue;
                }
                String fileName = rs.getName();
                OfflinePlayer owner = null;

                for (OfflinePlayer op : Bukkit.getOfflinePlayers()){
                    if (fileName.startsWith(op.getName())){
                        owner = op;
                        similarPlayerNames.add(op.getName());
                    }
                }

                if (similarPlayerNames.size() > 1) {
                    StringBuilder output = new StringBuilder();
                    boolean first = true;
                    for (String name : similarPlayerNames){
                        if (!first){
                            output.append(", ");
                        }
                        output.append(name);
                        first = false;
                    }
                    Movecraft.getInstance().getLogger().warning(I18nSupport.getInternationalisedString("RepairStateConversion - Similar players found") + output.toString());
                    confirmedSimilarPlayerNames.push(similarPlayerNames);
                    continue;
                } else if (similarPlayerNames.isEmpty() || owner == null){
                    repairStatesWithUnknownOwner++;
                    Movecraft.getInstance().getLogger().warning(String.format(I18nSupport.getInternationalisedString("Repair - Invalid Player Name"), fileName));
                    continue;
                }
                if (confirmedRepairStates.containsKey(owner.getUniqueId())) {
                    confirmedRepairStates.get(owner.getUniqueId()).add(rs);
                } else {
                    confirmedRepairStates.put(owner.getUniqueId(), new ArrayList<>());
                    confirmedRepairStates.get(owner.getUniqueId()).add(rs);
                }


            }
        }
        //Move the repair states with confirmed owners to UUID dirs
        for (UUID id : confirmedRepairStates.keySet()){
            String pName = Bukkit.getOfflinePlayer(id).getName();
            for (File file : confirmedRepairStates.get(id)) {
                File playerDir = new File(repairStateDir, id.toString());
                if (!playerDir.exists()) {
                    playerDir.mkdirs();
                }
                String fileName = file.getName();
                //Remove player name from the file
                fileName = fileName.substring(pName.length());
                //if it begins with an underscore, remove that too
                if (fileName.startsWith("_")) {
                    fileName = fileName.substring(1);
                }
                File dest = new File(playerDir, fileName);
                if (file.renameTo(dest)) {
                    convertedRepairStates++;
                    continue;
                }
                failedConversions++;
            }
        }
        //Then check the similar names
        while (!confirmedSimilarPlayerNames.isEmpty()){
            List<String> similarPlayerNames = confirmedSimilarPlayerNames.poll();
            String[] names = similarPlayerNames.toArray(new String[1]);
            Arrays.sort(names, new StringLengthComparator());
            //Iterate over the name array in descending order as the last object is the longest one
            for (int i = names.length - 1 ; i >= 0  ; i--){
                String pName = names[i];
                for (File rs : repairStateDir.listFiles()){
                    String fileName = rs.getName();
                    //Continue if the file doen't start with the player name
                    if (!fileName.startsWith(pName)){
                        continue;
                    }

                    OfflinePlayer owner = null;
                    for (OfflinePlayer op : Bukkit.getOfflinePlayers()){
                        if (!op.getName().equals(pName)){
                            continue;
                        }
                        owner = op;
                    }
                    //If a player with that name is not found, add to count, make a log output and continue the loop
                    if (owner == null){
                        repairStatesWithUnknownOwner++;
                        Movecraft.getInstance().getLogger().warning(String.format(I18nSupport.getInternationalisedString("Repair - Invalid Player Name"), fileName));
                        continue;
                    }
                    //Remove player name from the file
                    fileName = fileName.substring(pName.length());
                    //if it begins with an underscore, remove that too
                    if (fileName.startsWith("_")){
                        fileName = fileName.substring(1);
                    }
                    File playerDir = new File(repairStateDir, owner.getUniqueId().toString());
                    File dest = new File(playerDir, fileName);
                    if (rs.renameTo(dest)){ //If successfully moved to UUID directory, add to count
                        convertedRepairStates++;
                        continue;
                    } //Add failed attempts to move to count as well
                    failedConversions++;
                }
            }
        }
        //Finally, put statistics out in the log
        Logger log = Movecraft.getInstance().getLogger();
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Successful conversions"), convertedRepairStates));
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Repair States With Unknown Owner"), repairStatesWithUnknownOwner));
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Failed conversions"), failedConversions));
    }

    public List<Repair> getRepairs() {
        return repairs;
    }

    private class StringLengthComparator implements Comparator<String>{

        @Override
        public int compare(String o1, String o2) {
            if (o1.length() > o2.length()){
                return 1;
            } else if (o1.length() < o2.length()){
                return -1;
            } else {
                return 0;
            }
        }
    }
}
