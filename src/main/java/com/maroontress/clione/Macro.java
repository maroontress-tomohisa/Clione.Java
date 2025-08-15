package com.maroontress.clione;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        @return {@code true} if the macro was expanded, {@code false} otherwise.
        @throws IOException if an I/O error occurs.
    */
    boolean apply(Preprocessor preprocessor, Token token) throws IOException;

    /**
        Returns the substitution mapping from the given macro arguments.

        @param args The list of macro arguments.
        @param preprocessor The preprocessor instance.
        @return The substitution mapping.
        @throws PreprocessException if an error occurs during preprocessing.
    */
    Map<String, List<Token>> getSubstitutionMapping(List<List<Token>> args,
            Preprocessor preprocessor) throws PreprocessException;

    /**
        Parses the macro arguments from the given preprocessor.

        @param preprocessor The preprocessor instance.
        @return The list of macro arguments.
        @throws IOException if an I/O error occurs.
    */
    List<List<Token>> parseArguments(Preprocessor preprocessor)
            throws IOException;

    /**
        Returns the default substitution mapping from the given macro arguments.

        @param args The list of macro arguments.
        @param preprocessor The preprocessor instance.
        @return The substitution mapping.
        @throws MacroArgumentException if the number of arguments is incorrect.
    */
    default Map<String, List<Token>> getDefaultSubstitutionMapping(
            List<List<Token>> args, Preprocessor preprocessor)
            throws MacroArgumentException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize != actualSize) {
            throw new MacroArgumentException(name(), expectedSize, actualSize,
                    List.copyOf(preprocessor.getExpandingMacros().values()));
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        return mapping;
    }
}
