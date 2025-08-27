package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Wraps a {@link Token} that originates from a macro parameter.
*/
public final class ParameterOriginatedToken extends AbstractWrappedToken {

    /**
        Constructs a new instance.

        @param token The token to be wrapped.
    */
    public ParameterOriginatedToken(Token token) {
        super(token);
    }

    @Override
    public boolean isOriginatingFromParameter() {
        return true;
    }
}
