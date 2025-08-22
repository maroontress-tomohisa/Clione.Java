package com.maroontress.clione.macro;

import java.util.Optional;

import com.maroontress.clione.Token;

/**
    Wraps a {@link Token} that originates from a macro parameter.
*/
public final class ParameterOriginatedToken implements MacroToken {

    private final Token token;

    /**
        Constructs a new instance.

        @param token The token to be wrapped.
    */
    public ParameterOriginatedToken(final Token token) {
        this.token = token;
    }

    @Override
    public Optional<Token> getToken() {
        return Optional.of(token);
    }

    @Override
    public Optional<String> getMacroEndName() {
        return Optional.empty();
    }

    @Override
    public boolean isOriginatingFromParameter() {
        return true;
    }
}
