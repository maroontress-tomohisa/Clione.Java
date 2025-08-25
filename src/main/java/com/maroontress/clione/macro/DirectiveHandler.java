package com.maroontress.clione.macro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maroontress.clione.Token;

/**
    Handles preprocessor directives.
*/
public interface DirectiveHandler {

    /**
        Applies the directive.

        @param directiveTokens the tokens of the directive line.
        @param directiveNameIndex the index of the directive name token.
        @throws PreprocessException if an error occurs during preprocessing.
    */
    void apply(List<Token> directiveTokens, int directiveNameIndex)
            throws PreprocessException;

    /**
        Creates a new map of directive handlers.

        @param keeper the macro keeper.
        @return a new map of directive handlers.
    */
    static Map<String, DirectiveHandler> newMap(MacroKeeper keeper) {
        var map = new HashMap<String, DirectiveHandler>();
        map.put("define", new DefineHandler(keeper));
        map.put("undef", new UndefHandler(keeper));
        return Map.copyOf(map);
    }
}
