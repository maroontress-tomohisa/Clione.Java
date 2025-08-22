package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    Defines the behavior of function-like macros.
*/
public interface MacroBehavior {
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
}
