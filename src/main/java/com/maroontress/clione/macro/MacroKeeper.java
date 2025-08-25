package com.maroontress.clione.macro;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.maroontress.clione.Token;

/**
    Manages defined macros and the macro expansion chain.
*/
public final class MacroKeeper {

    private final Map<String, Macro> definedMacroMap = new HashMap<>();
    private final Map<String, Token> expandingChain = new LinkedHashMap<>();

    /**
        Defines a new macro.

        @param macro The macro to define.
    */
    public void defineMacro(Macro macro) {
        var name = macro.name();
        definedMacroMap.put(name, macro);
    }

    /**
        Removes a macro definition.

        @param name The name of the macro to undefine.
    */
    public void undefineMacro(String name) {
        definedMacroMap.remove(name);
    }

    /**
        Marks a macro as currently expanding.

        @param name The name of the macro.
        @param token The token that triggered the expansion.
    */
    public void startExpansion(String name, Token token) {
        expandingChain.put(name, token);
    }

    /**
        Checks if a macro is currently expanding.

        @param name The name of the macro.
        @return {@code true} if the macro is expanding,
            otherwise {@code false}.
    */
    public boolean isExpanding(String name) {
        return expandingChain.containsKey(name);
    }

    /**
        Marks the end of a macro's expansion.

        @param name The name of the macro.
    */
    public void endExpansion(String name) {
        expandingChain.remove(name);
    }

    /**
        Returns the current chain of expanding macros.

        @return The list of tokens that triggered the current
            macro expansions.
    */
    public List<Token> getExpandingChain() {
        return List.copyOf(expandingChain.values());
    }

    /**
        Gets a macro definition if it's not currently being expanded.

        <p>If the specified macro is currently in the process of expansion,
        this method returns an empty optional to prevent recursion.</p>

        @param name The name of the macro.
        @return An optional containing the macro, or an empty optional.
    */
    public Optional<Macro> getMacro(String name) {
        return (expandingChain.containsKey(name))
            ? Optional.empty()
            : Optional.ofNullable(definedMacroMap.get(name));
    }
}
