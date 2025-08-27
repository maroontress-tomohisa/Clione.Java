package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that the '##' operator appears at the beginning or end
    of a macro replacement list.
*/
public final class InvalidConcatenationOperatorException
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param causeToken The token that caused the error.
        @param isStart {@code true} if the operator is at the start of the
        macro expansion, {@code false} otherwise.
    */
    public InvalidConcatenationOperatorException(
            Token causeToken, boolean isStart) {
        super(
            String.format(
                "%s: error: '##' cannot appear at %s of macro expansion",
                causeToken.getSpan().getStart(),
                isStart ? "start" : "end"),
            causeToken);
    }
}
