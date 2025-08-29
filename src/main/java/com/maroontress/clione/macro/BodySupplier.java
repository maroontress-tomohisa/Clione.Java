package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

@FunctionalInterface
public interface BodySupplier {
    List<Token> get() throws PreprocessException;
}
