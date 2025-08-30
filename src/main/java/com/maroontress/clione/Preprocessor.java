package com.maroontress.clione;

import com.maroontress.clione.macro.MacroKeeper;
import com.maroontress.clione.macro.MacroTokenVisitor;
import com.maroontress.clione.macro.DirectiveHandler;
import com.maroontress.clione.macro.ParseKit;
import com.maroontress.clione.macro.InvalidPreprocessingDirectiveException;
import com.maroontress.clione.macro.MacroEndMarker;
import com.maroontress.clione.macro.TokenKit;
import com.maroontress.clione.macro.TokenReservoir;
import com.maroontress.clione.macro.WrappedToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
    The preprocessor that handles macro definitions and substitutions.

    <p>This class wraps a {@link LexicalParser} and intercepts the token stream
    to process preprocessor directives like {@code #define} and {@code #undef}.
    It maintains a map of defined macros and performs substitutions when
    identifiers are encountered.</p>
*/
public final class Preprocessor implements LexicalParser {

    private final LexicalParser parser;
    private final MacroKeeper keeper = new MacroKeeper();
    private final Map<String, DirectiveHandler> handlerMap;
    private final TokenReservoir reservoir;
    private final ParseKit kit;
    private final MacroTokenVisitor<Optional<Token>> tokenVisiror;

    /**
        Creates a new instance.

        @param parser The lexical parser to wrap.
    */
    public Preprocessor(LexicalParser parser) {
        this.parser = parser;
        this.reservoir = new TokenReservoir(parser);
        this.handlerMap = DirectiveHandler.newMap(keeper);
        this.kit = new ParseKit(keeper, reservoir, parser.getReservedWords());
        this.tokenVisiror = newVisitor(this);
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    @Override
    public Optional<SourceChar> getEof() throws IOException {
        return parser.getEof();
    }

    @Override
    public SourceLocation getLocation() {
        return parser.getLocation();
    }

    @Override
    public Optional<Token> next() throws IOException {
        for (;;) {
            var maybeMacroToken = reservoir.nextMacroToken();
            if (maybeMacroToken.isEmpty()) {
                return Optional.empty();
            }

            var macroToken = maybeMacroToken.get();
            try {
                var result = macroToken.apply(tokenVisiror);
                if (result.isPresent()) {
                    return result;
                }
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }

    private Optional<Token> handleToken(Token token)
            throws IOException {
        var type = token.getType();
        if (type == TokenType.IDENTIFIER) {
            return handleIdentifier(token);
        }
        if (type == TokenType.DIRECTIVE) {
            updateMacrosFromDirective(token);
        }
        return Optional.of(token);
    }

    @Override
    public Set<String> getReservedWords() {
        return parser.getReservedWords();
    }

    /**
        Updates the macro map from a directive token.

        @param token The directive token.
        @throws IOException If an I/O error occurs.
    */
    private void updateMacrosFromDirective(Token token) throws IOException {
        var children = token.getChildren();
        /*
            The directive token's children always include at least the
            DIRECTIVE_END token, so checking for an empty list here is
            unnecessary.

            // if (children.isEmpty()) {
            //     return;
            // }
        */
        var maybePair = TokenKit.findSignificantToken(children, 0);
        if (maybePair.isEmpty()) {
            return;
        }
        var pair = maybePair.get();
        var directiveNameToken = pair.token();
        if (!directiveNameToken.isType(TokenType.DIRECTIVE_NAME)) {
            throw new InvalidPreprocessingDirectiveException(directiveNameToken);
        }

        var directiveNameIndex = pair.index();
        var directiveName = directiveNameToken.getValue();
        var handler = handlerMap.get(directiveName);
        if (handler == null) {
            throw new UnsupportedOperationException(
                    directiveNameToken + ": not yet implemented");
        }
        handler.apply(children, directiveNameIndex);
    }

    /**
        Handles an identifier token.

        @param token The identifier token.
        @return An optional containing the next token, or an empty optional if
        the identifier is a macro to be expanded.
        @throws IOException If an I/O error occurs.
    */
    private Optional<Token> handleIdentifier(Token token) throws IOException {
        var name = token.getValue();
        var maybeMacro = keeper.getMacro(name);
        if (maybeMacro.isEmpty()) {
            return Optional.of(token);
        }
        var macro = maybeMacro.get();
        return macro.apply(kit, token);
    }

    /**
        Handles a macro end marker.

        @param name The name of the macro.
    */
    private void handleEndMarker(String name) {
        keeper.endExpansion(name);
    }

    private static MacroTokenVisitor<Optional<Token>> newVisitor(
            Preprocessor self) {
        return new MacroTokenVisitor<Optional<Token>>() {

            @Override
            public Optional<Token> visit(MacroEndMarker marker) {
                self.handleEndMarker(marker.getName());
                return Optional.empty();
            }

            @Override
            public Optional<Token> visit(WrappedToken wrappedToken) {
                try {
                    return self.handleToken(wrappedToken.unwrap());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
