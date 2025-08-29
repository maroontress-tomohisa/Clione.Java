package com.maroontress.clione.macro;

import java.util.Optional;
import java.util.function.Supplier;

import com.maroontress.clione.Token;

/**
    Represents a token in the preprocessor queue.
*/
public interface MacroToken {

    /**
        Returns the token if this instance represents a token.

        @return An optional containing the token, or an empty optional.
    */
    Optional<Token> getToken();

    /**
        Returns the name of the macro that has just finished expanding.

        @return An optional containing the macro name, or an empty optional.
    */
    Optional<String> getMacroEndName();

    /**
        Expands this token using the specified visitor.

        @param visitor The visitor to use for expansion.
        @throws PreprocessException If an error occurs during expansion.
    */
    void expand(MacroExpansionVisitor visitor) throws PreprocessException;

    /**
        Adds this token to the given macro argument builder.

        @param builder The macro argument builder to which this token is added.
        @param exceptionSupplier A supplier for creating an exception if an
        error occurs.
        @return {@code true} if this token completes the macro argument list,
        {@code false} otherwise.
        @throws PreprocessException If an error occurs while adding the token.
    */
    boolean addToArguments(MacroArgumentBuilder builder,
            Supplier<PreprocessException> exceptionSupplier)
            throws PreprocessException;
}
