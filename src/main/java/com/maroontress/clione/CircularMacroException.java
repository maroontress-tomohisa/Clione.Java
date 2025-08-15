package com.maroontress.clione;

import java.util.ArrayList;
import java.util.List;

/**
    Thrown to indicate that a circular macro expansion has been detected.
*/
public final class CircularMacroException extends MacroExpansionException {

    private static final long serialVersionUID = 1L;

    /** The name of the macro where the circular expansion was detected. */
    private final String macroName;

    /** A string representing the expansion path that forms the cycle. */
    private final String cyclePath;

    /**
        Constructs an instance of this class.

        @param macroName The name of the macro where the circular expansion
        was detected.
        @param cyclePath A string representing the expansion path that forms
        the cycle.
        @param preprocessor The preprocessor instance.
    */
    public CircularMacroException(
            final String macroName,
            final String cyclePath,
            final Preprocessor preprocessor) {
        super(buildMessage(macroName,
                           cyclePath,
                           new ArrayList<>(preprocessor.getExpandingMacros().values())),
              new ArrayList<>(preprocessor.getExpandingMacros().values()));
        this.macroName = macroName;
        this.cyclePath = cyclePath;
    }

    private static String buildMessage(
            final String macroName,
            final String cyclePath,
            final List<Token> expandingTokens) {
        var causeToken = expandingTokens.get(0);
        return String.format(
                "%s: error: circular macro expansion for '%s', path: %s",
                causeToken.getSpan().getStart(), macroName, cyclePath);
    }

    /**
        Returns the name of the macro where the circular expansion was
        detected.

        @return The macro name.
    */
    public String getMacroName() {
        return macroName;
    }

    /**
        Returns a string representing the expansion path that forms the cycle.

        @return The cycle path.
    */
    public String getCyclePath() {
        return cyclePath;
    }
}
