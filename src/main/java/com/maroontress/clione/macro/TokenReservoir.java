package com.maroontress.clione.macro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.Token;

/**
    The token stream for macro expansion.
*/
public final class TokenReservoir {

    private final LexicalParser parser;
    private final Deque<MacroToken> tokenQueue = new ArrayDeque<>();

    /**
        Initializes the instance.

        @param parser The lexical parser
    */
    public TokenReservoir(LexicalParser parser) {
        this.parser = parser;
    }

    /**
        Returns the next macro token from the preprocessor stream.

        <p>This method first checks the internal token queue, which may
        contain tokens from a macro expansion. If the queue is empty, it
        fetches the next token from the underlying lexical parser.</p>

        @return An optional containing the next macro token, or an empty
            optional if the end of the stream is reached.
    */
    public Optional<MacroToken> nextMacroToken() {
        if (!tokenQueue.isEmpty()) {
            return Optional.of(tokenQueue.removeFirst());
        }
        try {
            return parser.next().map(WrappedToken::of);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
        Expands the specified macro.

        @param macro The macro
        @param supplier The supplier of the macro body
    */
    public void expandMacro(Macro macro, Supplier<List<Token>> supplier) {
        var name = macro.name();
        tokenQueue.addFirst(new MacroEndMarker(name));
        var queue = new ArrayDeque<>(supplier.get());
        while (!queue.isEmpty()) {
            tokenQueue.addFirst(WrappedToken.of(queue.removeLast()));
        }
    }

    /**
        Puts back a list of macro tokens to the reservoir.

        @param list The list of macro tokens to put back.
    */
    public void putBack(List<MacroToken> list) {
        for (var i = list.size() - 1; i >= 0; --i) {
            tokenQueue.addFirst(list.get(i));
        }
    }
}
