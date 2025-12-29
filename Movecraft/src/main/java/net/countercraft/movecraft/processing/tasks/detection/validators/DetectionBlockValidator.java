package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.RequiredBlockEntry;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class DetectionBlockValidator extends AbstractBlockConstraintValidator {
    @Override
    protected Collection<RequiredBlockEntry> getRelevantConstraintSet(TypeSafeCraftType type) {
        return type.get(PropertyKeys.DETECTION_BLOCKS);
    }

    @Override
    protected String getFailMessage(RequiredBlockEntry.DetectionResult result, @NotNull String errorMessage, RequiredBlockEntry failedCondition) {
        String failMessage = "";
        switch (result) {
            case NOT_ENOUGH:
                failMessage += I18nSupport.getInternationalisedString("Detection - Not enough detectionblock");
                break;
            case TOO_MUCH:
                failMessage += I18nSupport.getInternationalisedString("Detection - Too much detectionblock");
                break;
            default:
                break;
        }
        failMessage += ": [" + failedCondition.materialsToString() + "] " + errorMessage;
        return failMessage;
    }

}
