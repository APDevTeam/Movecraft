package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.countercraft.movecraft.util.ChatUtils.ERROR_PREFIX;

public class RemoteSign extends AbstractCraftSign {
    private static final String HEADER = "Remote Sign";

    public RemoteSign() {
        super(null, false);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {
        // TODO: How to react?
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper sign) {
        player.sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign - Must be a part of a piloted craft"));
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, SignListener.SignWrapper sign, Craft craft, Player player) {
        Map<AbstractMovecraftSign, LinkedList<SignListener.SignWrapper>> foundTargetSigns = new HashMap<>();
        boolean firstError = true;
        final String targetIdent = sign.getRaw(1);
        for (MovecraftLocation tloc : craft.getHitBox()) {
            BlockState tstate = craft.getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ()).getState();
            if (!Tag.ALL_SIGNS.isTagged(tstate.getType())) {
                continue;
            }
            if (!(tstate instanceof Sign)) {
                continue;
            }
            Sign ts = (Sign) tstate;

            SignListener.SignWrapper[] targetSignWrappers = SignListener.INSTANCE.getSignWrappers(ts);

            if (targetSignWrappers != null) {
                for (SignListener.SignWrapper wrapper : targetSignWrappers) {
                    // Matches source?
                    final String signHeader = PlainTextComponentSerializer.plainText().serialize(wrapper.line(0));
                    AbstractMovecraftSign signHandler = AbstractMovecraftSign.get(signHeader);
                    // Ignore other remove signs
                    if (signHandler == null || signHandler instanceof RemoteSign) {
                        continue;
                    }
                    // But does it match the source man?
                    if (matchesDescriptor(targetIdent, wrapper)) {
                        // Forbidden strings
                        if (hasForbiddenString(wrapper)) {
                            if (firstError) {
                                player.sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Forbidden string found"));
                                firstError = false;
                            }
                            player.sendMessage(" - ".concat(tloc.toString()).concat(" : ").concat(ts.getLine(0)));
                        } else {
                            LinkedList<SignListener.SignWrapper> value = foundTargetSigns.computeIfAbsent(signHandler, (a) -> new LinkedList<>());
                            value.add(wrapper);
                        }
                    }
                }
            }
        }
        if (!firstError) {
            return false;
        }
        else if (foundTargetSigns.isEmpty()) {
            player.sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Could not find target sign"));
            return false;
        }

        if (Settings.MaxRemoteSigns > -1) {
            int foundLocCount = foundTargetSigns.size();
            if(foundLocCount > Settings.MaxRemoteSigns) {
                player.sendMessage(String.format(I18nSupport.getInternationalisedString("Remote Sign - Exceeding maximum allowed"), foundLocCount, Settings.MaxRemoteSigns));
                return false;
            }
        }

        // call the handlers!
        foundTargetSigns.entrySet().forEach(entry -> {
            AbstractMovecraftSign signHandler = entry.getKey();
            for (SignListener.SignWrapper wrapper : entry.getValue()) {
                signHandler.processSignClick(clickType, wrapper, player);
            }
        });

        return true;
    }

    @Override
    protected boolean isSignValid(Action clickType, SignListener.SignWrapper sign, Player player) {
        String target = sign.getRaw(1);
        if (target.isBlank()) {
            player.sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Sign - Cannot be blank"));
            return false;
        }

        if (hasForbiddenString(sign)) {
            player.sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Forbidden string found"));
            return false;
        }

        return true;
    }

    protected static boolean hasForbiddenString(SignListener.SignWrapper wrapper) {
        for (int i = 0; i < wrapper.lines().size(); i++) {
            String s = wrapper.getRaw(i).toLowerCase();
            if(Settings.ForbiddenRemoteSigns.contains(s))
                return true;
        }
        return false;
    }

    // Walks through all strings on the wrapper and if any of the non-header strings match it returns true
    protected static boolean matchesDescriptor(final String descriptor, final SignListener.SignWrapper potentialTarget) {
        for (int i = 1; i < potentialTarget.lines().size(); i++) {
            String targetStr = potentialTarget.getRaw(i);
            if (descriptor.equalsIgnoreCase(targetStr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, SignListener.SignWrapper sign) {
        return isSignValid(Action.PHYSICAL, sign, event.getPlayer());
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (!craft.getType().getBoolProperty(CraftType.ALLOW_REMOTE_SIGN)) {
            player.sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Sign - Not allowed on this craft"));
            return false;
        }

        if (super.canPlayerUseSignOn(player, craft)) {
            return true;
        }

        return craft.getHitBox().inBounds(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
    }
}
