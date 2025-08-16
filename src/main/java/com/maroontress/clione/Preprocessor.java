package com.maroontress.clione;

import com.maroontress.clione.impl.ReaderSource;
import com.maroontress.clione.impl.SourceChars;
import com.maroontress.clione.impl.TokenBuilder;
import com.maroontress.clione.impl.Transcriber;
import com.maroontress.clione.macro.CircularMacroException;
import com.maroontress.clione.macro.FunctionLikeMacro;
import com.maroontress.clione.macro.InvalidPreprocessingTokenException;
import com.maroontress.clione.macro.InvalidVariadicArgumentException;
import com.maroontress.clione.macro.Macro;
import com.maroontress.clione.macro.MissingCommaException;
import com.maroontress.clione.macro.MissingIdentifierException;
import com.maroontress.clione.macro.MissingParenException;
import com.maroontress.clione.macro.ObjectLikeMacro;
import com.maroontress.clione.macro.PreprocessException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
// CHECKSTYLE:OFF ClassDataAbstractionCoupling
public final class Preprocessor implements LexicalParser {

    private enum ParameterParseState {
        EXPECT_IDENTIFIER,
        EXPECT_COMMA_OR_PAREN,
    }

    private final Map<String, Token> expandingMacros = new LinkedHashMap<>();
    private final Map<String, Macro> macros = new HashMap<>();
    private final LexicalParser parser;
    private final LinkedList<PreprocessToken> tokenQueue = new LinkedList<>();

    /**
        Creates a new instance.

        @param parser The lexical parser to wrap.
    */
    public Preprocessor(LexicalParser parser) {
        this.parser = parser;
    }

    public Map<String, Token> getExpandingMacros() {
        return expandingMacros;
    }

    public LinkedList<PreprocessToken> getTokenQueue() {
        return tokenQueue;
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
        while (true) {
            var nextPreprocessToken = nextPreprocessTokenFromAnywhere();
            if (nextPreprocessToken.isEmpty()) {
                return Optional.empty();
            }

            var preprocessToken = nextPreprocessToken.get();
            var result = preprocessToken.handle(this);
            if (result.isPresent()) {
                return result;
            }
        }
    }

    @Override
    public Set<String> getReservedWords() {
        return parser.getReservedWords();
    }

    private Optional<PreprocessToken> nextPreprocessTokenFromAnywhere()
            throws IOException {
        if (!tokenQueue.isEmpty()) {
            return Optional.of(tokenQueue.removeFirst());
        }
        return parser.next().map(TokenWrapper::new);
    }

    public void prependTokens(final List<Token> tokens) {
        for (var i = tokens.size() - 1; i >= 0; --i) {
            tokenQueue.addFirst(new TokenWrapper(tokens.get(i)));
        }
    }

    private static Token stringize(final List<Token> tokens, final Token context) {
        var builder = new TokenBuilder();
        var quote = '"';

        if (tokens.isEmpty()) {
            var loc = context.getSpan().getStart();
            builder.append(SourceChars.of(quote, loc.getLine(), loc.getColumn()));
            builder.append(SourceChars.of(quote, loc.getLine(), loc.getColumn() + 1));
        } else {
            var firstToken = tokens.get(0);
            var lastToken = tokens.get(tokens.size() - 1);
            var startLoc = firstToken.getSpan().getStart();
            var endLoc = lastToken.getSpan().getEnd();

            builder.append(SourceChars.of(quote, startLoc.getLine(),
                        startLoc.getColumn()));

            for (var token : tokens) {
                for (var sourceChar : token.getChars()) {
                    var c = sourceChar.toChar();
                    if (c == '\\' || c == '"') {
                        var loc = sourceChar.getSpan().getStart();
                        builder.append(SourceChars.of('\\',
                                    loc.getLine(),
                                    loc.getColumn()));
                    }
                    builder.append(sourceChar);
                }
            }

            builder.append(SourceChars.of(quote, endLoc.getLine(),
                        endLoc.getColumn()));
        }
        return builder.toToken(TokenType.STRING);
    }

