package com.maroontress.clione.macro;

import com.maroontress.clione.Token;
import java.util.List;

/**
    Supplies the list of {@code Token} instances as a macro body.
*/
@FunctionalInterface
public interface BodySupplier {
    /**
        Returns the list of {@code Token} instances.

        @return The list of {@code Token} instances
        @throws PreprocessException if it fails to preprocess
    */
    List<Token> get() throws PreprocessException;
}
