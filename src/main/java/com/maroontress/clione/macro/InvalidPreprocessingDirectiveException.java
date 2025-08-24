package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

public final class InvalidPreprocessingDirectiveException extends
        PreprocessException {

    private static final long serialVersionUID = 1L;

    public InvalidPreprocessingDirectiveException(Token causeToken) {
        super(newMessage(causeToken), causeToken);
    }

    private static String newMessage(Token causeToken) {
        return String.format("%s: error: '%s' is invalid preprocessing directive",
            causeToken.getSpan().getStart(), causeToken.getValue());
    }
}
