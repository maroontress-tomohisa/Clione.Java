package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    Thrown when a directive is found within macro arguments.
*/
public final class DirectiveWithinMacroArgumentsException extends
        MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /**
        Creates a new instance.

        @param directive The token that represents the directive.
        @param expandingTokens The list of tokens that represents the macro
        expansion stack.
    */
    public DirectiveWithinMacroArgumentsException(
            Token directive, List<Token> expandingTokens) {
        super(newMessage(directive), directive, expandingTokens);
    }

    private static String newMessage(Token causeToken) {
        return String.format(
            "%s: error: embedding a directive within macro arguments has "
                + "undefined behavior",
            causeToken.getSpan().getStart());
    }
}
