package com.maroontress.clione.macro.parameter;

import java.util.function.Consumer;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.FunctionLikeMacroBehavior;
import com.maroontress.clione.macro.PreprocessException;

/**
    The uncompleted state of the function-like macro parameter parser.
*/
public final class UncompletedState implements State {

    private final PunctuatorFunction onCloseParen;
    private final ParameterFunction nextState;

    /**
        Creates a new instance.

        @param onCloseParen The function to process a close parenthesis.
        @param nextState The function to process a token.
    */
    public UncompletedState(
            PunctuatorFunction onCloseParen,
            ParameterFunction nextState) {
        this.onCloseParen = onCloseParen;
        this.nextState = nextState;
    }

    @Override
    public State onCloseParen(Token token) throws PreprocessException {
        return onCloseParen.apply(token);
    }

    @Override
    public State nextState(Token token, Consumer<String> addParameter)
            throws PreprocessException {
        return nextState.apply(token, addParameter);
    }

    @Override
    public boolean isCompleted() {
        return false;
    }

    @Override
    public FunctionLikeMacroBehavior getFunctionLikeMacroBehavior() {
        throw new UnsupportedOperationException(
                "Unimplemented method 'getFunctionLikeMacroBehavior'");
    }
}
