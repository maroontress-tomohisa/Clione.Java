package com.maroontress.clione.macro;

/**
    Visits macro tokens during expansion.
*/
public interface MacroExpansionVisitor {

    /**
        Expands a macro end marker.

        @param marker The macro end marker.
    */
    void expandMacroEndMarker(MacroEndMarker marker);

    /**
        Expands a wrapped token.

        @param wrappedToken The wrapped token.
        @throws PreprocessException If an error occurs during expansion.
    */
    void expandWrappedToken(WrappedToken wrappedToken)
            throws PreprocessException;
}
