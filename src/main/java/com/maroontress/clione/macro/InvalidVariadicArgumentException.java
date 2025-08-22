package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a variadic argument is not the last argument.
*/
public final class InvalidVariadicArgumentException
        extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /** The token that is not a valid variadic argument. */
    private final Token invalidToken;

    /**
        Creates a new instance.

        @param message The detail message.
        @param invalidToken The token that is not a valid variadic argument.
        @param expandingTokens The list of tokens that represents the macro
        expansion stack.
    */
    public InvalidVariadicArgumentException(String message,
            Token invalidToken, List<Token> expandingTokens) {
        this(message, invalidToken, expandingTokens,
            getCauseToken(invalidToken, expandingTokens));
    }

    private InvalidVariadicArgumentException(String message,
            Token invalidToken, List<Token> expandingTokens,
            Token causeToken) {
        super(newMessage(message, causeToken), causeToken, expandingTokens);
        this.invalidToken = invalidToken;
    }

    /**
        Returns the token that is not a valid variadic argument.

        @return The token that is not a valid variadic argument.
    */
    public Token getInvalidToken() {
        return invalidToken;
    }

    private static Token getCauseToken(
            Token invalidToken, List<Token> expandingTokens) {
        return expandingTokens.size() == 1
                ? invalidToken
                : expandingTokens.get(0);
    }

    private static String newMessage(String message, Token causeToken) {
        return String.format("%s: error: %s",
            causeToken.getSpan().getStart(),
            message);
    }
}
