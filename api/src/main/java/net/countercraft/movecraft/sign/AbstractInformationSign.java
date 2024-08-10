package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractInformationSign extends AbstractCraftSign {

    public AbstractInformationSign() {
        // Info signs only display things, that should not require permissions, also it doesn't matter if the craft is busy or not
        super(null, true);
    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, Craft craft) {
        // Permcheck related, no perms required, return true
        return true;
    }

    @Override
    public void onCraftDetect(CraftDetectEvent event, AbstractSignListener.SignWrapper sign) {
        // TODO: Check if the craft supports this sign? If no, cancel
        super.onCraftDetect(event, sign);
        this.refreshSign(event.getCraft(), sign, true);
    }

    @Override
    public void onSignMovedByCraft(SignTranslateEvent event) {
        super.onSignMovedByCraft(event);
        final Craft craft = event.getCraft();
        for (MovecraftLocation movecraftLocation : event.getLocations()) {
            Block block = movecraftLocation.toBukkit(craft.getWorld()).getBlock();
            if (block instanceof Sign sign) {
                for (AbstractSignListener.SignWrapper wrapper : AbstractSignListener.INSTANCE.getSignWrappers(sign, event)) {
                    this.refreshSign(event.getCraft(), wrapper, false);
                }
            }
        }
    }

    @Override
    protected void onCraftNotFound(Player player, AbstractSignListener.SignWrapper sign) {
        // Nothing to do
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        return true;
    }

    @Override
    public boolean shouldCancelEvent(boolean processingSuccessful, @Nullable Action type, boolean sneaking) {
        if (processingSuccessful) {
            return true;
        }
        return !sneaking;
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, Craft craft) {
        this.refreshSign(craft, sign, false);
        return true;
    }

    protected void refreshSign(@Nullable Craft craft, AbstractSignListener.SignWrapper sign, boolean fillDefault) {
        boolean changedSome = false;
        Component[] updatePayload = new Component[sign.lines().size()];
        for(int i = 1; i < sign.lines().size(); i++) {
            Component oldComponent = sign.line(i);
            Component potentiallyNew;
            if (craft == null || fillDefault) {
                potentiallyNew = this.getDefaultString(i, oldComponent);
            } else {
                 potentiallyNew = this.getUpdateString(i, oldComponent, craft);
            }
            if (potentiallyNew != null && !potentiallyNew.equals(oldComponent)) {
                String oldValue = PlainTextComponentSerializer.plainText().serialize(oldComponent);
                String newValue = PlainTextComponentSerializer.plainText().serialize(potentiallyNew);
                if (!oldValue.equals(newValue)) {
                    changedSome = true;
                    updatePayload[i] = potentiallyNew;
                }
            }
        }
        if (changedSome) {
            this.performUpdate(updatePayload, sign);
            this.sendUpdatePacket(craft, sign);
            sign.block().update(true);
        }
    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        this.refreshSign(null, sign, true);
        return true;
    }

    /*
        Data to set on the sign. Return null if no update should happen!
        Attention: A update will only be performed, if the new and old component are different!
         */
    @Nullable
    protected abstract Component getUpdateString(int lineIndex, Component oldData, Craft craft);
    @Nullable
    protected abstract Component getDefaultString(int lineIndex, Component oldComponent);

    /*
     * @param newComponents: Array of nullable values. The index represents the index on the sign. Only contains the updated components
     *
     * Only gets called if at least one line has changed
     */
    protected abstract void performUpdate(Component[] newComponents, AbstractSignListener.SignWrapper sign);

    /*
    Gets called after performUpdate has been called
     */
    protected abstract void sendUpdatePacket(Craft craft, AbstractSignListener.SignWrapper sign);
}
