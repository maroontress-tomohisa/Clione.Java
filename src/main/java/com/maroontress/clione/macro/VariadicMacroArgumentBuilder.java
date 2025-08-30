package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Builds arguments for variadic function-like macros.
*/
public final class VariadicMacroArgumentBuilder extends MacroArgumentBuilder {

    /**
        Constructs a new instance.

        @param kit The parse kit.
        @param macro The macro for which the builder is created.
        @param openParen The opening parenthesis of the argument list.
    */
    VariadicMacroArgumentBuilder(
            ParseKit kit, FunctionLikeMacro macro, Token openParen) {
        super(kit, macro, openParen);
    }

    @Override
    protected boolean skipsComma() {
        var args = getArgs();
        return args.size() >= getMacroParameterSize();
    }
}
