package com.maroontress.clione.macro;

import java.util.Collection;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Utility for handling the C-style variadic macro token {@code __VA_ARGS__}.
*/
public final class VaArgs {

    /**
        The {@code __VA_ARGS__} keyword.
    */
    public static final String KEYWORD = "__VA_ARGS__";

    private VaArgs() {
    }

    /**
        Validates the specified macro body whether it contains the {@code
        __VA_ARGS__} keyword.

        <p>If the keyword is found this method throws {@link
        VaArgsKeywordMisusageException}, which includes the offending {@link
        Token} so callers can report precise diagnostics (position, value,
        etc.).</p>

        @param body The macro body as a collection of {@link Token} objects.
        @throws VaArgsKeywordMisusageException If the macro body contains the
            {@code __VA_ARGS__} keyword.
    */
    public static void validateBody(Collection<Token> body)
            throws VaArgsKeywordMisusageException {
        var maybeVaArg = body.stream()
                .filter(t -> {
                    return t.isType(TokenType.IDENTIFIER)
                        && t.isValue(VaArgs.KEYWORD);
                })
                .findFirst();
        if (maybeVaArg.isPresent()) {
            throw new VaArgsKeywordMisusageException(maybeVaArg.get());
        }
    }
}
