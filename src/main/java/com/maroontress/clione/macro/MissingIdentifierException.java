package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a macro parameter identifier is missing.
*/
public final class MissingIdentifierException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param causeToken The token at which the error was detected.
    */
    public MissingIdentifierException(Token causeToken) {
        super("missing identifier in macro parameter list", causeToken);
    }
}