    private Token concatenate(Token left, Token right)
            throws InvalidPreprocessingTokenException {
        var builder = new TokenBuilder();
        for (var sourceChar : left.getChars()) {
            builder.append(sourceChar);
        }
        for (var sourceChar : right.getChars()) {
            builder.append(sourceChar);
        }

        var tokenString = builder.toTokenString();
        var type = getTokenType(tokenString);
        if (type == null) {
            var list = List.copyOf(expandingMacros.values());
            throw new InvalidPreprocessingTokenException(tokenString, list);
        }
        return builder.toToken((type == TokenType.IDENTIFIER
                && getReservedWords().contains(tokenString))
            ? TokenType.RESERVED
            : type);
    }

    private static TokenType getTokenType(String tokenString) {
        var source = new ReaderSource(new StringReader(tokenString));
        var x = new Transcriber(source);
        try {
            var type = x.readToken();
            if (type == null || type == TokenType.DIRECTIVE) {
                return null;
            }
            var nextType = x.readToken();
            if (nextType != null) {
                return null;
            }
            return type;
        } catch (IOException e) {
            // This should not happen with StringReader.
            return null;
        }
    }

    public List<Token> substitute(Macro macro, Token token, List<List<Token>> args)
            throws PreprocessException {
        var mapping = macro.getSubstitutionMapping(args, this);
        var substituted = substituteParamsAndStringify(macro.body(), mapping);
        return concatenateTokens(substituted);
    }

    private List<Token> substituteParamsAndStringify(List<Token> body,
            Map<String, List<Token>> mapping) throws PreprocessException {
        var substituted = new ArrayList<Token>();
        var i = 0;
        while (i < body.size()) {
            var currentToken = body.get(i);
            var currentValue = currentToken.getValue();
            var currentTokenType = currentToken.getType();
            if (currentTokenType == TokenType.OPERATOR
                    && "#".equals(currentValue)) {
                if (i + 1 < body.size()) {
                    var nextToken = body.get(i + 1);
                    var nextValue = nextToken.getValue();
                    if (nextToken.getType() == TokenType.IDENTIFIER
                            && mapping.containsKey(nextValue)) {
                        var argTokens = mapping.get(nextValue);
                        substituted.add(stringize(argTokens, currentToken));
                        // consume parameter
                        i++;
                    } else {
                        substituted.add(currentToken);
                    }
                } else {
                    substituted.add(currentToken);
                }
            } else if (currentToken.getType() == TokenType.IDENTIFIER) {
                substituteIdentifier(currentToken, mapping, substituted);
            } else {
                substituted.add(currentToken);
            }
            i++;
        }
        return substituted;
    }

    private void substituteIdentifier(Token token,
            Map<String, List<Token>> mapping,
            List<Token> substituted) throws PreprocessException {
        var tokenValue = token.getValue();
        if ("__VA_ARGS__".equals(tokenValue)) {
            substituteVaArgs(token, mapping, substituted);
            return;
        }
        var value = mapping.get(tokenValue);
        if (value == null) {
            substituted.add(token);
            return;
        }
        substituted.addAll(value);
    }

    private void substituteVaArgs(Token token, Map<String, List<Token>> mapping,
            List<Token> substituted) throws PreprocessException {
        var vaTokens = mapping.get("__VA_ARGS__");
        if (!vaTokens.isEmpty()) {
            substituted.addAll(vaTokens);
            return;
        }
        var n = substituted.size() - 1;
        for (var k = n; k >= 0; --k) {
            var t = substituted.get(k);
            var type = t.getType();
            if (type == TokenType.DELIMITER) {
                continue;
            }
            if (type == TokenType.PUNCTUATOR && ",".equals(t.getValue())) {
                var list = List.copyOf(expandingMacros.values());
                throw new InvalidVariadicArgumentException(
                    "empty __VA_ARGS__ with preceding comma", list, t);
            }
            break;
        }
    }

