package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Handles the #undef directive.
*/
public final class UndefHandler implements DirectiveHandler {

    private final MacroKeeper keeper;

    /**
        Constructs a new instance.

        @param keeper the macro keeper.
    */
    public UndefHandler(MacroKeeper keeper) {
        this.keeper = keeper;
    }

    /** {@inheritDoc} */
    @Override
    public void apply(List<Token> directiveTokens, int directiveNameIndex)
            throws PreprocessException {
        var maybeNamePair = TokenKit.findSignificantToken(
                directiveTokens, directiveNameIndex + 1);
        if (maybeNamePair.isEmpty()) {
            throw new MissingMacroNameException(directiveTokens.getLast());
        }
        var namePair = maybeNamePair.get();
        var macroNameToken = namePair.token();
        if (!macroNameToken.isType(TokenType.IDENTIFIER)) {
            throw new InvalidMacroNameException(macroNameToken);
        }
        keeper.undefineMacro(macroNameToken.getValue());
    }
}
