package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    The default macro behavior for function-like macros.
*/
public final class DefaultMacroBehavior implements MacroBehavior {

    /**
        Creates a new instance.
    */
    public DefaultMacroBehavior() {
        // do nothing
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            FunctionLikeMacro macro, MacroArgument args,
            Preprocessor preprocessor) throws PreprocessException {
        return macro.getDefaultSubstitutionMapping(args, preprocessor);
    }

    @Override
    public MacroArgumentBuilder createArgumentBuilder(
            FunctionLikeMacro macro, Token openParen) {
        return new DefaultMacroArgumentBuilder(macro, openParen);
    }
}
