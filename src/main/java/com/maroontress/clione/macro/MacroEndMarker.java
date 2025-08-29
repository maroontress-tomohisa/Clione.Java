package com.maroontress.clione.macro;

import java.util.function.Supplier;

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

    public String getName() {
        return name;
    }

    @Override
    public boolean addToArguments(
            MacroArgumentBuilder ignored,
            Supplier<PreprocessException> exceptionSupplier)
            throws PreprocessException {
        throw exceptionSupplier.get();
    }

    @Override
    public <T> T apply(MacroTokenVisitor<T> visitor) {
        return visitor.handleMacroEndMarker(this);
    }
}
