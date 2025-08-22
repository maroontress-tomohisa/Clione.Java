package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
 * Thrown when the '#' operator is not followed by a macro parameter.
 */
public final class InvalidStringizingOperatorException
        extends PreprocessException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance of this class.
     *
     * @param causeToken The token that caused the exception.
     */
    public InvalidStringizingOperatorException(final Token causeToken) {
        super(causeToken.getSpan().getStart()
            + ": error: '#' is not followed by a macro parameter",
            causeToken);
    }
}
