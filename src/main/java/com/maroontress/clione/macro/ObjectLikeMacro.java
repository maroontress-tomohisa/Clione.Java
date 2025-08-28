package com.maroontress.clione.macro;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    Represents an object-like preprocessor macro.
*/
public final class ObjectLikeMacro implements Macro {

    private final String name;
    private final List<Token> body;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param body The collection of tokens that form the macro's body.
    */
    public ObjectLikeMacro(String name, Collection<Token> body) {
        this.name = name;
        this.body = List.copyOf(body);
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public List<Token> body() {
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Token> apply(Preprocessor preprocessor, Token token)
            throws PreprocessException {
        return preprocessor.expandObjectBasedMacro(this, token);
    }
}
