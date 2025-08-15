package com.maroontress.clione;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
    Represents an object-like preprocessor macro.
*/
public final class SimpleMacro implements Macro {

    private final String name;
    private final List<Token> body;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param body The list of tokens that form the macro's body.
    */
    public SimpleMacro(String name, List<Token> body) {
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
    public boolean apply(Preprocessor preprocessor,
                         Token token) throws IOException {
        preprocessor.getExpandingMacros()
            .put(name(), token);
        preprocessor.getTokenQueue()
            .addFirst(new Preprocessor.MacroEndMarker(name()));
        preprocessor.prependTokens(body());
        return true;
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            List<List<Token>> args, Preprocessor preprocessor)
            throws PreprocessException {
        return getDefaultSubstitutionMapping(args, preprocessor);
    }

    @Override
    public List<List<Token>> parseArguments(Preprocessor unused) {
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (SimpleMacro) o;
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
