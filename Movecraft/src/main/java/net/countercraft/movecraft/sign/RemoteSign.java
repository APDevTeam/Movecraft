package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

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
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {
        player.sendMessage(ERROR_PREFIX+I18nSupport.getInternationalisedString("Remote Sign - Must be a part of a piloted craft"));
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action clickType, AbstractSignListener.SignWrapper sign, Craft craft, Player player) {
        LinkedList<MovecraftLocation> foundLocations = new LinkedList<MovecraftLocation>();
        Map<AbstractMovecraftSign, LinkedList<AbstractSignListener.SignWrapper>> foundTargetSigns = new Hashtable<>();
        boolean firstError = true;
        final String targetIdent = sign.getRaw(1).toUpperCase();
        for (MovecraftLocation tloc : craft.getHitBox()) {
            BlockState tstate = craft.getWorld().getBlockAt(tloc.getX(), tloc.getY(), tloc.getZ()).getState();
            if (!(tstate instanceof Sign)) {
                continue;
            }
            Sign ts = (Sign) tstate;

            AbstractSignListener.SignWrapper[] targetSignWrappers = Movecraft.getInstance().getAbstractSignListener().getSignWrappers(ts);

            if (targetSignWrappers != null) {
                for (AbstractSignListener.SignWrapper wrapper : targetSignWrappers) {
                    // Matches source?
                    final String signHeader = PlainTextComponentSerializer.plainText().serialize(wrapper.line(0));
                    Optional<AbstractMovecraftSign> signHandler = AbstractMovecraftSign.tryGet(signHeader);
                    // Ignore other remove signs
                    if (!signHandler.isPresent() || signHandler.get() instanceof RemoteSign) {
                        continue;
                    }
                    // Forbidden strings
                    if (hasForbiddenString(wrapper)) {
                        if (firstError) {
                            player.sendMessage(I18nSupport.getInternationalisedString("Remote Sign - Forbidden string found"));
                            firstError = false;
                        }
                        player.sendMessage(" - ".concat(tloc.toString()).concat(" : ").concat(ts.getLine(0)));
                    }
                    // But does it match the source man?
                    if (matchesDescriptor(targetIdent, wrapper)) {
                        LinkedList<AbstractSignListener.SignWrapper> value = foundTargetSigns.getOrDefault(signHandler.get(), new LinkedList<>());
                        value.add(wrapper);
                        foundLocations.add(tloc);
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
            for (AbstractSignListener.SignWrapper wrapper : entry.getValue()) {
                signHandler.processSignClick(clickType, wrapper, player);
            }
        });

        return true;
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        return processingSuccessful || !sneaking;
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
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

    protected static boolean hasForbiddenString(AbstractSignListener.SignWrapper wrapper) {
        for (int i = 0; i < wrapper.lines().size(); i++) {
            String s = wrapper.getRaw(i).toLowerCase();
            if(Settings.ForbiddenRemoteSigns.contains(s))
                return true;
        }
        return false;
    }

    // Walks through all strings on the wrapper and if any of the non-header strings match it returns true
    protected static boolean matchesDescriptor(final String descriptor, final AbstractSignListener.SignWrapper potentialTarget) {
        for (int i = 1; i < potentialTarget.lines().size(); i++) {
            String targetStr = potentialTarget.getRaw(i).toUpperCase();
            if (descriptor.equalsIgnoreCase(targetStr)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        return isSignValid(Action.PHYSICAL, sign, event.getPlayer());
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (!craft.getType().getBoolProperty(CraftType.ALLOW_REMOTE_SIGN)) {
            player.sendMessage(ERROR_PREFIX + I18nSupport.getInternationalisedString("Remote Sign - Not allowed on this craft"));
            return false;
        }

        return super.canPlayerUseSignOn(player, craft);
    }
}
