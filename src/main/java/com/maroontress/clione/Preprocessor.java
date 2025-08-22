package com.maroontress.clione;

import com.maroontress.clione.macro.FunctionLikeMacro;
import com.maroontress.clione.macro.InvalidConcatenationOperatorException;
import com.maroontress.clione.macro.InvalidPreprocessingTokenException;
import com.maroontress.clione.macro.InvalidStringizingOperatorException;
import com.maroontress.clione.macro.Macro;
import com.maroontress.clione.macro.MacroArgument;
import com.maroontress.clione.macro.MacroEndMarker;
import com.maroontress.clione.macro.MissingCommaException;
import com.maroontress.clione.macro.MissingIdentifierException;
import com.maroontress.clione.macro.MissingParenException;
import com.maroontress.clione.macro.ObjectLikeMacro;
import com.maroontress.clione.macro.ParameterOriginatedToken;
import com.maroontress.clione.macro.PreprocessException;
import com.maroontress.clione.macro.MacroToken;
import com.maroontress.clione.macro.TokenWrapper;
import com.maroontress.clione.macro.Tokens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
    The preprocessor that handles macro definitions and substitutions.

    <p>This class wraps a {@link LexicalParser} and intercepts the token stream
    to process preprocessor directives like {@code #define} and {@code #undef}.
    It maintains a map of defined macros and performs substitutions when
    identifiers are encountered.</p>
*/
// CHECKSTYLE:OFF ClassDataAbstractionCoupling
public final class Preprocessor implements LexicalParser {

    private final Map<String, Token> expandingMacros = new LinkedHashMap<>();
    private final Map<String, Macro> macros = new HashMap<>();
    private final LexicalParser parser;
    private LinkedList<MacroToken> tokenQueue = new LinkedList<>();

    /**
        Creates a new instance.

        @param parser The lexical parser to wrap.
    */
    public Preprocessor(LexicalParser parser) {
        this.parser = parser;
    }

    /**
        Returns the map of macros that are currently being expanded.

        @return The map of expanding macros.
    */
    public Map<String, Token> getExpandingMacros() {
        return expandingMacros;
    }

    /**
        Returns the queue of preprocessor tokens.

        @return The queue of preprocessor tokens.
    */
    public LinkedList<MacroToken> getTokenQueue() {
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
        for (;;) {
            var maybeMacroToken = nextMacroToken();
            if (maybeMacroToken.isEmpty()) {
                return Optional.empty();
            }

            var macroToken = maybeMacroToken.get();
            macroToken.getMacroEndName()
                .ifPresent(this::handleEndMarker);
            var maybeToken = macroToken.getToken();
            var result = handleToken(maybeToken);
            if (result.isPresent()) {
                return result;
            }
        }
    }

    private Optional<Token> handleToken(Optional<Token> maybeToken)
            throws IOException {
        if (!maybeToken.isPresent()) {
            return maybeToken;
        }
        var token = maybeToken.get();
        var type = token.getType();
        if (type == TokenType.IDENTIFIER) {
            return handleIdentifier(token);
        }
        if (type == TokenType.DIRECTIVE) {
            updateMacrosFromDirective(token);
        }
        return maybeToken;
    }

    @Override
    public Set<String> getReservedWords() {
        return parser.getReservedWords();
    }

    /**
        Returns the next macro token from the preprocessor stream.

        <p>This method first checks the internal token queue, which may
        contain tokens from a macro expansion. If the queue is empty, it
        fetches the next token from the underlying lexical parser.</p>

        @return An optional containing the next macro token, or an empty
        optional if the end of the stream is reached.
        @throws IOException If an I/O error occurs.
    */
    public Optional<MacroToken> nextMacroToken() throws IOException {
        if (!tokenQueue.isEmpty()) {
            return Optional.of(tokenQueue.removeFirst());
        }
        return parser.next().map(TokenWrapper::new);
    }

    /**
        Expands an object-like macro.

        @param macro The macro to be expanded.
        @param token The token that triggered the macro expansion.
        @return An empty optional.
        @throws PreprocessException If an error occurs during preprocessing.
    */
    public Optional<Token> expandObjectBasedMacro(Macro macro, Token token)
            throws PreprocessException {
        return expandMacro(macro, token, () -> macro.body());
    }

    /**
        Expands a function-like macro.

        @param macro The macro to be expanded.
        @param token The token that triggered the macro expansion.
        @param args The arguments of the macro invocation.
        @return An empty optional.
        @throws PreprocessException If an error occurs during preprocessing.
    */
    public Optional<Token> expandFunctionBasedMacro(Macro macro, Token token,
            MacroArgument args)
            throws PreprocessException {
        return expandMacro(macro, token, () -> substitute(macro, token, args));
    }

    private Optional<Token> expandMacro(
            Macro macro, Token token, BodySupplier supplier)
            throws PreprocessException {
        var name = macro.name();
        expandingMacros.put(name, token);
        tokenQueue.addFirst(new MacroEndMarker(name));
        prependTokens(supplier.get());
        return Optional.empty();
    }

    /**
        Prepends a list of tokens to the token queue.

        @param tokens The list of tokens to prepend.
    */
    private void prependTokens(List<Token> tokens) {
        for (var i = tokens.size() - 1; i >= 0; --i) {
            tokenQueue.addFirst(new TokenWrapper(tokens.get(i)));
        }
    }

    /**
        Substitutes macro parameters and returns the resulting list of tokens.

        <p>This method performs the following substitutions:</p>
        <ul>
            <li>Parameter substitution</li>
            <li>Stringification using the '#' operator</li>
            <li>Token concatenation using the '##' operator</li>
        </ul>

        @param macro The macro to substitute.
        @param token The token that triggered the macro expansion.
        @param args The list of macro arguments.
        @return The list of tokens after substitution.
        @throws PreprocessException if an error occurs during preprocessing.
    */
    public List<Token> substitute(Macro macro, Token token, MacroArgument args)
            throws PreprocessException {
        var mapping = macro.getSubstitutionMapping(args, this);
        var substituted = substituteParamsAndStringify(macro, mapping);
        var concatenated = concatenateTokens(substituted);
        var expanded = expandMarkedTokens(concatenated, macro, token);

        var result = new ArrayList<Token>();
        expanded.forEach(mt -> mt.getToken().ifPresent(result::add));
        return result;
    }

    private List<MacroToken> expandMarkedTokens(List<MacroToken> tokens,
            Macro parentMacro, Token parentToken) throws PreprocessException {
        var result = new ArrayList<MacroToken>();
        var worklist = new LinkedList<>(tokens);

        while (!worklist.isEmpty()) {
            var current = worklist.removeFirst();
            if (current instanceof MacroEndMarker) {
                current.getMacroEndName().ifPresent(expandingMacros::remove);
                continue;
            }

            var maybeToken = current.getToken();
            if (maybeToken.isEmpty()) {
                continue;
            }
            var token = maybeToken.get();

            if (!current.isOriginatingFromParameter()
                    || token.getType() != TokenType.IDENTIFIER) {
                result.add(current);
                continue;
            }

            var name = token.getValue();
            var macro = macros.get(name);

            if (macro == null || expandingMacros.containsKey(name)) {
                result.add(current);
                continue;
            }

            if (macro instanceof FunctionLikeMacro) {
                var openParenOpt = lookAheadForParen(worklist);
                if (openParenOpt.isEmpty()) {
                    result.add(current);
                    continue;
                }
                var openParen = openParenOpt.get();
                // We need to find the opening parenthesis and remove it.
                // The lookAheadForParen just peeks.
                while (!worklist.isEmpty()) {
                    var t = worklist.removeFirst();
                    if (t.getToken().isPresent()
                            && t.getToken().get().equals(openParen)) {
                        break;
                    }
                }

                var fnMacro = (FunctionLikeMacro) macro;
                var builder = fnMacro.newArgumentBuilder(openParen);

                while (!worklist.isEmpty()) {
                    var next = worklist.removeFirst();
                    var nextToken = next.getToken().orElse(null);
                    if (nextToken != null && builder.addToken(nextToken)) {
                        break; // Found closing paren
                    }
                }

                var args = builder.build();
                var substituted = substitute(macro, token, args);
                expandingMacros.put(name, token);
                worklist.addFirst(new MacroEndMarker(name));
                for (var i = substituted.size() - 1; i >= 0; --i) {
                    worklist.addFirst(
                        new ParameterOriginatedToken(substituted.get(i)));
                }
            } else {
                // expand the object-like macro
                expandingMacros.put(name, token);
                worklist.addFirst(new MacroEndMarker(name));
                var body = macro.body();
                for (var i = body.size() - 1; i >= 0; --i) {
                    worklist.addFirst(
                        new ParameterOriginatedToken(body.get(i)));
                }
            }
        }

        return result;
    }

    private List<MacroToken> substituteParamsAndStringify(Macro macro,
            Map<String, List<Token>> mapping) throws PreprocessException {
        var body = macro.body();
        var substituted = new ArrayList<MacroToken>();
        var i = 0;
        while (i < body.size()) {
            var currentToken = body.get(i);
            if (Tokens.isStringizingOperator(currentToken)) {
                var nextTokenIndex = i + 1;
                while (nextTokenIndex < body.size()
                        && Tokens.isDelimiterOrComment(body.get(nextTokenIndex))) {
                    nextTokenIndex++;
                }

                if (nextTokenIndex < body.size()) {
                    var nextToken = body.get(nextTokenIndex);
                    var nextValue = nextToken.getValue();
                    if (nextToken.getType() == TokenType.IDENTIFIER
                            && mapping.containsKey(nextValue)) {
                        var argTokens = mapping.get(nextValue);
                        var stringized = Tokens.stringize(argTokens,
                                currentToken.getSpan());
                        substituted.add(new TokenWrapper(stringized));
                        i = nextTokenIndex;
                    } else {
                        substituted.add(new TokenWrapper(currentToken));
                    }
                } else {
                    substituted.add(new TokenWrapper(currentToken));
                }
            } else if (currentToken.getType() == TokenType.IDENTIFIER) {
                substituteIdentifier(currentToken, mapping, substituted);
            } else {
                substituted.add(new TokenWrapper(currentToken));
            }
            i++;
        }
        return substituted;
    }

    private void substituteIdentifier(Token token,
            Map<String, List<Token>> mapping,
            List<MacroToken> substituted) throws PreprocessException {
        var tokenValue = token.getValue();
        if ("__VA_ARGS__".equals(tokenValue)) {
            // ここはポリモーフィズムを適用すべき
            var vaArgs = mapping.get("__VA_ARGS__");
            if (vaArgs != null) {
                vaArgs.forEach(
                    t -> substituted.add(new ParameterOriginatedToken(t)));
            }
            return;
        }
        var value = mapping.get(tokenValue);
        if (value == null) {
            substituted.add(new TokenWrapper(token));
            return;
        }
        value.forEach(t -> substituted.add(new ParameterOriginatedToken(t)));
    }

    private List<MacroToken> concatenateTokens(List<MacroToken> tokens)
            throws PreprocessException {
        if (tokens.isEmpty()) {
            return tokens;
        }

        var result = new ArrayList<MacroToken>();
        var i = 0;
        while (i < tokens.size()) {
            var currentMacroToken = tokens.get(i);
            var currentToken = currentMacroToken.getToken().get();
            if (Tokens.isConcatenatingOperator(currentToken)) {
                MacroToken left = null;
                int leftIndex = -1;
                for (var j = result.size() - 1; j >= 0; j--) {
                    if (!Tokens.isDelimiterOrComment(result.get(j).getToken().get())) {
                        left = result.get(j);
                        leftIndex = j;
                        break;
                    }
                }

                MacroToken right = null;
                int rightIndex = -1;
                for (var j = i + 1; j < tokens.size(); j++) {
                    if (!Tokens.isDelimiterOrComment(tokens.get(j).getToken().get())) {
                        right = tokens.get(j);
                        rightIndex = j;
                        break;
                    }
                }

                if (left != null && right != null) {
                    result.subList(leftIndex, result.size()).clear();
                    var concatenated = Tokens.concatenate(
                        left.getToken().get(),
                        right.getToken().get(),
                        getReservedWords());
                    if (concatenated.getType() == TokenType.UNKNOWN) {
                        throw new InvalidPreprocessingTokenException(
                            concatenated.getValue(),
                            List.copyOf(expandingMacros.values()));
                    }
                    result.add(new TokenWrapper(concatenated));
                    i = rightIndex;
                }
            } else {
                result.add(currentMacroToken);
            }
            i++;
        }
        return result;
    }

    private static Optional<Token> lookAheadForParen(List<MacroToken> list) {
        // Streamで書き直せるね
        var iter = list.iterator();
        while (iter.hasNext()) {
            var macroToken = iter.next();
            var maybeToken = macroToken.getToken();
            if (maybeToken.isEmpty()) {
                continue;
            }
            var token = maybeToken.get();
            if (Tokens.isDelimiterOrComment(token)) {
                continue;
            }
            if (Tokens.isOpenParenthesis(token)) {
                return Optional.of(token);
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
        Looks ahead for a parenthesis in the token stream.

        <p>This method peeks at the token stream to check if a parenthesis
        follows the current position, skipping comments and delimiters.
        If a parenthesis is found, it is returned, and the peeked tokens are
        left in the queue. Otherwise, an empty optional is returned, and the
        peeked tokens are put back into the queue.</p>

        @return An optional containing the parenthesis token if found, or an
        empty optional otherwise.
        @throws IOException if an I/O error occurs.
    */
    public Optional<Token> lookAheadForParen() throws IOException {
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

            var nextToken = next.getToken();
            if (nextToken.isPresent()) {
                var token = nextToken.get();
                if (Tokens.isDelimiterOrComment(token)) {
                    continue;
                }
                if (Tokens.isOpenParenthesis(token)) {
                    return Optional.of(token);
                }
            }

            for (var k = peeked.size() - 1; k >= 0; --k) {
                tokenQueue.addFirst(peeked.get(k));
            }
            return Optional.empty();
        }
    }

    /**
        Updates the macro map from a directive token.

        @param token The directive token.
        @throws IOException If an I/O error occurs.
    */
    public void updateMacrosFromDirective(Token token) throws IOException {
        var children = token.getChildren();
        if (children.isEmpty()) {
            return;
        }

        int directiveNameIndex = -1;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getType() == TokenType.DIRECTIVE_NAME) {
                directiveNameIndex = i;
                break;
            }
        }

        if (directiveNameIndex == -1) {
            return;
        }

        var directiveName = children.get(directiveNameIndex).getValue();
        if ("define".equals(directiveName)) {
            handleDefine(children, directiveNameIndex);
        } else if ("undef".equals(directiveName)) {
            handleUndef(children, directiveNameIndex);
        }
    }

    private void handleDefine(List<Token> directiveTokens, int directiveNameIndex) throws IOException {
        int nameIndex = findFirstIdentifierAfter(directiveNameIndex, directiveTokens);
        if (nameIndex == -1) {
            return;
        }

        var macroName = directiveTokens.get(nameIndex).getValue();

        var nextTokenIndex = nameIndex + 1;
        if (nextTokenIndex < directiveTokens.size()) {
            var nextToken = directiveTokens.get(nextTokenIndex);
            if (Tokens.isOpenParenthesis(nextToken)) {
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
    private void parseFunctionLikeMacro(List<Token> tokens, int nameIndex)
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
            if (Tokens.isDelimiterOrComment(token)) {
                ++currentIndex;
                continue;
            }
            lastToken = token;

            if (tokenType == TokenType.DIRECTIVE_END) {
                break;
            }
            var tokenValue = token.getValue();

            if (Tokens.isCloseParenthesis(token)) {
                if (state == ParameterParseState.EXPECT_IDENTIFIER && !firstParam) {
                    throw new MissingIdentifierException(token);
                }
                closingParenFound = true;
                break;
            }

            if (Tokens.isElipsis(token)) {
                if (state == ParameterParseState.EXPECT_COMMA_OR_PAREN) {
                    throw new MissingCommaException(token);
                }
                isVariadic = true;
                currentIndex++;
                while (currentIndex < tokens.size()) {
                    token = tokens.get(currentIndex);
                    if (Tokens.isCloseParenthesis(token)) {
                        closingParenFound = true;
                        break;
                    }
                    if (!Tokens.isDelimiterOrComment(token)) {
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
                if (Tokens.isComma(lastToken)) {
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

        validateMacroBody(body, parameters, isVariadic);

        macros.put(macroName, new FunctionLikeMacro(macroName, parameters, isVariadic, body));
    }

    private void validateMacroBody(
            List<Token> body, List<String> parameters, boolean isVariadic)
            throws PreprocessException {
        if (!body.isEmpty()) {
            for (var token : body) {
                if (Tokens.isDelimiterOrComment(token)) {
                    continue;
                }
                if (Tokens.isConcatenatingOperator(token)) {
                    throw new InvalidConcatenationOperatorException(token, true);
                }
                break;
            }
            for (var i = body.size() - 1; i >= 0; --i) {
                var token = body.get(i);
                if (Tokens.isDelimiterOrComment(token)) {
                    continue;
                }
                if (Tokens.isConcatenatingOperator(token)) {
                    throw new InvalidConcatenationOperatorException(token, false);
                }
                break;
            }
        }
        validateStringizingOperators(body, parameters, isVariadic);
    }

    private void validateStringizingOperators(
            List<Token> body, List<String> parameters, boolean isVariadic)
                throws PreprocessException {
        var isValid = newStringizingOperandValidator(parameters, isVariadic);
        for (var i = 0; i < body.size(); ++i) {
            var token = body.get(i);
            if (!Tokens.isStringizingOperator(token)) {
                continue;
            }
            var nextTokenIndex = i + 1;
            while (nextTokenIndex < body.size()
                    && Tokens.isDelimiterOrComment(body.get(nextTokenIndex))) {
                ++nextTokenIndex;
            }
            if (nextTokenIndex >= body.size()) {
                throw new InvalidStringizingOperatorException(token);
            }
            var nextToken = body.get(nextTokenIndex);
            if (!isValid.test(nextToken)) {
                throw new InvalidStringizingOperatorException(
                        nextToken);
            }
        }
    }

    private Predicate<Token> newStringizingOperandValidator(
            List<String> parameters, boolean isVariadic) {
        return isVariadic
                ? token -> {
                    var value = token.getValue();
                    return token.getType() == TokenType.IDENTIFIER
                            && (parameters.contains(value)
                                    || "__VA_ARGS__".equals(value));
                }
                : token -> {
                    return token.getType() == TokenType.IDENTIFIER
                            && parameters.contains(token.getValue());
                };
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

        for (var i = macroBody.size() - 1; i >= 0; i--) {
            if (!Tokens.isDelimiterOrComment(macroBody.get(i))) {
                return macroBody.subList(0, i + 1);
            }
        }

        return new ArrayList<>();
    }

    private int findFirstIdentifierAfter(int startIndex, List<Token> tokens) {
        int index = startIndex + 1;
        while (index < tokens.size()
                && Tokens.isDelimiterOrComment(tokens.get(index))) {
            index++;
        }
        if (index >= tokens.size()
                || tokens.get(index).getType() != TokenType.IDENTIFIER) {
            return -1;
        }
        return index;
    }

    private int findNextTokenAfter(int startIndex, List<Token> tokens) {
        int index = startIndex + 1;
        while (index < tokens.size()
                && Tokens.isDelimiterOrComment(tokens.get(index))) {
            index++;
        }
        if (index >= tokens.size()
                || tokens.get(index).getType() == TokenType.DIRECTIVE_END) {
            return -1;
        }
        return index;
    }

    private void handleUndef(List<Token> directiveTokens, int directiveNameIndex) {
        int nameIndex = findFirstIdentifierAfter(directiveNameIndex, directiveTokens);
        if (nameIndex != -1) {
            var macroName = directiveTokens.get(nameIndex).getValue();
            macros.remove(macroName);
        }
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
        var m = macros.get(name);
        if (m == null) {
            return Optional.of(token);
        }
        if (expandingMacros.containsKey(name)) {
            return Optional.of(token);
        }
        return m.apply(this, token);
    }

    /**
        Handles a macro end marker.

        @param name The name of the macro.
    */
    public void handleEndMarker(String name) {
        expandingMacros.remove(name);
    }

    private enum ParameterParseState {
        EXPECT_IDENTIFIER,
        EXPECT_COMMA_OR_PAREN,
    }

    @FunctionalInterface
    private interface BodySupplier {
        List<Token> get() throws PreprocessException;
    }
}
// CHECKSTYLE:ON ClassDataAbstractionCoupling
