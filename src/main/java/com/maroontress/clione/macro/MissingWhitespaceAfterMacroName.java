package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

public final class MissingWhitespaceAfterMacroName
        extends PreprocessException {

    private static final long serialVersionUID = 1L;

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
