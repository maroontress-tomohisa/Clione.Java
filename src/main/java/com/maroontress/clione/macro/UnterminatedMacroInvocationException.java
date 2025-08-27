package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a function-like macro invocation is not terminated.
*/
public final class UnterminatedMacroInvocationException
        extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /**
        Constructs an instance of this class.

        @param macroName The token of the macro name.
        @param expandingTokens The list of tokens that are being expanded.
    */
    public UnterminatedMacroInvocationException(Token macroName,
            List<Token> expandingTokens) {
        this(macroName, getCauseToken(macroName, expandingTokens),
            expandingTokens);
    }

    private UnterminatedMacroInvocationException(
            Token macroName, Token causeToken, List<Token> expandingTokens) {
        super(newMessage(causeToken), causeToken, expandingTokens);
    }

    private static String newMessage(Token causeToken) {
        return String.format(
            "%s: error: unterminated function-like macro invocation",
            causeToken.getSpan().getStart());
    }

    private static Token getCauseToken(Token macroName,
            List<Token> expandingTokens) {
        return expandingTokens.size() == 0
            ? macroName
            : expandingTokens.get(0);
    }
}
