package net.countercraft.movecraft.commands;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static net.countercraft.movecraft.util.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class MovecraftCommandManager extends PaperCommandManager {
    public MovecraftCommandManager(Plugin plugin) {
        super(plugin);
        this.registerMovecraftCompletions();
        this.registerMovecraftContexts();
        this.registerMovecraftConditions();
    }

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern PIPE = Pattern.compile("\\|");

    @Override //gonna keep this in case it is actually used
    public boolean hasPermission(CommandIssuer issuer, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        //handle AND like normal using comma ","
        String[] perms = COMMA.split(permission);
        if (perms.length > 1) {
            return super.hasPermission(issuer, Set.of(perms));
        }

        //handle OR using pipe "|"
        CommandSender sender = issuer.getIssuer();
        for (String perm : PIPE.split(permission)) {
            perm = perm.trim();
            if (!perm.isEmpty() && sender.hasPermission(perm)) {
                return true;
            }
        }

        return false;
    }

    private void registerMovecraftCompletions() {
        getCommandCompletions().registerCompletion("crafttypes", c ->  {
            Set<CraftType> craftTypes = CraftManager.getInstance().getCraftTypes();
            List<String> craftNames = craftTypes.stream().map(type -> type.getStringProperty(CraftType.NAME)).toList();
            return craftNames;
        });

        getCommandCompletions().registerCompletion("directions", c -> {
            var allDirections = CruiseDirection.valuesString();
            var allButNone = allDirections.stream().filter(p -> !p.equals("none")).toList();
            return allButNone;
        });
    }

    private void registerMovecraftContexts() {
        getCommandContexts().registerContext(CruiseDirection.class, (c) -> {
            String data = c.popFirstArg();
            return CruiseDirection.fromString(data);
        });

        getCommandContexts().registerContext(CraftType.class, (c) -> {
            String data = c.popFirstArg();
            CraftType type = CraftManager.getInstance().getCraftTypeFromString(data);

            if (type == null) {
                throw new InvalidCommandArgument("You must supply a craft type!");
            }

            return type;
        });
    }

    private void registerMovecraftConditions() {
        getCommandConditions().addCondition("pilot_others", (context -> {
            var issuer = context.getIssuer();
            if (!issuer.hasPermission("movecraft.commands.release.others")) {
                throw new ConditionFailedException(MOVECRAFT_COMMAND_PREFIX
                        + I18nSupport.getInternationalisedString("Release - No Force Release"));

            }
        }));
    }
}
