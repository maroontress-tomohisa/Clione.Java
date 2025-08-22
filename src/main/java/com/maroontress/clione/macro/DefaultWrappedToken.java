package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

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
