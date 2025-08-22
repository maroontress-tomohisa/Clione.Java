package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Represents a default implementation of a wrapped token.

    <p>This class is used for tokens that originate directly from the source
    code and are not the result of a macro parameter expansion. It extends the
    {@link AbstractWrappedToken} and implements the
    {@link #isOriginatingFromParameter()} method to always return
    {@code false}.</p>
*/
public final class DefaultWrappedToken extends AbstractWrappedToken {

    /**
        Constructs a new instance.

        @param token The token to be wrapped.
    */
    public DefaultWrappedToken(Token token) {
        super(token);
    }

    @Override
    public boolean isOriginatingFromParameter() {
        return false;
    }
}
