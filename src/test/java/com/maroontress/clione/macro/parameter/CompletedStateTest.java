package com.maroontress.clione.macro.parameter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maroontress.clione.macro.FunctionLikeMacroBehavior;
import org.junit.jupiter.api.Test;

public final class CompletedStateTest {

    @Test
    void testOnCloseParen() {
        var state = new CompletedState(FunctionLikeMacroBehavior.REGULAR);
        assertThrows(UnsupportedOperationException.class, () -> {
            state.onCloseParen(null);
        });
    }

    @Test
    void testNextState() {
        var state = new CompletedState(FunctionLikeMacroBehavior.REGULAR);
        assertThrows(UnsupportedOperationException.class, () -> {
            state.nextState(null, null);
        });
    }
}
