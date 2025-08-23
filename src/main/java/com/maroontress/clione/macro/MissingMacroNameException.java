package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a macro name is missing.
*/
public final class MissingMacroNameException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public MissingMacroNameException(final Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(final Token causeToken) {
        var end = causeToken.getSpan().getEnd();
        return String.format(
            "L%d:%d: error: macro name missing",
            end.getLine(), end.getColumn() + 1);
    }
}
