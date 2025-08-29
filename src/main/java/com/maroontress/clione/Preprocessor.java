package com.maroontress.clione;

import com.maroontress.clione.macro.MacroKeeper;
import com.maroontress.clione.macro.MacroTokenVisitor;
import com.maroontress.clione.macro.BodySupplier;
import com.maroontress.clione.macro.DirectiveHandler;
import com.maroontress.clione.macro.Foo;
import com.maroontress.clione.macro.InvalidPreprocessingDirectiveException;
import com.maroontress.clione.macro.InvalidPreprocessingTokenException;
import com.maroontress.clione.macro.Macro;
import com.maroontress.clione.macro.MacroEndMarker;
import com.maroontress.clione.macro.PreprocessException;
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
    private final Foo foo;

    /**
        Creates a new instance.

        @param parser The lexical parser to wrap.
    */
    public Preprocessor(LexicalParser parser) {
        this.parser = parser;
        this.reservoir = new TokenReservoir(parser);
        this.handlerMap = DirectiveHandler.newMap(keeper);
        this.foo = new Foo() {

            @Override
            public MacroKeeper getKeeper() {
                return keeper;
            }

            @Override
            public TokenReservoir getReservoir() {
                return reservoir;
            }

            @Override
            public Optional<Token> expand(Macro macro, Token token, BodySupplier supplier)
                    throws PreprocessException {
                keeper.startExpansion(macro.name(), token);
                reservoir.expandMacro(macro, supplier);
                return Optional.empty();
            }

            @Override
            public Token concatenate(Token left, Token right) throws PreprocessException {
                var token = Tokens.concatenate(left, right, getReservedWords());
                if (token.isType(TokenType.UNKNOWN)) {
                    throw new InvalidPreprocessingTokenException(
                        token.getValue(),
                        keeper.getExpandingChain());
                }
                return token;
            }
        };
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
            var visitor = new MacroTokenVisitor<Optional<Token>>() {

                @Override
                public Optional<Token> handleMacroEndMarker(MacroEndMarker marker) {
                    handleEndMarker(marker.getName());
                    return Optional.empty();
                }

                @Override
                public Optional<Token> handleWrappedToken(WrappedToken wrappedToken) {
                    try {
                        return handleToken(wrappedToken.unwrap());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
            try {
                var result = macroToken.apply(visitor);
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
        return macro.apply(foo, token);
    }

    /**
        Handles a macro end marker.

        @param name The name of the macro.
    */
    public void handleEndMarker(String name) {
        keeper.endExpansion(name);
    }
}
