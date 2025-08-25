package com.maroontress.clione.macro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maroontress.clione.Token;

public interface DirectiveHandler {

    void apply(List<Token> directiveTokens, int directiveNameIndex)
            throws PreprocessException;

    static Map<String, DirectiveHandler> newMap(MacroKeeper keeper) {
        var map = new HashMap<String, DirectiveHandler>();
        map.put("define", new DefineHandler(keeper));
        map.put("undef", new UndefHandler(keeper));
        return Map.copyOf(map);
    }
}
