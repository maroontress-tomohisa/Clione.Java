package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    The macro behavior for variadic function-like macros.
*/
public final class VariadicMacroBehavior implements MacroBehavior {

    /**
        Creates a new instance.
    */
    public VariadicMacroBehavior() {
        // do nothing
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            FunctionLikeMacro macro, MacroArgument args,
            Preprocessor preprocessor) throws PreprocessException {
        return macro.getVariadicSubstitutionMapping(args, preprocessor);
    }

    @Override
    public MacroArgumentBuilder createArgumentBuilder(
            FunctionLikeMacro macro, Token openParen) {
        return new VariadicMacroArgumentBuilder(macro, openParen);
    }
}
