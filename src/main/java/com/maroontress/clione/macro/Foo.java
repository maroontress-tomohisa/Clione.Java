package com.maroontress.clione.macro;

import java.util.Optional;

import com.maroontress.clione.Token;

public interface Foo {

    MacroKeeper getKeeper();

    TokenReservoir getReservoir();

    Optional<Token> expand(Macro macro, Token token, BodySupplier supplier)
        throws PreprocessException;

    Token concatenate(Token left, Token right) throws PreprocessException;
}
