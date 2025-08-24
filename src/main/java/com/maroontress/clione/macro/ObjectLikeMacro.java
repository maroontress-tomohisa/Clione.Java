package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;
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
        @param body The list of tokens that form the macro's body.
    */
    public ObjectLikeMacro(String name, List<Token> body) {
        this.name = name;
        this.body = List.copyOf(body);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> parameters() {
        return List.of();
    }

    @Override
    public List<Token> body() {
        return body;
    }

    @Override
    public Optional<Token> apply(Preprocessor preprocessor, Token token)
            throws PreprocessException {
        return preprocessor.expandObjectBasedMacro(this, token);
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            MacroArgument args, Preprocessor preprocessor)
            throws PreprocessException {
        return getDefaultSubstitutionMapping(args, preprocessor);
    }
}
