package com.maroontress.clione;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * The preprocessor that handles macro definitions and substitutions.
 *
 * <p>This class wraps a {@link LexicalParser} and intercepts the token stream
 * to process preprocessor directives like {@code #define} and {@code #undef}.
 * It maintains a map of defined macros and performs substitutions when
 * identifiers are encountered.</p>
 */
public final class Preprocessor implements LexicalParser {

    private final LexicalParser parser;
    private final Map<String, List<Token>> macros = new HashMap<>();
    private final LinkedList<Token> tokenQueue = new LinkedList<>();

    /**
     * Creates a new instance.
     * @param parser The lexical parser to wrap.
     */
    public Preprocessor(final LexicalParser parser) {
        this.parser = parser;
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
        if (!tokenQueue.isEmpty()) {
            return Optional.of(tokenQueue.removeFirst());
        }

        var nextToken = parser.next();
        if (nextToken.isEmpty()) {
            return Optional.empty();
        }

        var token = nextToken.get();

        if (token.getType() == TokenType.IDENTIFIER) {
            var name = token.getValue();
            if (macros.containsKey(name)) {
                tokenQueue.addAll(macros.get(name));
                return next();
            }
        }

        if (token.getType() == TokenType.DIRECTIVE) {
            updateMacrosFromDirective(token);
        }

        return Optional.of(token);
    }

    private void updateMacrosFromDirective(final Token token) {
        var children = token.getChildren();
        if (children.isEmpty() || children.get(0).getType() != TokenType.DIRECTIVE_NAME) {
            return;
        }

        var directiveName = children.get(0).getValue();
        if ("define".equals(directiveName)) {
            handleDefine(children);
        } else if ("undef".equals(directiveName)) {
            handleUndef(children);
        }
    }

    private void handleDefine(final List<Token> directiveTokens) {
        int nameIndex = findFirstTokenAfter(0, directiveTokens);
        if (nameIndex == -1) {
            return;
        }

        var macroName = directiveTokens.get(nameIndex).getValue();
        int bodyIndex = findNextTokenAfter(nameIndex, directiveTokens);
        if (bodyIndex == -1) {
            macros.put(macroName, new ArrayList<>());
            return;
        }

        var macroBody = new ArrayList<Token>();
        for (int i = bodyIndex; i < directiveTokens.size(); i++) {
            var token = directiveTokens.get(i);
            if (token.getType() == TokenType.DIRECTIVE_END) {
                break;
            }
            macroBody.add(token);
        }

        macros.put(macroName, macroBody);
    }

    private int findNextTokenAfter(final int startIndex, final List<Token> tokens) {
        int index = startIndex + 1;
        while (index < tokens.size()
                && tokens.get(index).getType() == TokenType.DELIMITER) {
            index++;
        }
        if (index >= tokens.size()
                || tokens.get(index).getType() == TokenType.DIRECTIVE_END) {
            return -1;
        }
        return index;
    }

    private void handleUndef(final List<Token> directiveTokens) {
        int nameIndex = findFirstTokenAfter(0, directiveTokens);
        if (nameIndex != -1) {
            var macroName = directiveTokens.get(nameIndex).getValue();
            macros.remove(macroName);
        }
    }

    private int findFirstTokenAfter(final int startIndex, final List<Token> tokens) {
        int index = startIndex + 1;
        while (index < tokens.size()
                && tokens.get(index).getType() == TokenType.DELIMITER) {
            index++;
        }
        if (index >= tokens.size()
                || tokens.get(index).getType() != TokenType.IDENTIFIER) {
            return -1;
        }
        return index;
    }
}
