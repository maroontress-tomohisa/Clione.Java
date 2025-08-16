package com.maroontress.clione.macro;

import java.util.List;
import java.util.stream.Stream;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that `__VA_ARGS__` is used with zero arguments and is
    preceded by a comma.
*/
public final class InvalidVariadicArgumentException
        extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param message The detail message.
        @param tokenToPrepend The token to prepend to the expansion stack.
    */
    public InvalidVariadicArgumentException(String message,
            List<Token> expandingTokens,
            Token tokenToPrepend) {
        this(message, buildExpandingTokens(expandingTokens, tokenToPrepend));
    }

    private InvalidVariadicArgumentException(
            String message, List<Token> expandingTokens) {
        super(newMessage(message, expandingTokens.get(0)), expandingTokens);
    }

    private static List<Token> buildExpandingTokens(
            List<Token> expandingTokens, Token tokenToPrepend) {
        return Stream.concat(
                Stream.of(tokenToPrepend), expandingTokens.stream())
            .toList();
    }

    private static String newMessage(String message, Token causeToken) {
        return String.format("%s: error: %s",
            causeToken.getSpan().getStart(),
            message);
    }
}
