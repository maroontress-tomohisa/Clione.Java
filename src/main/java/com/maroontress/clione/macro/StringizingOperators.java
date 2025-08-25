package com.maroontress.clione.macro;

import java.util.List;
import java.util.function.Predicate;

import com.maroontress.clione.Token;

public final class StringizingOperators {

    public static boolean defaultOperandValidator(
            Token token, List<String> parameters) {
        return newOperandValidator(
                token, parameters, ignored -> false);
    }

    public static boolean variadicOperandValidator(
            Token token, List<String> parameters) {
        return newOperandValidator(
                token, parameters, s -> s.equals(MacroKeywords.VA_ARGS));
    }

    private static boolean newOperandValidator(
            Token token, List<String> parameters, Predicate<String> extra) {
        var value = token.getValue();
        return parameters.contains(value) || extra.test(value);
    }
}
