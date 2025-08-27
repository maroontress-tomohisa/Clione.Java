package com.maroontress.clione.macro.parameter;

import java.util.function.Consumer;

import com.maroontress.clione.Token;
import com.maroontress.clione.macro.FunctionLikeMacroBehavior;

/**
    The completed state of the function-like macro parameter parser.
*/
public class CompletedState implements State {

    private final FunctionLikeMacroBehavior behavior;

    /**
        Creates a new instance.

        @param behavior The function-like macro.
    */
    public CompletedState(FunctionLikeMacroBehavior behavior) {
        this.behavior = behavior;
    }

    @Override
    public State onCloseParen(Token token) {
        throw new UnsupportedOperationException(
                "Unimplemented method 'onCloseParen'");
    }

    @Override
    public State nextState(Token token, Consumer<String> addParameter) {
        throw new UnsupportedOperationException(
                "Unimplemented method 'nextState'");
    }

    @Override
    public boolean isCompleted() {
        return true;
    }

    @Override
    public FunctionLikeMacroBehavior getFunctionLikeMacroBehavior() {
        return behavior;
    }
}
