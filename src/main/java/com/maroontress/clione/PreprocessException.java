package com.maroontress.clione;

import java.io.IOException;

/**
    The base class for exceptions thrown during preprocessing.
*/
public abstract class PreprocessException extends IOException {

    private static final long serialVersionUID = 1L;

    /** The token that caused this exception. */
    private final Token causeToken;

    /**
        Constructs an instance of this class with the specified detail message.

        @param message the detail message.
        @param causeToken the token that caused the exception.
    */
    protected PreprocessException(String message, Token causeToken) {
        super(message);
        this.causeToken = causeToken;
    }

    /**
        Constructs an instance of this class with the specified detail message
        and cause.

        @param message the detail message.
        @param cause the cause.
        @param causeToken the token that caused the exception.
    */
    protected PreprocessException(String message, Throwable cause,
            Token causeToken) {
        super(message, cause);
        this.causeToken = causeToken;
    }

    /**
        Returns the token that caused the exception.

        @return the token that caused the exception.
    */
    public Token getCauseToken() {
        return causeToken;
    }
}
