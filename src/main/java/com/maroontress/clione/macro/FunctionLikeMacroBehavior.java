package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Defines the behavior of function-like macros.
*/
public interface FunctionLikeMacroBehavior {

    public static final FunctionLikeMacroBehavior DEFAULT
            = new RegularFunctionLikeBehavior();

    public static final FunctionLikeMacroBehavior VARIADIC
            = new VariadicFunctionLikeBehavior();

    /**
        Returns the substitution mapping for the given macro and arguments.

        @param macro The macro to be expanded.
        @param args The arguments of the macro invocation.
        @param preprocessor The preprocessor.
        @return The substitution mapping.
        @throws PreprocessException If an error occurs during preprocessing.
    */
    Map<String, List<Token>> getSubstitutionMapping(
            FunctionLikeMacro macro, MacroArgument args,
            Preprocessor preprocessor) throws PreprocessException;

    /**
        Creates a new macro argument builder.

        @param macro The macro for which the builder is created.
        @param openParen The opening parenthesis of the argument list.
        @return A new macro argument builder.
    */
    MacroArgumentBuilder createArgumentBuilder(
            FunctionLikeMacro macro, Token openParen);

    BiPredicate<Token, List<String>> getStringizingOperandValidator();

    default Predicate<Token> newStringizingOperandValidator(
            List<String> parameters) {
        var validator = getStringizingOperandValidator();
        return token -> {
            return token.getType() == TokenType.IDENTIFIER
                    && validator.test(token, parameters);
        };
    }

    default void validateVaArgKeyword(List<Token> body)
            throws PreprocessException {
        // Do nothing
    }
}