    private List<Token> concatenateTokens(final List<Token> tokens) throws PreprocessException {
        var result = new ArrayList<Token>();
        int i = 0;
        while (i < tokens.size()) {
            var currentToken = tokens.get(i);
            if (currentToken.getType() == TokenType.OPERATOR
                    && "##".equals(currentToken.getValue())) {
                Token left = null;
                int leftIndex = -1;
                for (int j = result.size() - 1; j >= 0; j--) {
                    if (result.get(j).getType() != TokenType.DELIMITER) {
                        left = result.get(j);
                        leftIndex = j;
                        break;
                    }
                }

                Token right = null;
                int rightIndex = -1;
                for (int j = i + 1; j < tokens.size(); j++) {
                    if (tokens.get(j).getType() != TokenType.DELIMITER) {
                        right = tokens.get(j);
                        rightIndex = j;
                        break;
                    }
                }

                if (left != null && right != null) {
                    result.subList(leftIndex, result.size()).clear();
                    result.add(concatenate(left, right));
                    i = rightIndex;
                }
            } else {
                result.add(currentToken);
            }
            i++;
        }
        return result;
    }

    public Optional<Token> lookAheadForParen() throws IOException {
        var peeked = new ArrayList<PreprocessToken>();

        while (true) {
            var nextOpt = nextPreprocessTokenFromAnywhere();
            if (nextOpt.isEmpty()) {
                // EOF, not found. Put stuff back.
                for (int i = peeked.size() - 1; i >= 0; i--) {
                    tokenQueue.addFirst(peeked.get(i));
                }
                return Optional.empty();
            }

            var next = nextOpt.get();
            peeked.add(next);

            if (next instanceof TokenWrapper) {
                var token = ((TokenWrapper) next).getToken();
                var type = token.getType();

                if (type == TokenType.COMMENT || type == TokenType.DELIMITER) {
                    continue;
                }

                if (type == TokenType.PUNCTUATOR && token.getValue().equals("(")) {
                    return Optional.of(token);
                }
            }

            for (int i = peeked.size() - 1; i >= 0; i--) {
                tokenQueue.addFirst(peeked.get(i));
            }
            return Optional.empty();
        }
    }

