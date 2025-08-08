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
    private final Map<String, Macro> macros = new HashMap<>();
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
                var macro = macros.get(name);
                if (macro.isFunctionLike()) {
                    // It is a function-like macro.
                    // We must look ahead for a '('.
                    var openParen = lookAheadForParen();
                    if (openParen.isPresent()) {
                        var args = parseArguments();
                        var substituted = substitute(macro, args);
                        tokenQueue.addAll(substituted);
                        return next();
                    }
                } else {
                    // It is an object-like macro.
                    tokenQueue.addAll(macro.body());
                    return next();
                }
            }
        }

        if (token.getType() == TokenType.DIRECTIVE) {
            updateMacrosFromDirective(token);
        }

        return Optional.of(token);
    }

    private List<Token> substitute(final Macro macro, final List<List<Token>> args) {
        var params = macro.parameters();
        if (params.size() != args.size()) {
            // C99 standard says this is a constraint violation.
            // We'll just skip substitution.
            return List.of();
        }
        var mapping = new HashMap<String, List<Token>>();
        for (int i = 0; i < params.size(); i++) {
            mapping.put(params.get(i), args.get(i));
        }

        var result = new ArrayList<Token>();
        for (var token : macro.body()) {
            if (token.getType() == TokenType.IDENTIFIER && mapping.containsKey(token.getValue())) {
                result.addAll(mapping.get(token.getValue()));
            } else {
                result.add(token);
            }
        }
        return result;
    }

    private List<List<Token>> parseArguments() throws IOException {
        var args = new ArrayList<List<Token>>();
        var currentArg = new ArrayList<Token>();
        var parenLevel = 1;

        while (true) {
            var nextToken = parser.next();
            if (nextToken.isEmpty()) {
                break; // Unexpected EOF
            }
            var token = nextToken.get();

            if (token.getType() == TokenType.PUNCTUATOR && token.getValue().equals(")")) {
                parenLevel--;
                if (parenLevel == 0) {
                    if (!args.isEmpty() || !currentArg.isEmpty()) {
                        args.add(currentArg);
                    }
                    break;
                }
            } else if (token.getType() == TokenType.PUNCTUATOR && token.getValue().equals("(")) {
                parenLevel++;
            } else if (token.getType() == TokenType.PUNCTUATOR && token.getValue().equals(",") && parenLevel == 1) {
                args.add(currentArg);
                currentArg = new ArrayList<>();
                continue;
            }

            currentArg.add(token);
        }
        return args;
    }

    private Optional<Token> lookAheadForParen() throws IOException {
        var nextToken = parser.next();
        if (nextToken.isEmpty()) {
            return Optional.empty();
        }
        var token = nextToken.get();
        if (token.getType() == TokenType.PUNCTUATOR && token.getValue().equals("(")) {
            return Optional.of(token);
        }
        // This was not a function call, so put the token back.
        tokenQueue.add(token);
        return Optional.empty();
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
        var nameToken = directiveTokens.get(nameIndex);

        // Check for function-like macro. There must be no space
        // between the macro name and the '('.
        var nextTokenIndex = nameIndex + 1;
        if (nextTokenIndex < directiveTokens.size()) {
            var nextToken = directiveTokens.get(nextTokenIndex);
            if (nextToken.getType() == TokenType.PUNCTUATOR && nextToken.getValue().equals("(")) {
                // Function-like macro
                parseFunctionLikeMacro(directiveTokens, nameIndex);
                return;
            }
        }

        // Object-like macro
        int bodyIndex = findNextTokenAfter(nameIndex, directiveTokens);
        List<Token> body;
        if (bodyIndex == -1) {
            body = new ArrayList<>();
        } else {
            body = getMacroBody(bodyIndex, directiveTokens);
        }
        macros.put(macroName, new Macro(macroName, false, List.of(), body));
    }

    private void parseFunctionLikeMacro(final List<Token> tokens, final int nameIndex) {
        var macroName = tokens.get(nameIndex).getValue();
        var parameters = new ArrayList<String>();
        var currentIndex = nameIndex + 2; // After macro name and '('

        // Parse parameters
        while (currentIndex < tokens.size()) {
            var token = tokens.get(currentIndex);
            if (token.getType() == TokenType.PUNCTUATOR && token.getValue().equals(")")) {
                break;
            }
            if (token.getType() == TokenType.IDENTIFIER) {
                parameters.add(token.getValue());
            }
            // We ignore commas and other tokens between identifiers for simplicity.
            currentIndex++;
        }

        // Find body
        var bodyIndex = findNextTokenAfter(currentIndex, tokens);
        List<Token> body;
        if (bodyIndex == -1) {
            body = new ArrayList<>();
        } else {
            body = getMacroBody(bodyIndex, tokens);
        }

        macros.put(macroName, new Macro(macroName, true, parameters, body));
    }

    private List<Token> getMacroBody(int bodyIndex, final List<Token> tokens) {
        var macroBody = new ArrayList<Token>();
        for (int i = bodyIndex; i < tokens.size(); i++) {
            var token = tokens.get(i);
            if (token.getType() == TokenType.DIRECTIVE_END) {
                break;
            }
            macroBody.add(token);
        }
        return macroBody;
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
