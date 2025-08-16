package com.maroontress.clione.macro;

import java.util.List;
import java.util.stream.Stream;

import com.maroontress.clione.Token;

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
    */
    public CircularMacroException(
            String macroName,
            List<Token> expandingTokens) {
        this(macroName,
            newCyclePath(macroName, expandingTokens),
            expandingTokens);
    }

    private CircularMacroException(
            String macroName,
            String cyclePath,
            List<Token> expandingTokens) {
        super(newMessage(macroName, cyclePath, expandingTokens.get(0)),
            expandingTokens);
        this.macroName = macroName;
        this.cyclePath = cyclePath;
    }

    private static String newCyclePath(
            String macroName, List<Token> expandingTokens) {
        var path = Stream.concat(
                expandingTokens.stream().map(t -> t.getValue()),
                Stream.of(macroName))
            .toList();
        return String.join(" -> ", path);
    }

    private static String newMessage(
            String macroName, String cycle, Token causeToken) {
        return String.format(
                "%s: error: circular macro expansion for '%s', path: %s",
                causeToken.getSpan().getStart(), macroName, cycle);
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
