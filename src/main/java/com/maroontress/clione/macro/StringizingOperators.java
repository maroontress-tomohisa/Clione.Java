package com.maroontress.clione.macro;

import java.util.List;
import java.util.function.Predicate;

import com.maroontress.clione.Token;

/**
    Provides static methods for stringizing operator.
*/
public final class StringizingOperators {

    private StringizingOperators() {
    }

    /**
        Returns the result of validation of the token.

        @param token The token.
        @param parameters The macro parameters.
        @return The result of validation of the token.
    */
    public static boolean defaultOperandValidator(
            Token token, List<String> parameters) {
        return newOperandValidator(
                token, parameters, ignored -> false);
    }

    /**
        Returns the result of validation of the token.

        @param token The token.
        @param parameters The macro parameters.
        @return The result of validation of the token.
    */
    public static boolean variadicOperandValidator(
            Token token, List<String> parameters) {
        return newOperandValidator(
                token, parameters, s -> s.equals(VaArgs.KEYWORD));
    }

    private static boolean newOperandValidator(
            Token token, List<String> parameters, Predicate<String> extra) {
        var value = token.getValue();
        return parameters.contains(value) || extra.test(value);
    }
}
