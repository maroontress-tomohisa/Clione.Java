package com.maroontress.clione.macro;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            throws IOException {
        preprocessor.getExpandingMacros()
            .put(name(), token);
        preprocessor.getTokenQueue()
            .addFirst(new Preprocessor.MacroEndMarker(name()));
        preprocessor.prependTokens(body());
        return Optional.empty();
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            List<List<Token>> args, Preprocessor preprocessor)
            throws PreprocessException {
        return getDefaultSubstitutionMapping(args, preprocessor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ObjectLikeMacro) o;
        return Objects.equals(name, that.name)
            && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, body);
    }

    @Override
    public String toString() {
        return "SimpleMacro{"
            + "name='" + name + '\''
            + ", body=" + body
            + '}';
    }
}
