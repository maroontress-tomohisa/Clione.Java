package com.maroontress.clione.macro;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.macro.parameter.ParameterParser;

/**
    Handles the #define directive.
*/
public final class DefineHandler implements DirectiveHandler {

    private final MacroKeeper keeper;

    /**
        Constructs a new instance.

        @param keeper the macro keeper.
    */
    public DefineHandler(MacroKeeper keeper) {
        this.keeper = keeper;
    }

    /** {@inheritDoc} */
    @Override
    public void apply(List<Token> directiveTokens, int directiveNameIndex)
            throws PreprocessException {
        var maybePair = TokenKit.findSignificantToken(
                directiveTokens, directiveNameIndex + 1);
        if (maybePair.isEmpty()) {
            throw new MissingMacroNameException(directiveTokens.getLast());
        }
        var pair = maybePair.get();
        var macroNameToken = pair.token();
        if (!macroNameToken.isType(TokenType.IDENTIFIER)) {
            throw new InvalidMacroNameException(macroNameToken);
        }
        var macroName = macroNameToken.getValue();
        if (macroName.equals(MacroKeywords.VA_ARGS)) {
            throw new VaArgsKeywordMisusageException(macroNameToken);
        }
        var nameIndex = pair.index();

        var nextTokenIndex = pair.index() + 1;
        if (nextTokenIndex < directiveTokens.size()) {
            var nextToken = directiveTokens.get(nextTokenIndex);
            if (TokenKit.isOpenParenthesis(nextToken)) {
                var subList = directiveTokens.subList(
                        nameIndex + 2, directiveTokens.size());
                var queue = new ArrayDeque<>(subList);
                parseFunctionLikeMacro(macroName, queue);
                return;
            }
            if (!TokenKit.isDelimiterOrComment(nextToken)
                    && !nextToken.isType(TokenType.DIRECTIVE_END)) {
                throw new MissingWhitespaceAfterMacroName(nextToken);
            }
        }

        var maybeBodyPair = TokenKit.findSignificantToken(
                directiveTokens, nameIndex + 1);
        var body = maybeBodyPair.map(p -> getMacroBody(p.index(), directiveTokens))
            .orElseGet(() -> List.<Token>of());
        validateVaArgKeyword(body);
        keeper.defineMacro(new ObjectLikeMacro(macroName, body));
    }

    /**
        Validates whether the `__VA_ARGS__` keyword is used correctly.

        @param body The macro body.
        @throws VaArgsKeywordMisusageException If the `__VA_ARGS__` keyword is
        used incorrectly.
    */
    public static void validateVaArgKeyword(List<Token> body)
            throws VaArgsKeywordMisusageException {
        var maybeVaArg = body.stream()
                .filter(t -> {
                    return t.isType(TokenType.IDENTIFIER)
                        && t.isValue(MacroKeywords.VA_ARGS);
                })
                .findFirst();
        if (maybeVaArg.isPresent()) {
            throw new VaArgsKeywordMisusageException(maybeVaArg.get());
        }
    }

    /**
        Returns the macro body from the given tokens.

        @param bodyIndex The index of the first token of the macro body.
        @param tokens The list of tokens.
        @return The macro body.
    */
    public static List<Token> getMacroBody(int bodyIndex, List<Token> tokens) {
        var macroBody = new ArrayList<Token>();
        for (var i = bodyIndex; i < tokens.size(); ++i) {
            var token = tokens.get(i);
            if (token.isType(TokenType.DIRECTIVE_END)) {
                break;
            }
            macroBody.add(token);
        }

        for (var i = macroBody.size() - 1; i >= 0; i--) {
            if (!TokenKit.isDelimiterOrComment(macroBody.get(i))) {
                return macroBody.subList(0, i + 1);
            }
        }

        return new ArrayList<>();
    }

    private void parseFunctionLikeMacro(String name, Deque<Token> queue)
            throws PreprocessException {
        var parser = new ParameterParser(name, queue);
        var macro = parser.parse();
        keeper.defineMacro(macro);
    }
}
