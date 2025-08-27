package com.maroontress.clione.macro.parameter;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.PreprocessException;

/**
    Thrown to indicate that a macro parameter identifier is missing.
*/
public final class MissingMacroParameterException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public MissingMacroParameterException(Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(Token causeToken) {
        return String.format(
            "%s: error: missing identifier in macro parameter list",
            causeToken.getSpan().getStart());
    }
}
