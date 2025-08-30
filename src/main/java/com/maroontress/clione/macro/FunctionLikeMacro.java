package com.maroontress.clione.macro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.Tokens;

/**
    Represents a function-like preprocessor macro.
*/
public final class FunctionLikeMacro implements Macro {

    private final String name;
    private final List<String> parameters;
    private final List<Token> body;
    private final FunctionLikeMacroBehavior behavior;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param parameters The collection of parameter names.
        @param body The collection of tokens that form the macro's body.
        @param behavior The macro behavior.
    */
    public FunctionLikeMacro(String name,
                             Collection<String> parameters,
                             Collection<Token> body,
                             FunctionLikeMacroBehavior behavior) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.body = List.copyOf(body);
        this.behavior = behavior;
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public List<Token> body() {
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Token> apply(ParseKit kit, Token token) throws IOException {
        var maybeArguments = parseArguments(
                kit, token, newUmiExceptionSupplier(kit.getKeeper(), token));
        return !maybeArguments.isPresent()
                ? Optional.of(token)
                : expand(kit, token, maybeArguments.get());
    }

    /**
        Returns the list of parameter names.

        @return The list of parameter names.
    */
    public List<String> parameters() {
        return parameters;
    }

    /**
        Creates a substitution mapping from parameter names to the given
        arguments.

        @param args The macro arguments.
        @param keeper The macro keeper
        @return A map from parameter names to token lists.
        @throws PreprocessException If the arguments are invalid.
    */
    public Map<String, List<Token>> getSubstitutionMapping(
            MacroArgument args, MacroKeeper keeper)
            throws PreprocessException {
        var argumentSize = args.size();
        for (var k = 0; k < argumentSize; ++k) {
            var tokenList = args.get(k);
            if (tokenList.isEmpty()) {
                continue;
            }
            var maybeFirst = tokenList.stream()
                    .filter(t -> t.isType(TokenType.DIRECTIVE))
                    .findFirst();
            if (maybeFirst.isPresent()) {
                throw new DirectiveWithinMacroArgumentsException(
                    maybeFirst.get(),
                    keeper.getExpandingChain());
            }
        }
        return behavior.getSubstitutionMapping(this, args, keeper);
    }

    private Optional<Token> expand(
            ParseKit kit, Token token, MacroArgument args) {
        return kit.expand(this, token, () -> substitute(token, args, kit));
    }

    private Optional<MacroArgument> parseArguments(
            ParseKit kit, Token macroName,
            Supplier<PreprocessException> supplier)
            throws IOException {
        var reservoir = kit.getReservoir();
        var maybeOpenParen = kit.lookAhead(TokenKit::isOpenParenthesis);
        // Use maybeOpenParen#map()
        if (!maybeOpenParen.isPresent()) {
            return Optional.empty();
        }
        var openParen = maybeOpenParen.get();
        var builder = behavior.createArgumentBuilder(kit, this, openParen);
        for (;;) {
            var maybeMacroToken = reservoir.nextMacroToken();
            if (maybeMacroToken.isEmpty()) {
                throw supplier.get();
            }
            var macroToken = maybeMacroToken.get();
            if (macroToken.addToArguments(builder, supplier)) {
                break;
            }
        }
        return Optional.of(builder.build());
    }

    private Supplier<PreprocessException> newUmiExceptionSupplier(
            MacroKeeper keeper, Token macroName) {
        return () -> {
            return new UnterminatedMacroInvocationException(
                macroName, keeper.getExpandingChain());
        };
    }

    /**
        Creates a new macro argument builder.

        @param openParen The opening parenthesis of the argument list.
        @return A new macro argument builder.
    */
    public MacroArgumentBuilder newArgumentBuilder(
            ParseKit kit, Token openParen) {
        return behavior.createArgumentBuilder(kit, this, openParen);
    }

    /**
        Returns the default substitution mapping from the given macro
        arguments.

        @param args The list of macro arguments.
        @param keeper The macro keeper
        @return The substitution mapping.
        @throws MacroArgumentException if the number of arguments is incorrect.
    */
    public Map<String, List<Token>> getDefaultSubstitutionMapping(
            MacroArgument args, MacroKeeper keeper)
            throws MacroArgumentException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize != actualSize) {
            var causeToken = expectedSize > actualSize
                    ? args.getCloseParen()
                    : args.getComma(expectedSize - 1);
            throw new MacroArgumentException(causeToken,
                    expectedSize, actualSize,
                    keeper.getExpandingChain());
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        return mapping;
    }

