package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that '__VA_ARGS__' appears in a location other than
    the replacement list of a variadic macro.
*/
public final class VaArgsKeywordMisusageException
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param causeToken The token that caused the error.
    */
    public VaArgsKeywordMisusageException(Token causeToken) {
        super(String.format(
                "%s: error: __VA_ARGS__ can only appear in the expansion of "
                        + "a C99 variadic macro",
                causeToken.getSpan().getStart()), causeToken);
    }
}
