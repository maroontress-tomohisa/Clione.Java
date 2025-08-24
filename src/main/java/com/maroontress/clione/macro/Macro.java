package com.maroontress.clione.macro;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    Represents a preprocessor macro.
*/
public interface Macro {

    /**
        Returns the name of the macro.

        @return The name of the macro.
    */
    String name();

    /**
        Returns the unmodifiable list of parameter names for the macro.

        @return The unmodifiable list of parameter names.
    */
    List<String> parameters();

    /**
        Returns the unmodifiable list of tokens that form the macro's body.

        @return The unmodifiable list of tokens in the macro's body.
    */
    List<Token> body();

    /**
        Applies the macro expansion.

        @param preprocessor The preprocessor instance.
        @param token The token that triggered the macro expansion.
        @return The next token to be processed, or an empty optional if no
        token is available.
        @throws IOException if an I/O error occurs.
    */
    Optional<Token> apply(Preprocessor preprocessor, Token token)
            throws IOException;

    /**
        Returns the substitution mapping from the given macro arguments.

        @param args The list of macro arguments.
        @param preprocessor The preprocessor instance.
        @return The substitution mapping.
        @throws PreprocessException if an error occurs during preprocessing.
    */
    Map<String, List<Token>> getSubstitutionMapping(MacroArgument args,
            Preprocessor preprocessor) throws PreprocessException;

    /**
        Returns the default substitution mapping from the given macro
        arguments.

        @param args The list of macro arguments.
        @param preprocessor The preprocessor instance.
        @return The substitution mapping.
        @throws MacroArgumentException if the number of arguments is incorrect.
    */
    default Map<String, List<Token>> getDefaultSubstitutionMapping(
            MacroArgument args, Preprocessor preprocessor)
            throws MacroArgumentException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize != actualSize) {
            var causeToken = expectedSize > actualSize
                    ? args.getCloseParen()
                    : args.getComma(expectedSize - 1);
            var expandingTokens = List.copyOf(
                    preprocessor.getExpandingMacros().values());
            throw new MacroArgumentException(
                    causeToken, expectedSize, actualSize, expandingTokens);
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        return mapping;
    }
}
