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

        @param kit The parse kit.
        @param token The token that triggered the macro expansion.
        @return The next token to be processed, or an empty optional if no
            token is available.
        @throws IOException if an I/O error occurs.
    */
    Optional<Token> apply(ParseKit kit, Token token) throws IOException;

    /**
        Accepts the pasting visitor.

        @param visitor The visitor.
    */
    void paste(PastingVisitor visitor);

    /**
        Defines the visitor for the token pasting (##) operator.
    */
    interface PastingVisitor {

        /**
            Visits the object-like macro.

            @param macro The macro.
        */
        void paste(ObjectLikeMacro macro);

        /**
            Visits the function-like macro.

            @param macro The macro.
        */
        void paste(FunctionLikeMacro macro);
    }
}
