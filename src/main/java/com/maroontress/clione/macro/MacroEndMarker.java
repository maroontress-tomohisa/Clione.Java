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

    /** {@inheritDoc} */
    @Override
    public Optional<Token> getToken() {
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> getMacroEndName() {
        return Optional.of(name);
    }

    /** {@inheritDoc} */
    @Override
    public void expand(MacroExpansionVisitor visitor) {
        visitor.expandMacroEndMarker(this);
    }
}
