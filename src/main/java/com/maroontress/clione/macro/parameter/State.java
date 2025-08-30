package com.maroontress.clione.macro.parameter;

import java.util.function.Consumer;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.macro.FunctionLikeMacroBehavior;
import com.maroontress.clione.macro.PreprocessException;
import com.maroontress.clione.macro.TokenKit;
import com.maroontress.clione.macro.VaArgs;
import com.maroontress.clione.macro.VaArgsKeywordMisusageException;

/**
    The state of the function-like macro parameter parser.
*/
public interface State {

    /**
        The initial state.
    */
    State INITIAL = new UncompletedState(
            ignored -> State.REGULAR_COMPLETED,
            State::nextStateOfIdentifier);

    /**
        The state that expects an identifier.
    */
    State EXPECT_IDENTIFIER = new UncompletedState(
            State::missingParameter,
            State::nextStateOfIdentifier);

    /**
        The state that expects a comma or a close parenthesis.
    */
    State EXPECT_COMMA_OR_PAREN = new UncompletedState(
            ignored -> State.REGULAR_COMPLETED,
            State::nextStateOfPunctuator);

    /**
        The state after an ellipsis.
    */
    State AFTER_ELLIPSIS = new UncompletedState(
            ignored -> State.VARIADIC_COMPLETED,
            State::missingParen);

    /**
        The completed state.
    */
    State REGULAR_COMPLETED = new CompletedState(
            FunctionLikeMacroBehavior.REGULAR);

    /**
        The completed state for the variadic macro.
    */
    State VARIADIC_COMPLETED = new CompletedState(
            FunctionLikeMacroBehavior.VARIADIC);

    /**
        Returns whether the parsing is completed.

        @return true if the parsing is completed, false otherwise.
    */
    boolean isCompleted();

    /**
        Processes a close parenthesis.

        @param token The token.
        @return The next state.
        @throws PreprocessException if an error occurs.
    */
    State onCloseParen(Token token) throws PreprocessException;

    /**
        Processes a token.

        @param token The token.
        @param addParameter The consumer to add a parameter.
        @return The next state.
        @throws PreprocessException if an error occurs.
    */
    State nextState(Token token, Consumer<String> addParameter)
            throws PreprocessException;

    /**
        Returns the function-like macro behavior.

        @return The function-like macro behavior.
    */
    FunctionLikeMacroBehavior getFunctionLikeMacroBehavior();

    private static State nextStateOfIdentifier(Token token,
            Consumer<String> addParameter) throws PreprocessException {
        if (TokenKit.isEllipsis(token)) {
            return State.AFTER_ELLIPSIS;
        }
        if (!token.isType(TokenType.IDENTIFIER)) {
            throw TokenKit.isComma(token)
                ? new MissingMacroParameterException(token)
                : new InvalidMacroParameterTokenException(token);
        }
        if (token.isValue(VaArgs.KEYWORD)) {
            throw new VaArgsKeywordMisusageException(token);
        }
        addParameter.accept(token.getValue());
        return State.EXPECT_COMMA_OR_PAREN;
    }

    private static State nextStateOfPunctuator(Token token,
            Consumer<String> addParameter) throws PreprocessException {
        if (!TokenKit.isComma(token)) {
            throw new MissingCommaInMacroParameterListException(token);
        }
        return State.EXPECT_IDENTIFIER;
    }

    private static State missingParameter(Token token)
            throws PreprocessException {
        throw new MissingMacroParameterException(token);
    }

    private static State missingParen(Token token,
            Consumer<String> addParameter) throws PreprocessException {
        throw new MissingParenInMacroParameterListException(token);
    }
}
