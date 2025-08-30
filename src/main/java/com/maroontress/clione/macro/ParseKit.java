package com.maroontress.clione.macro;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.Tokens;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
    Provides utility methods for parsing.
*/
public final class ParseKit {

    private MacroKeeper keeper;
    private TokenReservoir reservoir;
    private Set<String> reservedWords;

    /**
        Constructs a new instance.

        @param keeper The macro keeper.
        @param reservoir The token reservoir.
        @param reservedWords The set of reserved words.
    */
    public ParseKit(MacroKeeper keeper, TokenReservoir reservoir, Set<String> reservedWords) {
        this.keeper = keeper;
        this.reservoir = reservoir;
        this.reservedWords = reservedWords;
    }

    /**
        Returns the macro keeper.

        @return The macro keeper
    */
    public MacroKeeper getKeeper() {
        return keeper;
    }

    /**
        Returns the token reservoir.

        @return The token reservoir
    */
    public TokenReservoir getReservoir() {
        return reservoir;
    }

    /**
        Expands the specified macro.

        @param macro The macro
        @param token The token
        @param supplier The supplier of the macro body
        @return The first token of the expansion result
    */
    public Optional<Token> expand(
            Macro macro, Token token, Supplier<List<Token>> supplier) {
        keeper.startExpansion(macro.name(), token);
        reservoir.expandMacro(macro, supplier);
        return Optional.empty();
    }

    /**
        Concatenates the specified tokens.

        @param left The left-hand-side token
        @param right The right-hand-side token
        @return The concatenated token
    */
    public Token concatenate(Token left, Token right) {
        var token = Tokens.concatenate(left, right, reservedWords);
        if (token.isType(TokenType.UNKNOWN)) {
            var cause = new InvalidPreprocessingTokenException(
                    token.getValue(), keeper.getExpandingChain());
            throw new UncheckedIOException(cause);
        }
        return token;
    }

    /**
        Looks ahead for a token that matches the given predicate.

        @param predicate The predicate to match.
        @return An optional containing the matched token, or an empty
            optional if no token matches.
    */
    public Optional<Token> lookAhead(Predicate<Token> predicate) {
        var peeked = new ArrayList<MacroToken>();
        var macroEndNameList = new ArrayList<String>();
        var visitor = new MacroTokenVisitor<Token>() {

            @Override
            public Token visit(MacroEndMarker marker) {
                macroEndNameList.add(marker.getName());
                return null;
            }

            @Override
            public Token visit(WrappedToken wrappedToken) {
                return wrappedToken.unwrap();
            }
        };
        for (;;) {
            var maybeMacroToken = reservoir.nextMacroToken();
            if (maybeMacroToken.isEmpty()) {
                break;
            }

            var macroToken = maybeMacroToken.get();
            peeked.add(macroToken);

            var token = macroToken.apply(visitor);
            if (token == null) {
                continue;
            }
            if (TokenKit.isDelimiterOrComment(token)) {
                continue;
            }
            if (!predicate.test(token)) {
                break;
            }
            for (var name : macroEndNameList) {
                keeper.endExpansion(name);
            }
            return Optional.of(token);
        }
        reservoir.putBack(peeked);
        return Optional.empty();
    }
}
