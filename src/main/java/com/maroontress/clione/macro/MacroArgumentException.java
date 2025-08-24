package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    Thrown to indicate that a function-like macro has been called with the
    wrong number of arguments.
*/
public final class MacroArgumentException extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /** The expected number of arguments. */
    private final int expected;

    /** The actual number of arguments. */
    private final int actual;

    /**
        Creates a new instance.

        @param causeToken The token that triggers this exception.
        @param expected The expected number of arguments.
        @param actual The actual number of arguments.
        @param expandingTokens the list of tokens that represents the macro
        expansion stack.
    */
    public MacroArgumentException(Token causeToken,
            int expected,
            int actual,
            List<Token> expandingTokens) {
        super(newMessage(causeToken, expected, actual, expandingTokens),
                causeToken, expandingTokens);
        this.expected = expected;
        this.actual = actual;
    }

    private static String newMessage(Token causeToken, int expected,
            int actual, List<Token> expandingTokens) {
        var manyOrFew = (actual > expected) ? "many" : "few";
        var format = "%s: error: too %s arguments for macro '%s' "
            + "(expected %d, but got %d)";
        var macroName = expandingTokens.get(0);
        return String.format(format, causeToken.getSpan().getStart(),
            manyOrFew, macroName.getValue(), expected, actual);
    }

    /**
        Returns the expected number of arguments.

        @return The expected number of arguments.
    */
    public int getExpected() {
        return expected;
    }

    /**
        Returns the actual number of arguments.

        @return The actual number of arguments.
    */
    public int getActual() {
        return actual;
    }
}
