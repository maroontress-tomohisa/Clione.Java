package com.maroontress.clione.macro;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

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
            FunctionLikeMacro macro, MacroArgument args, MacroKeeper keeper)
            throws PreprocessException {
        return macro.getDefaultSubstitutionMapping(args, keeper);
    }

    @Override
    public MacroArgumentBuilder createArgumentBuilder(
            ParseKit kit, FunctionLikeMacro macro, Token openParen) {
        return new DefaultMacroArgumentBuilder(kit, macro, openParen);
    }

    @Override
    public BiPredicate<Token, List<String>> getStringizingOperandValidator() {
        return StringizingOperators::defaultOperandValidator;
    }

    @Override
    public void validateVaArgKeyword(Collection<Token> body)
            throws PreprocessException {
        VaArgs.validateBody(body);
    }
}
