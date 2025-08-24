package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Represents a token that is wrapped to provide additional information for
    macro expansion.

    <p>This interface is used to distinguish tokens that originate from the
    original source code from those that are generated during macro
    pre-expansion. It extends the {@link MacroToken} interface and adds a
    method to retrieve the original, unwrapped token.</p>
*/
public interface WrappedToken extends MacroToken {

    /**
        Returns the original, unwrapped token.

        @return The original token.
    */
    Token unwrap();

    /**
        Returns whether the token originates from a macro parameter.

        @return {@code true} if the token originates from a macro parameter,
            {@code false} otherwise.
    */
    boolean isOriginatingFromParameter();

    /**
        Creates a new {@code WrappedToken} instance from a given token.

        @param token The token to wrap.
        @return A new {@code WrappedToken} instance.
    */
    static WrappedToken of(Token token) {
        return new DefaultWrappedToken(token);
    }
}
