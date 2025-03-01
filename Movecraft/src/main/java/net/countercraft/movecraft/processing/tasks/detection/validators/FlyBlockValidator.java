package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FlyBlockValidator extends AbstractBlockConstraintValidator {
    @Override
    protected Collection<RequiredBlockEntry> getRelevantConstraintSet(CraftType type) {
        return type.getRequiredBlockProperty(CraftType.FLY_BLOCKS);
    }

    @Override
    protected String getFailMessage(RequiredBlockEntry.DetectionResult result, @NotNull String errorMessage, RequiredBlockEntry failedCondition) {
        String failMessage = "";
        switch (result) {
            case NOT_ENOUGH:
                failMessage += I18nSupport.getInternationalisedString("Detection - Not enough flyblock");
                break;
            case TOO_MUCH:
                failMessage += I18nSupport.getInternationalisedString("Detection - Too much flyblock");
                break;
            default:
                break;
        }
        failMessage += ": [" + failedCondition.materialsToString() + "] " + errorMessage;
        return failMessage;
    }

}