    /**
        Creates a substitution mapping for a variadic macro invocation.

        @param args The macro arguments.
        @param keeper The macro keeper
        @return A map from parameter names to token lists.
        @throws PreprocessException If the arguments are invalid.
    */
    public Map<String, List<Token>> getVariadicSubstitutionMapping(
            MacroArgument args, MacroKeeper keeper)
            throws PreprocessException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize > actualSize) {
            var closeParen = args.getCloseParen();
            throw new MacroArgumentException(closeParen, expectedSize,
                    actualSize, keeper.getExpandingChain());
        }
        if (params.size() > 0 && expectedSize == actualSize) {
            var closeParen = args.getCloseParen();
            throw new InvalidVariadicArgumentException(
                "passing no argument for the '...' parameter of a variadic macro "
                    + "is a C23 extension",
                closeParen, keeper.getExpandingChain());
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        // The last argument list contains all the variadic arguments.
        var vaArgs = (expectedSize < actualSize)
            ? args.get(actualSize - 1)
            : new ArrayList<Token>();
        mapping.put(VaArgs.KEYWORD, vaArgs);
        return mapping;
    }

    /**
        Substitutes macro parameters and returns the resulting list of tokens.

        <p>This method performs the following substitutions:</p>
        <ul>
            <li>Parameter substitution</li>
            <li>Stringification using the '#' operator</li>
            <li>Token concatenation using the '##' operator</li>
        </ul>

        @param token The token that triggered the macro expansion.
        @param args The list of macro arguments.
        @param kit The parse kit.
        @return The list of tokens after substitution.
    */
    public List<Token> substitute(Token token, MacroArgument args, ParseKit kit) {
        var keeper = kit.getKeeper();
        try {
            var mapping = getSubstitutionMapping(args, keeper);
            var substituted = substituteParamsAndStringify(mapping);
            var concatenated = concatenateTokens(substituted, kit);
            return expandMarkedTokens(concatenated, kit).stream()
                    .map(WrappedToken::unwrap)
                    .collect(Collectors.toList());
        } catch (PreprocessException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<WrappedToken> substituteParamsAndStringify(
            Map<String, List<Token>> mapping) throws PreprocessException {
        var substituted = new ArrayList<WrappedToken>();
        var i = 0;
        while (i < body.size()) {
            var currentToken = body.get(i);
            if (TokenKit.isStringizingOperator(currentToken)) {
                var nextTokenIndex = i + 1;
                while (nextTokenIndex < body.size()
                        && TokenKit.isDelimiterOrComment(body.get(nextTokenIndex))) {
                    nextTokenIndex++;
                }

                if (nextTokenIndex < body.size()) {
                    var nextToken = body.get(nextTokenIndex);
                    var nextValue = nextToken.getValue();
                    if (nextToken.isType(TokenType.IDENTIFIER)
                            && mapping.containsKey(nextValue)) {
                        var argTokens = mapping.get(nextValue);
                        var stringized = Tokens.stringize(argTokens,
                                currentToken.getSpan().getStart());
                        substituted.add(WrappedToken.of(stringized));
                        i = nextTokenIndex;
                    } else {
                        substituted.add(WrappedToken.of(currentToken));
                    }
                } else {
                    substituted.add(WrappedToken.of(currentToken));
                }
            } else if (currentToken.isType(TokenType.IDENTIFIER)) {
                substituteIdentifier(currentToken, mapping, substituted);
            } else {
                substituted.add(WrappedToken.of(currentToken));
            }
            i++;
        }
        return substituted;
    }

    private static void substituteIdentifier(Token token,
            Map<String, List<Token>> mapping,
            List<WrappedToken> substituted) throws PreprocessException {
        var tokenValue = token.getValue();
        if (tokenValue.equals(VaArgs.KEYWORD)) {
            var vaArgs = mapping.get(VaArgs.KEYWORD);
            if (vaArgs != null) {
                vaArgs.forEach(
                    t -> substituted.add(new ParameterOriginatedToken(t)));
            }
            return;
        }
        var value = mapping.get(tokenValue);
        if (value == null) {
            substituted.add(WrappedToken.of(token));
            return;
        }
        value.forEach(t -> substituted.add(new ParameterOriginatedToken(t)));
    }

    private List<WrappedToken> concatenateTokens(List<WrappedToken> tokens, ParseKit kit)
            throws PreprocessException {
        if (tokens.isEmpty()) {
            return tokens;
        }

        var result = new ArrayList<WrappedToken>();
        var i = 0;
        while (i < tokens.size()) {
            var currentMacroToken = tokens.get(i);
            var currentToken = currentMacroToken.unwrap();
            if (!TokenKit.isConcatenatingOperator(currentToken)) {
                result.add(currentMacroToken);
                ++i;
                continue;
            }
            var maybeLeft = findLastPastableToken(result);
            var maybeRight = findFirstPastableToken(tokens, i + 1);

            if (maybeRight.isEmpty()) {
                maybeLeft.map(left -> result.subList(left.index() + 1, result.size()))
                    .orElse(result)
                    .clear();
                break;
            }
            var right = maybeRight.get();
            if (maybeLeft.isEmpty()) {
                result.clear();
                i = right.index();
                continue;
            }
            var left = maybeLeft.get();
            result.subList(left.index(), result.size()).clear();
            var concatenated = kit.concatenate(left.token(), right.token());
            result.add(WrappedToken.of(concatenated));
            i = right.index() + 1;
        }
        return result;
    }

    private List<WrappedToken> expandMarkedTokens(List<WrappedToken> tokens, ParseKit kit)
            throws PreprocessException {
        var expander = new Expander(kit, tokens);
        return expander.apply();
    }

    private static Optional<TokenIndexPair> findLastPastableToken(
            List<WrappedToken> wrappedTokens) {
        for (var k = wrappedTokens.size() - 1; k >= 0; --k) {
            var token = wrappedTokens.get(k).unwrap();
            if (!TokenKit.isDelimiterOrComment(token)) {
                return Optional.of(new TokenIndexPair(token, k));
            }
        }
        return Optional.empty();
    }

    private static Optional<TokenIndexPair> findFirstPastableToken(
            List<WrappedToken> wrappedTokens, int start) {
        var n = wrappedTokens.size();
        for (var k = start; k < n; ++k) {
            var token = wrappedTokens.get(k).unwrap();
            if (!TokenKit.isDelimiterOrComment(token)) {
                return Optional.of(new TokenIndexPair(token, k));
            }
        }
        return Optional.empty();
    }

    @Override
    public void paste(PastingVisitor visitor) {
        visitor.paste(this);
    }
}
