package com.maroontress.clione.macro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.maroontress.clione.Preprocessor;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Represents a function-like preprocessor macro.
*/
public final class FunctionLikeMacro implements Macro {

    private static final MacroBehavior DEFAULT_BEHAVIOR
        = new DefaultMacroBehavior();
    private static final MacroBehavior VARIADIC_BEHAVIOR
        = new VariadicMacroBehavior();

    private final String name;
    private final List<String> parameters;
    private final List<Token> body;
    private final MacroBehavior behavior;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param parameters The list of parameter names.
        @param isVariadic {@code true} if the macro is variadic, {@code false}
        otherwise.
        @param body The list of tokens that form the macro's body.
    */
    public FunctionLikeMacro(String name,
                             List<String> parameters,
                             boolean isVariadic,
                             List<Token> body) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.body = List.copyOf(body);
        this.behavior = isVariadic
            ? VARIADIC_BEHAVIOR
            : DEFAULT_BEHAVIOR;
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> parameters() {
        return parameters;
    }

    /** {@inheritDoc} */
    @Override
    public List<Token> body() {
        return body;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Token> apply(Preprocessor preprocessor, Token token)
            throws IOException {
        var maybeArguments = parseArguments(preprocessor, token);
        return !maybeArguments.isPresent()
            ? Optional.of(token)
            : preprocessor.expandFunctionBasedMacro(this, token, maybeArguments.get());
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            MacroArgument args, Preprocessor preprocessor)
            throws PreprocessException {
        var argumentSize = args.size();
        for (var k = 0; k < argumentSize; ++k) {
            var tokenList = args.get(k);
            if (tokenList.isEmpty()) {
                continue;
            }
            var maybeFirst = tokenList.stream()
                    .filter(t -> t.getType() == TokenType.DIRECTIVE)
                    .findFirst();
            if (maybeFirst.isPresent()) {
                throw new DirectiveWithinMacroArgumentsException(
                    maybeFirst.get(),
                    List.copyOf(preprocessor.getExpandingMacros().values()));
            }
        }
        return behavior.getSubstitutionMapping(this, args, preprocessor);
    }

    private Optional<MacroArgument> parseArguments(Preprocessor preprocessor,
            Token macroName) throws IOException {
        var maybeOpenParen = preprocessor.lookAheadForParen();
        /*
            This code should be written as follows:

            return maybeOpenParen.map(openParen -> {
                ...
            });

            but preprocessor.nextMacroToken() throws an IOException.
        */
        if (!maybeOpenParen.isPresent()) {
            return Optional.empty();
        }
        var openParen = maybeOpenParen.get();
        var builder = behavior.createArgumentBuilder(this, openParen);
        for (;;) {
            var maybeMacroToken = preprocessor.nextMacroToken();
            if (maybeMacroToken.isEmpty()) {
                var expandingTokens = List.copyOf(
                    preprocessor.getExpandingMacros().values());
                throw new UnterminatedMacroInvocationException(
                    macroName, expandingTokens);
            }
            var macroToken = maybeMacroToken.get();
            if (macroToken instanceof MacroEndMarker) {
                var expandingTokens = List.copyOf(
                    preprocessor.getExpandingMacros().values());
                throw new UnterminatedMacroInvocationException(
                    macroName, expandingTokens);
            }
            var maybeToken = macroToken.getToken();
            if (!maybeToken.isPresent()) {
                // ここには来ない。isPresent()がfalseになるのはMacroEndMarkerだけなので。
                // ポリモーフィズムを使う。
                continue;
            }
            var token = maybeToken.get();
            if (builder.addToken(token)) {
                break;
            }
        }
        return Optional.of(builder.build());
    }

    /**
        Creates a new macro argument builder.

        @param openParen The opening parenthesis of the argument list.
        @return A new macro argument builder.
    */
    public MacroArgumentBuilder newArgumentBuilder(Token openParen) {
        return behavior.createArgumentBuilder(this, openParen);
    }

    public Map<String, List<Token>> getVariadicSubstitutionMapping(
            MacroArgument args, Preprocessor preprocessor)
            throws PreprocessException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize > actualSize) {
            var expandingTokens = preprocessor.getExpandingMacros().values();
            var closeParen = args.getCloseParen();
            throw new MacroArgumentException(closeParen, expectedSize,
                    actualSize, List.copyOf(expandingTokens));
        }
        if (params.size() > 0 && expectedSize == actualSize) {
            var expandingTokens = preprocessor.getExpandingMacros().values();
            var closeParen = args.getCloseParen();
            throw new InvalidVariadicArgumentException(
                "passing no argument for the '...' parameter of a variadic macro "
                    + "is a C23 extension",
                closeParen, List.copyOf(expandingTokens));
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        // The last argument list contains all the variadic arguments.
        var vaArgs = (expectedSize < actualSize)
            ? args.get(actualSize - 1)
            : new ArrayList<Token>();
        mapping.put("__VA_ARGS__", vaArgs);
        return mapping;
    }
}
