package com.maroontress.clione.macro;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
        Returns the unmodifiable list of tokens that form the macro's body.

        @return The unmodifiable list of tokens in the macro's body.
    */
    List<Token> body();

    /**
        Applies the macro expansion.

        @param token The token that triggered the macro expansion.
        @return The next token to be processed, or an empty optional if no
            token is available.
        @throws IOException if an I/O error occurs.
    */
    Optional<Token> apply(Foo foo, Token token) throws IOException;
}
