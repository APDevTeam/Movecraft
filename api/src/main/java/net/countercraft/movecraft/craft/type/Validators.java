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

    static void registerAll() {

    }

}
