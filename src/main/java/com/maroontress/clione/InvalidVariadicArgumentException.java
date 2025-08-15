package com.maroontress.clione;

import java.util.ArrayList;
import java.util.List;

/**
    Thrown to indicate that `__VA_ARGS__` is used with zero arguments and is
    preceded by a comma.
*/
public final class InvalidVariadicArgumentException extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param message The detail message.
        @param preprocessor The preprocessor instance.
        @param tokenToPrepend The token to prepend to the expansion stack.
    */
    public InvalidVariadicArgumentException(final String message,
            final Preprocessor preprocessor,
            final Token tokenToPrepend) {
        this(message, buildExpandingTokens(preprocessor, tokenToPrepend));
    }

    private InvalidVariadicArgumentException(final String message,
            final List<Token> expandingTokens) {
        super(buildMessage(message, expandingTokens), expandingTokens);
    }

    private static List<Token> buildExpandingTokens(
            final Preprocessor preprocessor,
            final Token tokenToPrepend) {
        var expandingTokens =
            new ArrayList<>(preprocessor.getExpandingMacros().values());
        expandingTokens.add(0, tokenToPrepend);
        return expandingTokens;
    }

    private static String buildMessage(final String message, final List<Token> expandingTokens) {
        var causeToken = expandingTokens.get(0);
        return String.format("%s: error: %s", causeToken.getSpan().getStart(), message);
    }
}
