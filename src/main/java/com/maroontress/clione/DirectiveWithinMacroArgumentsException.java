package com.maroontress.clione;

import java.util.List;

public final class DirectiveWithinMacroArgumentsException extends
        MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param message The detail message.
        @param expandingTokens The list of tokens that represents the macro
        expansion stack.
    */
    public DirectiveWithinMacroArgumentsException(String message,
            Token directive,
            List<Token> expandingTokens) {
        super(newMessage(message, directive), directive, expandingTokens);
    }

    private static String newMessage(String message, Token causeToken) {
        return String.format("%s: error: %s",
                causeToken.getSpan().getStart(),
                message);
    }
}
