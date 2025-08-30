package com.maroontress.clione.macro;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Defines the behavior of function-like macros.
*/
public interface FunctionLikeMacroBehavior {

    /**
        The behavior for regular function-like macros.
    */
    FunctionLikeMacroBehavior REGULAR = new RegularFunctionLikeBehavior();

    /**
        The behavior for variadic function-like macros.
    */
    FunctionLikeMacroBehavior VARIADIC = new VariadicFunctionLikeBehavior();

    /**
        Returns the substitution mapping for the given macro and arguments.

        @param macro The macro to be expanded.
        @param args The arguments of the macro invocation.
        @param keeper The macro keeper
        @return The substitution mapping.
        @throws PreprocessException If an error occurs during preprocessing.
    */
    Map<String, List<Token>> getSubstitutionMapping(
            FunctionLikeMacro macro, MacroArgument args,
            MacroKeeper keeper) throws PreprocessException;

    /**
        Creates a new macro argument builder.

        @param macro The macro for which the builder is created.
        @param openParen The opening parenthesis of the argument list.
        @return A new macro argument builder.
    */
    MacroArgumentBuilder createArgumentBuilder(
            FunctionLikeMacro macro, Token openParen);

    /**
        Returns the stringizing operand validator.

        @return The stringizing operand validator.
    */
    BiPredicate<Token, List<String>> getStringizingOperandValidator();

    /**
        Creates a new stringizing operand validator.

        @param parameters The list of macro parameters.
        @return A new stringizing operand validator.
    */
    default Predicate<Token> newStringizingOperandValidator(
            List<String> parameters) {
        var validator = getStringizingOperandValidator();
        return token -> {
            return token.isType(TokenType.IDENTIFIER)
                    && validator.test(token, parameters);
        };
    }

    /**
        Validates whether the {@code __VA_ARGS__} keyword is used correctly.

        @param body The macro body.
        @throws PreprocessException If the {@code __VA_ARGS__} keyword is used
            incorrectly.
    */
    default void validateVaArgKeyword(Collection<Token> body)
            throws PreprocessException {
        // Do nothing
    }
}
