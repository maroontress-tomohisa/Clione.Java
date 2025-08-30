package com.maroontress.clione.macro;

/**
    The visitor of {@code MacroToken}.

    @param <T> The type of a value to be returned
*/
public interface MacroTokenVisitor<T> {

    /**
        Handles the specified marker.

        @param marker The marker
        @return The result of handling
    */
    T handleMacroEndMarker(MacroEndMarker marker);

    /**
        Handles the specified token.

        @param wrappedToken The token
        @return The result of handling
    */
    T handleWrappedToken(WrappedToken wrappedToken);
}