    private void updateMacrosFromDirective(final Token token) throws IOException {
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

    private void handleDefine(final List<Token> directiveTokens) throws IOException {
        int nameIndex = findFirstTokenAfter(0, directiveTokens);
        if (nameIndex == -1) {
            return;
        }

        var macroName = directiveTokens.get(nameIndex).getValue();

        var nextTokenIndex = nameIndex + 1;
        if (nextTokenIndex < directiveTokens.size()) {
            var nextToken = directiveTokens.get(nextTokenIndex);
            if (nextToken.getType() == TokenType.PUNCTUATOR && nextToken.getValue().equals("(")) {
                parseFunctionLikeMacro(directiveTokens, nameIndex);
                return;
            }
        }

        int bodyIndex = findNextTokenAfter(nameIndex, directiveTokens);
        List<Token> body;
        if (bodyIndex == -1) {
            body = new ArrayList<>();
        } else {
            body = getMacroBody(bodyIndex, directiveTokens);
        }
        macros.put(macroName, new ObjectLikeMacro(macroName, body));
    }

    // CHECKSTYLE:OFF CyclomaticComplexity
    private void parseFunctionLikeMacro(final List<Token> tokens, final int nameIndex)
            throws PreprocessException {
        var macroName = tokens.get(nameIndex).getValue();
        var parameters = new ArrayList<String>();
        var currentIndex = nameIndex + 2;
        var isVariadic = false;
        var closingParenFound = false;
        Token lastToken = tokens.get(nameIndex);

        var state = ParameterParseState.EXPECT_IDENTIFIER;
        var firstParam = true;

        while (currentIndex < tokens.size()) {
            var token = tokens.get(currentIndex);
            var tokenType = token.getType();
            if (tokenType == TokenType.DELIMITER) {
                currentIndex++;
                continue;
            }
            lastToken = token;

            if (tokenType == TokenType.DIRECTIVE_END) {
                break;
            }
            var tokenValue = token.getValue();

            if (tokenType == TokenType.PUNCTUATOR && ")".equals(tokenValue)) {
                if (state == ParameterParseState.EXPECT_IDENTIFIER && !firstParam) {
                    throw new MissingIdentifierException(token);
                }
                closingParenFound = true;
                break;
            }

            if (tokenType == TokenType.PUNCTUATOR && "...".equals(tokenValue)) {
                if (state == ParameterParseState.EXPECT_COMMA_OR_PAREN) {
                    throw new MissingCommaException(token);
                }
                isVariadic = true;
                currentIndex++;
                while (currentIndex < tokens.size()) {
                    token = tokens.get(currentIndex);
                    if (token.getType() == TokenType.PUNCTUATOR && ")".equals(token.getValue())) {
                        closingParenFound = true;
                        break;
                    }
                    if (token.getType() != TokenType.DELIMITER) {
                        throw new MissingParenException(token);
                    }
                    currentIndex++;
                }
                break;
            }

            switch (state) {
                case EXPECT_IDENTIFIER:
                    if (tokenType == TokenType.IDENTIFIER) {
                        parameters.add(tokenValue);
                        state = ParameterParseState.EXPECT_COMMA_OR_PAREN;
                        firstParam = false;
                    } else {
                        throw new MissingIdentifierException(token);
                    }
                    break;
                case EXPECT_COMMA_OR_PAREN:
                    if (tokenType == TokenType.PUNCTUATOR && ",".equals(tokenValue)) {
                        state = ParameterParseState.EXPECT_IDENTIFIER;
                    } else {
                        throw new MissingCommaException(token);
                    }
                    break;
            }
            currentIndex++;
        }

        if (!closingParenFound) {
            throw new MissingParenException(lastToken);
        }

        var bodyIndex = findNextTokenAfter(currentIndex, tokens);
        List<Token> body;
        if (bodyIndex == -1) {
            body = new ArrayList<>();
        } else {
            body = getMacroBody(bodyIndex, tokens);
        }

        macros.put(macroName, new FunctionLikeMacro(macroName, parameters, isVariadic, body));
    }
    // CHECKSTYLE:ON CyclomaticComplexity

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

    public interface PreprocessToken {
        Optional<Token> handle(Preprocessor preprocessor) throws IOException;
    }

    public static final class TokenWrapper implements PreprocessToken {
        private final Token token;

        TokenWrapper(Token token) {
            this.token = token;
        }

        public Token getToken() {
            return token;
        }

        // このクラスはIDENTIFIER, DIRECTIVE, それ以外で分けた方が良い

        @Override
        public Optional<Token> handle(Preprocessor preprocessor) throws IOException {
            var type = token.getType();
            if (type == TokenType.IDENTIFIER) {
                return handleIdentifier(preprocessor);
            }
            if (type == TokenType.DIRECTIVE) {
                preprocessor.updateMacrosFromDirective(token);
            }
            return Optional.of(token);
        }

        private Optional<Token> handleIdentifier(Preprocessor preprocessor)
                throws IOException {
            var name = token.getValue();
            var macros = preprocessor.macros;
            var m = macros.get(name);
            if (m == null) {
                return Optional.of(token);
            }
            var expandingMacros = preprocessor.expandingMacros;
            if (expandingMacros.containsKey(name)) {
                var expandingTokens = List.copyOf(expandingMacros.values());
                throw new CircularMacroException(name, expandingTokens);
            }
            return m.apply(preprocessor, token);
        }
    }

    public static final class MacroEndMarker implements PreprocessToken {
        private final String name;

        public MacroEndMarker(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Optional<Token> handle(Preprocessor preprocessor) {
            preprocessor.expandingMacros.remove(name);
            return Optional.empty();
        }
    }
}
