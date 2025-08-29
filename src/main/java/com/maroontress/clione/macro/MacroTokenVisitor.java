package com.maroontress.clione.macro;

public interface MacroTokenVisitor<T> {

    T handleMacroEndMarker(MacroEndMarker marker);

    T handleWrappedToken(WrappedToken wrappedToken);
}
