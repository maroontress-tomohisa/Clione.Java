package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a parenthesis is missing in a function-like macro
    definition.
*/
public final class MissingParenException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public MissingParenException(Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(Token causeToken) {
        return String.format(
            "%s: error: missing ')' in macro parameter list",
            causeToken.getSpan().getStart());
    }
}
