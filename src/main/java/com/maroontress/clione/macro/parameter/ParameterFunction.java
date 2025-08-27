package com.maroontress.clione.macro.parameter;

import java.util.function.Consumer;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.PreprocessException;

/**
    A function that processes a token.
*/
@FunctionalInterface
public interface ParameterFunction {

    /**
        Applies the function.

        @param token The token.
        @param addParameter The consumer to add a parameter.
        @return The next state.
        @throws PreprocessException if an error occurs.
    */
    State apply(Token token, Consumer<String> addParameter)
            throws PreprocessException;
}
