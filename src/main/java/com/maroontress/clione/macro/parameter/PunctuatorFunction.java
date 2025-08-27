package com.maroontress.clione.macro.parameter;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.PreprocessException;

/**
    A function that processes a punctuator.
*/
@FunctionalInterface
public interface PunctuatorFunction {

    /**
        Applies the function.

        @param token The token.
        @return The next state.
        @throws PreprocessException if an error occurs.
    */
    State apply(Token token) throws PreprocessException;
}
