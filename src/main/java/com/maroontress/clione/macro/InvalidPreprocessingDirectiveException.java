package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Represents the exception that the preprocessing directive is invalid.
*/
public final class InvalidPreprocessingDirectiveException extends
        PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Initializes the instance.

        @param causeToken
            The token that is the cause of this exception.
    */
    public InvalidPreprocessingDirectiveException(Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(Token causeToken) {
        return String.format("%s: error: '%s' is an invalid preprocessing directive",
            causeToken.getSpan().getStart(), causeToken.getValue());
    }
}
