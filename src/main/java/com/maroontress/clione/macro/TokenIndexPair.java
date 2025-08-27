package com.maroontress.clione.macro;

import com.maroontress.clione.Token;

/**
    Represents a pair of a token and its index.
    @param token The token.
    @param index The index of the token.
*/
public record TokenIndexPair(Token token, int index) {
}
