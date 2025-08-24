package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Represents the exception that there is no whitespace after the macro name.
*/
public final class MissingWhitespaceAfterMacroName
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /**
        Initializes the instance.

        @param causeToken
            The token that is the cause of this exception.
    */
    public MissingWhitespaceAfterMacroName(Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(Token causeToken) {
        var start = causeToken.getSpan().getStart();
        return String.format(
            "%s: error: ISO C99 requires whitespace after the macro name",
            start);
    }
}
