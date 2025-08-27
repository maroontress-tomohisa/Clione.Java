package com.maroontress.clione.macro.parameter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public final class UncompletedStateTest {

    @Test
    void testGetFunctionLikeMacroBehavior() {
        var state = new UncompletedState(null, null);
        assertThrows(UnsupportedOperationException.class, () -> {
            state.getFunctionLikeMacroBehavior();
        });
    }
}
