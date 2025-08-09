package com.maroontress.clione;

import java.util.List;
import java.util.Objects;

/**
    Represents a preprocessor macro.
*/
public final class Macro {

    private final String name;
    private final boolean isFunctionLike;
    private final List<String> parameters;
    private final List<Token> body;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param isFunctionLike {@code true} if the macro is function-like,
        {@code false} otherwise.
        @param parameters The list of parameter names. This is empty for
        object-like macros.
        @param body The list of tokens that form the macro's body.
    */
    public Macro(String name,
            boolean isFunctionLike,
            List<String> parameters,
            List<Token> body) {
        this.name = name;
        this.isFunctionLike = isFunctionLike;
        this.parameters = List.copyOf(parameters);
        this.body = List.copyOf(body);
    }

    /**
        Returns the name of the macro.

        @return The name of the macro.
    */
    public String name() {
        return name;
    }

    /**
        Returns whether the macro is function-like.

        @return {@code true} if the macro is function-like, {@code false}
        otherwise.
    */
    public boolean isFunctionLike() {
        return isFunctionLike;
    }

    /**
        Returns the unmodifiable list of parameter names for the macro.

        @return The unmodifiable list of parameter names.
    */
    public List<String> parameters() {
        return parameters;
    }

    /**
        Returns the unmodifiable list of tokens that form the macro's body.

        @return The unmodifiable list of tokens in the macro's body.
    */
    public List<Token> body() {
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var macro = (Macro) o;
        return isFunctionLike == macro.isFunctionLike
            && Objects.equals(name, macro.name)
            && Objects.equals(parameters, macro.parameters)
            && Objects.equals(body, macro.body);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(name, isFunctionLike, parameters, body);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Macro{"
            + "name='" + name + '\''
            + ", isFunctionLike=" + isFunctionLike
            + ", parameters=" + parameters
            + ", body=" + body
            + '}';
    }
}
