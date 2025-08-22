package com.maroontress.clione.macro;

import java.util.Optional;

import com.maroontress.clione.Token;

/**
    A marker to signal the end of a macro expansion.
*/
public final class MacroEndMarker implements MacroToken {

    private final String name;

    /**
        Creates a new instance.

        @param name The name of the macro.
    */
    public MacroEndMarker(String name) {
        this.name = name;
    }

    @Override
    public Optional<Token> getToken() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMacroEndName() {
        return Optional.of(name);
    }

    @Override
    public boolean isOriginatingFromParameter() {
        return false;
    }
}
