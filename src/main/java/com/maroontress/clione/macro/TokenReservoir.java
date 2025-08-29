package com.maroontress.clione.macro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Predicate;

import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.Token;

public final class TokenReservoir {

    private final LexicalParser parser;
    private final Deque<MacroToken> tokenQueue = new ArrayDeque<>();

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

    public void expandMacro(Macro macro, BodySupplier supplier)
            throws PreprocessException {
        var name = macro.name();
        tokenQueue.addFirst(new MacroEndMarker(name));
        var queue = new ArrayDeque<>(supplier.get());
        while (!queue.isEmpty()) {
            tokenQueue.addFirst(WrappedToken.of(queue.removeLast()));
        }
    }

    public Optional<Token> lookAhead(Predicate<Token> predicate) {
        var peeked = new ArrayList<MacroToken>();

        while (true) {
            var nextOpt = nextMacroToken();
            if (nextOpt.isEmpty()) {
                // EOF, not found. Put stuff back.
                for (int i = peeked.size() - 1; i >= 0; i--) {
                    tokenQueue.addFirst(peeked.get(i));
                }
                return Optional.empty();
            }

            var next = nextOpt.get();
            peeked.add(next);

            var token = next.apply(new MacroTokenVisitor<Token>() {

                @Override
                public Token handleMacroEndMarker(MacroEndMarker marker) {
                    return null;
                }

                @Override
                public Token handleWrappedToken(WrappedToken wrappedToken) {
                    return wrappedToken.unwrap();
                }
            });
            if (token != null) {
                if (TokenKit.isDelimiterOrComment(token)) {
                    continue;
                }
                if (predicate.test(token)) {
                    return Optional.of(token);
                }
            }

            for (var k = peeked.size() - 1; k >= 0; --k) {
                tokenQueue.addFirst(peeked.get(k));
            }
            return Optional.empty();
        }
    }
}
