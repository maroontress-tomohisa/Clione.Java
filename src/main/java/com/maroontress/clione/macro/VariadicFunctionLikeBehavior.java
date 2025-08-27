package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    The behavior for variadic function-like macros.
*/
public final class VariadicFunctionLikeBehavior
        implements FunctionLikeMacroBehavior {

    /**
        Creates a new instance.
    */
    public VariadicFunctionLikeBehavior() {
        // do nothing
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            FunctionLikeMacro macro, MacroArgument args,
            Preprocessor preprocessor) throws PreprocessException {
        return macro.getVariadicSubstitutionMapping(args, preprocessor);
    }

    @Override
    public BiPredicate<Token, List<String>> getStringizingOperandValidator() {
        return StringizingOperators::variadicOperandValidator;
    }

    @Override
    public MacroArgumentBuilder createArgumentBuilder(
            FunctionLikeMacro macro, Token openParen) {
        return new VariadicMacroArgumentBuilder(macro, openParen);
    }
}
