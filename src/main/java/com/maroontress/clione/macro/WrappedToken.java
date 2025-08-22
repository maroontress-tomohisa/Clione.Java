package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

public interface WrappedToken extends MacroToken {

    Token unwrap();

    static WrappedToken of(Token token) {
        return new DefaultWrappedToken(token);
    }
}
