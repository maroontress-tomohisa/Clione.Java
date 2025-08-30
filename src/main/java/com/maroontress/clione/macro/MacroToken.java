package com.maroontress.clione.macro;

import java.util.function.Supplier;

/**
    Represents a token in the preprocessor queue.
*/
public interface MacroToken {

    /**
        Applies the specified visitor.

        @param <T> The type of a value to be returned
        @param visitor The visitor
        @return The result of visiting
    */
    <T> T apply(MacroTokenVisitor<T> visitor);

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
