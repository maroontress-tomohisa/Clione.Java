package com.maroontress.clione.macro;

import java.util.Optional;

import com.maroontress.clione.Token;

public abstract class AbstractWrappedToken implements WrappedToken {

    private final Token token;

    /**
        Constructs a new instance.

        @param token The token to be wrapped.
    */
    public AbstractWrappedToken(Token token) {
        this.token = token;
    }

    @Override
    public final Optional<Token> getToken() {
        return Optional.of(token);
    }

    @Override
    public final Optional<String> getMacroEndName() {
        return Optional.empty();
    }

    @Override
    public abstract boolean isOriginatingFromParameter();

    @Override
    public final Token unwrap() {
        return token;
    }
}
