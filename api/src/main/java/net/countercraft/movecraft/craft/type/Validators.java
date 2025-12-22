package net.countercraft.movecraft.craft.type;

import net.countercraft.movecraft.util.Pair;

import java.util.function.Predicate;

public class Validators {

    public static Pair<Predicate<TypeSafeCraftType>, String> register(Pair<Predicate<TypeSafeCraftType>, String> validator) {
        if (!TypeSafeCraftType.VALIDATOR_REGISTRY.add(validator)) {
            throw new IllegalStateException("No duplicate validators allowed!");
        }
        return validator;
    }

    static void register(Predicate<TypeSafeCraftType> predicate, String errorMessage) {
        register(new Pair<>(predicate, errorMessage));
    }

    static void registerAll() {
        // Validator to avoid parent recursions!
        register(
                type -> {
                    TypeSafeCraftType tmp = type;
                    if (tmp.getParent() != null) {
                        do {
                            tmp = tmp.getParent();
                            if (tmp == type) {
                                return false;
                            }
                        } while(tmp != null);
                    }
                    return true;
                },
                "Type must not be used as parent in its own parent hierarchy!"
        );
    }

}
