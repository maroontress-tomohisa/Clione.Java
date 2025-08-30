package com.maroontress.clione.macro;

import com.maroontress.clione.Token;
import java.util.Optional;

/**
    The facade of the macro expansion engine.
*/
public interface Foo {

    /**
        Returns the macro keeper.

        @return The macro keeper
    */
    MacroKeeper getKeeper();

    /**
        Returns the token reservoir.

        @return The token reservoir
    */
    TokenReservoir getReservoir();

    /**
        Expands the specified macro.

        @param macro The macro
        @param token The token
        @param supplier The supplier of the macro body
        @return The first token of the expansion result
        @throws PreprocessException if it fails to preprocess
    */
    Optional<Token> expand(Macro macro, Token token, BodySupplier supplier)
            throws PreprocessException;

    /**
        Concatenates the specified tokens.

        @param left The left-hand-side token
        @param right The right-hand-side token
        @return The concatenated token
        @throws PreprocessException if it fails to preprocess
    */
    Token concatenate(Token left, Token right) throws PreprocessException;
}
