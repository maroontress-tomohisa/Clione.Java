package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Builds arguments for non-variadic function-like macros.
*/
public final class DefaultMacroArgumentBuilder extends MacroArgumentBuilder {
    /**
        Constructs a new instance.

        @param kit The parse kit.
        @param macro The macro for which the builder is created.
        @param openParen The opening parenthesis of the argument list.
    */
    public DefaultMacroArgumentBuilder(
            ParseKit kit, FunctionLikeMacro macro, Token openParen) {
        super(kit, macro, openParen);
    }

    @Override
    protected boolean skipsComma() {
        return false;
    }
}
