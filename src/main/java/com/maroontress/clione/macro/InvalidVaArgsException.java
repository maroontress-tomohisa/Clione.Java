package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that the '__VA_ARGS__' appears in the replacement list
    of an object-like macro or a function-like macro that is not variadic.
*/
public final class InvalidVaArgsException
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param causeToken The token that caused the error.
    */
    public InvalidVaArgsException(Token causeToken) {
        super(String.format(
                "%s: error: __VA_ARGS__ can only appear in the expansion of "
                        + "a C99 variadic macro",
                causeToken.getSpan().getStart()), causeToken);
    }
}
