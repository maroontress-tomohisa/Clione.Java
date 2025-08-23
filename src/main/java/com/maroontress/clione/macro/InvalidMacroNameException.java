package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a macro name is not an identifier.
*/
public final class InvalidMacroNameException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public InvalidMacroNameException(final Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(final Token causeToken) {
        var start = causeToken.getSpan().getStart();
        return String.format(
            "L%d:%d: error: macro name must be an identifier",
            start.getLine(), start.getColumn());
    }
}
