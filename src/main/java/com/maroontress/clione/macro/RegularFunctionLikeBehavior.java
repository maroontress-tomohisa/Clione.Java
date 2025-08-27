package com.maroontress.clione.macro;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;

/**
    The behavior for regular function-like macros.
*/
public final class RegularFunctionLikeBehavior
        implements FunctionLikeMacroBehavior {

    /**
        Creates a new instance.
    */
    public RegularFunctionLikeBehavior() {
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

    @Override
    public BiPredicate<Token, List<String>> getStringizingOperandValidator() {
        return StringizingOperators::defaultOperandValidator;
    }

    @Override
    public void validateVaArgKeyword(List<Token> body)
            throws PreprocessException {
        DefineHandler.validateVaArgKeyword(body);
    }
}
