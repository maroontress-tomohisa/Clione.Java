package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    The base class for exceptions thrown during macro expansion.
*/
public abstract class MacroExpansionException extends PreprocessException {

    private static final long serialVersionUID = 1L;

    /** The list of tokens that represents the macro expansion stack. */
    private final List<Token> expandingTokens;

    /**
        Constructs an instance of this class.

        @param message the detail message.
        @param expandingTokens the list of tokens that represents the macro
        expansion stack.
    */
    protected MacroExpansionException(String message,
            List<Token> expandingTokens) {
        this(message, expandingTokens.get(0), expandingTokens);
    }

    /**
        Constructs an instance of this class.

        @param message the detail message.
        @param causedToken the token that caused this exception.
        @param expandingTokens the list of tokens that represents the macro
        expansion stack.
    */
    protected MacroExpansionException(String message,
            Token causedToken,
            List<Token> expandingTokens) {
        super(message, causedToken);
        this.expandingTokens = List.copyOf(expandingTokens);
    }

    /**
        Returns the list of tokens that represents the macro expansion stack.

        @return the list of tokens.
    */
    public List<Token> getExpandingTokens() {
        return expandingTokens;
    }
}
