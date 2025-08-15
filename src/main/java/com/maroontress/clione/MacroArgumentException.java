package com.maroontress.clione;

import java.util.List;

/**
    Thrown to indicate that a function-like macro has been called with the
    wrong number of arguments.
*/
public final class MacroArgumentException extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /** The name of the macro. */
    private final String macroName;

    /** The expected number of arguments. */
    private final int expected;

    /** The actual number of arguments. */
    private final int actual;

    /**
        Creates a new instance.

        @param macroName The name of the macro.
        @param expected The expected number of arguments.
        @param actual The actual number of arguments.
        @param expandingTokens the list of tokens that represents the macro
        expansion stack.
    */
    public MacroArgumentException(String macroName,
            int expected,
            int actual,
            List<Token> expandingTokens) {
        super(newMessage(macroName, expected, actual, expandingTokens),
              expandingTokens);
        this.macroName = macroName;
        this.expected = expected;
        this.actual = actual;
    }

    private static String newMessage(String macroName, int expected,
            int actual, List<Token> expandingTokens) {
        var manyOrFew = (actual > expected) ? "many" : "few";
        var format = "%s: error: too %s arguments for macro '%s' "
            + "(expected %d, but got %d)";
        var causeToken = expandingTokens.get(0);
        return String.format(format, causeToken.getSpan().getStart(),
            manyOrFew, macroName, expected, actual);
    }

    /**
        Returns the name of the macro.

        @return The name of the macro.
    */
    public String getMacroName() {
        return macroName;
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
