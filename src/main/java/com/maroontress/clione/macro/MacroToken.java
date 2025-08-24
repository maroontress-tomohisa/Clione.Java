package com.maroontress.clione.macro;

import java.util.Optional;

import com.maroontress.clione.Preprocessor.ExpansionVisitor;
import com.maroontress.clione.Token;

/**
    Represents a token in the preprocessor queue.
*/
public interface MacroToken {

    /**
        Returns the token if this instance represents a token.

        @return An optional containing the token, or an empty optional.
    */
    Optional<Token> getToken();

    /**
        Returns the name of the macro that has just finished expanding.

        @return An optional containing the macro name, or an empty optional.
    */
    Optional<String> getMacroEndName();

    void expand(ExpansionVisitor visitor) throws PreprocessException;
}
