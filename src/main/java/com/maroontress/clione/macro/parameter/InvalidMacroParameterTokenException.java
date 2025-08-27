package com.maroontress.clione.macro.parameter;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.PreprocessException;

/**
    Thrown to indicate that an invalid token is in a macro parameter list.
*/
public final class InvalidMacroParameterTokenException
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public InvalidMacroParameterTokenException(final Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(final Token causeToken) {
        var start = causeToken.getSpan().getStart();
        return String.format(
                "%s: error: invalid token in macro parameter list", start);
    }
}
