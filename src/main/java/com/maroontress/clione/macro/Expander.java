package com.maroontress.clione.macro;

import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Expands macro tokens recursively.
*/
public final class Expander {

    private final Foo foo;
    private final MacroKeeper keeper;
    private final List<WrappedToken> result = new ArrayList<>();
    private final Deque<MacroToken> workQueue;

    /**
        Constructs a new instance.

        @param foo The facade of the macro expansion engine
        @param tokens The list of tokens to expand.
    */
    public Expander(Foo foo, List<WrappedToken> tokens) {
        this.foo = foo;
        this.keeper = foo.getKeeper();
        this.workQueue = new ArrayDeque<>(tokens);
    }

    /**
        Applies the expansion process.

        @return The list of expanded tokens.
        @throws PreprocessException If an error occurs during expansion.
    */
    public List<WrappedToken> apply() throws PreprocessException {
        while (!workQueue.isEmpty()) {
            var macroToken = workQueue.removeFirst();
            macroToken.apply(new MacroTokenVisitor<Void>() {

                @Override
                public Void handleMacroEndMarker(MacroEndMarker marker) {
                    expandMacroEndMarker(marker);
                    return null;
                }

                @Override
                public Void handleWrappedToken(WrappedToken wrappedToken) {
                    expandWrappedToken(wrappedToken);
                    return null;
                }
            });
        }
        return result;
    }

    private void expandMacroEndMarker(MacroEndMarker marker) {
        keeper.endExpansion(marker.getName());
    }

    private void expandWrappedToken(WrappedToken wrappedToken) {
        var token = wrappedToken.unwrap();
        if (!wrappedToken.isOriginatingFromParameter()
                || !token.isType(TokenType.IDENTIFIER)) {
            result.add(wrappedToken);
            return;
        }
        var name = token.getValue();
        var maybeMacro = keeper.getMacro(name);
        if (maybeMacro.isEmpty()) {
            result.add(wrappedToken);
            return;
        }
        var macro = maybeMacro.get();
        if (macro instanceof FunctionLikeMacro) {
            expandFunctionLikeMacro(wrappedToken, (FunctionLikeMacro) macro);
        } else {
            expandObjectLikeMacro(token, macro);
        }
    }

    /**
        Expands an object-like macro.

        @param token The token that triggered the expansion.
        @param macro The macro to expand.
    */
    public void expandObjectLikeMacro(Token token, Macro macro) {
        var name = macro.name();
        keeper.startExpansion(name, token);
        workQueue.addFirst(new MacroEndMarker(name));
        var body = macro.body();
        for (var i = body.size() - 1; i >= 0; --i) {
            workQueue.addFirst(new ParameterOriginatedToken(body.get(i)));
        }
    }

    /**
        Expands a function-like macro.

        @param wrappedToken The wrapped token that triggered the expansion.
        @param macro The macro to expand.
    */
    public void expandFunctionLikeMacro(
            WrappedToken wrappedToken, FunctionLikeMacro macro) {
        var openParenOpt = lookAheadForParen(workQueue);
        if (openParenOpt.isEmpty()) {
            result.add(wrappedToken);
            return;
        }
        var openParen = openParenOpt.get();
        // We need to find the opening parenthesis and remove it.
        // The lookAheadForParen just peeks.
        while (!workQueue.isEmpty()) {
            var macroToken = workQueue.removeFirst();
            var token = macroToken.apply(new MacroTokenVisitor<Token>() {
                @Override
                public Token handleMacroEndMarker(MacroEndMarker marker) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Token handleWrappedToken(WrappedToken wrappedToken) {
                    return wrappedToken.unwrap();
                }
            });
            if (token == openParen) {
                break;
            }
        }
        var builder = macro.newArgumentBuilder(openParen);
        while (!workQueue.isEmpty()) {
            var next = workQueue.removeFirst();
            var token = next.apply(new MacroTokenVisitor<Token>() {

                @Override
                public Token handleMacroEndMarker(MacroEndMarker marker) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Token handleWrappedToken(WrappedToken wrappedToken) {
                    return wrappedToken.unwrap();
                }
            });
            if (builder.addToken(token)) {
                // Found closing paren
                break;
            }
        }
        var name = macro.name();
        var token = wrappedToken.unwrap();
        var args = builder.build();
        try {
            var substituted = macro.substitute(token, args, foo);
            keeper.startExpansion(name, token);
            workQueue.addFirst(new MacroEndMarker(name));
            for (var i = substituted.size() - 1; i >= 0; --i) {
                workQueue.addFirst(
                    new ParameterOriginatedToken(substituted.get(i)));
            }
        } catch (PreprocessException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<Token> lookAheadForParen(Collection<MacroToken> list) {
        // Deque x2 でロールバックするように修正
        var iter = list.iterator();
        while (iter.hasNext()) {
            var macroToken = iter.next();
            var token = macroToken.apply(new MacroTokenVisitor<Token>() {

                @Override
                public Token handleMacroEndMarker(MacroEndMarker marker) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Token handleWrappedToken(WrappedToken wrappedToken) {
                    return wrappedToken.unwrap();
                }
            });
            if (TokenKit.isDelimiterOrComment(token)) {
                continue;
            }
            return TokenKit.isOpenParenthesis(token)
                ? Optional.of(token)
                : Optional.empty();
        }
        return Optional.empty();
    }
}
