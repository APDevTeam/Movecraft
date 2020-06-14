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
    private final List<Repair> repairs = new CopyOnWriteArrayList<>();
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
        File craftRepairStateDir = new File(Movecraft.getInstance().getDataFolder(),"CraftRepairStates");
        if (!repairStateDir.exists()||!craftRepairStateDir.exists()){
            return;
        }
        int convertedRepairStates = 0;
        int repairStatesWithUnknownOwner = 0;
        int failedConversions = 0;
        File[] repairStates = repairStateDir.listFiles();
        if (repairStates == null) {
            return;
        }
        HashMap<String, OfflinePlayer> nameIDMap = new HashMap<>();
        for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()){
            nameIDMap.put(offlinePlayer.getName(), offlinePlayer);
        }

        for (File rs : repairStates){
            String fileName = rs.getName();
            if (!fileName.contains(".schematic")){
                continue;
            }
            List<String> similarPlayerNames = new ArrayList<>();
            OfflinePlayer owner = null;
            String possibleName = fileName.substring(0,2);
            for(int i = 2; i < fileName.length(); i++){
                possibleName += fileName.charAt(i);
                if(nameIDMap.containsKey(possibleName)){
                    owner = nameIDMap.get(possibleName);
                    similarPlayerNames.add(possibleName);
                }
            }
            if (owner == null){
                repairStatesWithUnknownOwner++;
                Movecraft.getInstance().getLogger().warning(String.format(I18nSupport.getInternationalisedString("Repair - Invalid Player Name"), fileName));
                continue;
            }
            if (similarPlayerNames.size() > 1) {
                String warning = String.join(", ", similarPlayerNames);
                Movecraft.getInstance().getLogger().warning(I18nSupport.getInternationalisedString("RepairStateConversion - Similar players found") + warning);
                confirmedSimilarPlayerNames.push(similarPlayerNames);
                continue;
            }
            if (!confirmedRepairStates.containsKey(owner.getUniqueId())) {
                confirmedRepairStates.put(owner.getUniqueId(), new ArrayList<>());
            }
            confirmedRepairStates.get(owner.getUniqueId()).add(rs);
        }
        File[] craftRepairStates = craftRepairStateDir.listFiles();
        if (craftRepairStateDir.exists() && (craftRepairStates != null || craftRepairStates.length > 0)){
            for (File rs : craftRepairStates){
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
                    String warning = String.join(", ", similarPlayerNames);
                    Movecraft.getInstance().getLogger().warning(I18nSupport.getInternationalisedString("RepairStateConversion - Similar players found") + warning);
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
            File playerDir = new File(repairStateDir, id.toString());
            if (!playerDir.exists()) {
                if(!playerDir.mkdirs()){
                    Movecraft.getInstance().getLogger().severe("Failed to generate folder for uuid " + id);
                    continue;
                }
            }
            for (File file : confirmedRepairStates.get(id)) {

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
                Movecraft.getInstance().getLogger().severe(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Conversion Failed"), fileName));
                failedConversions++;
            }
        }
        //Then check the similar names
        while (!confirmedSimilarPlayerNames.isEmpty()){
            List<String> similarPlayerNames = confirmedSimilarPlayerNames.poll();
            similarPlayerNames.sort(Comparator.comparing(String::length));
            //Iterate over the name array in descending order as the last object is the longest one
            for (int i = similarPlayerNames.size() - 1 ; i >= 0  ; i--){
                String pName = similarPlayerNames.get(i);
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
                    Movecraft.getInstance().getLogger().severe(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Conversion Failed"), fileName));
                    failedConversions++;
                }
                for (File rs : craftRepairStateDir.listFiles()){
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
                    Movecraft.getInstance().getLogger().severe(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Conversion Failed"), fileName));
                    failedConversions++;
                }
            }
        }
        //Finally, put statistics out in the log
        if (convertedRepairStates == 0 && repairStatesWithUnknownOwner == 0 && failedConversions == 0){
            return;
        }
        Logger log = Movecraft.getInstance().getLogger();
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Successful conversions"), convertedRepairStates));
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Repair States With Unknown Owner"), repairStatesWithUnknownOwner));
        log.info(String.format(I18nSupport.getInternationalisedString("RepairStateConversion - Failed conversions"), failedConversions));
    }
    public List<Repair> getRepairs() {
        return repairs;
    }
}
