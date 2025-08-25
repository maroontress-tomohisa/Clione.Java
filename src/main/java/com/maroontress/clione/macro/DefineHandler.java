package com.maroontress.clione.macro;

import java.util.ArrayList;
import java.util.List;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

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
        /*
            + apply
              + parseFunctionLikeMacro
              | + validateMacroBody
              |   + validateStringizingOperators
              + getMacroBody
        */
    }

    /** {@inheritDoc} */
    @Override
    public void apply(List<Token> directiveTokens, int directiveNameIndex)
            throws PreprocessException {
        var maybePair = Tokens.findSignificantToken(
                directiveTokens, directiveNameIndex + 1);
        if (maybePair.isEmpty()) {
            throw new MissingMacroNameException(directiveTokens.getLast());
        }
        var pair = maybePair.get();
        var macroNameToken = pair.token();
        if (macroNameToken.getType() != TokenType.IDENTIFIER) {
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
            if (Tokens.isOpenParenthesis(nextToken)) {
                parseFunctionLikeMacro(directiveTokens, nameIndex);
                return;
            }
            if (!Tokens.isDelimiterOrComment(nextToken)
                    && nextToken.getType() != TokenType.DIRECTIVE_END) {
                throw new MissingWhitespaceAfterMacroName(nextToken);
            }
        }

        var maybeBodyPair = Tokens.findSignificantToken(
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
                    return t.getType() == TokenType.IDENTIFIER
                        && t.getValue().equals(MacroKeywords.VA_ARGS);
                })
                .findFirst();
        if (maybeVaArg.isPresent()) {
            throw new VaArgsKeywordMisusageException(maybeVaArg.get());
        }
    }

    // CHECKSTYLE:OFF CyclomaticComplexity
    private void parseFunctionLikeMacro(List<Token> tokens, int nameIndex)
            throws PreprocessException {
        var name = tokens.get(nameIndex).getValue();
        var directiveEnd = tokens.getLast();
        var parameters = new ArrayList<String>();
        var currentIndex = nameIndex + 2;
        var isVariadic = false;
        var closingParenFound = false;
        var lastToken = tokens.get(nameIndex);

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

        var body = Tokens.findSignificantToken(tokens, currentIndex + 1)
                .map(p -> getMacroBody(p.index(), tokens))
                .orElseGet(() -> List.<Token>of());
        var behavior = isVariadic
                ? FunctionLikeMacroBehavior.VARIADIC
                : FunctionLikeMacroBehavior.DEFAULT;
        validateMacroBody(body, directiveEnd, parameters, behavior);
        var macro = new FunctionLikeMacro(name, parameters, body, behavior);
        keeper.defineMacro(macro);
    }
    // CHECKSTYLE:ON CyclomaticComplexity

    private void validateMacroBody(List<Token> body,
            Token directiveEnd,
            List<String> parameters,
            FunctionLikeMacroBehavior behavior) throws PreprocessException {
        if (!body.isEmpty()) {
            var firstToken = body.getFirst();
            if (Tokens.isConcatenatingOperator(firstToken)) {
                throw new InvalidConcatenationOperatorException(
                        firstToken, true);
            }
            var lastToken = body.getLast();
            if (Tokens.isConcatenatingOperator(lastToken)) {
                throw new InvalidConcatenationOperatorException(
                        lastToken, false);
            }
        }
        validateStringizingOperators(body, directiveEnd, parameters, behavior);
        behavior.validateVaArgKeyword(body);
    }

    private void validateStringizingOperators(List<Token> body,
            Token directiveEnd,
            List<String> parameters,
            FunctionLikeMacroBehavior behavior) throws PreprocessException {
        var validator = behavior.newStringizingOperandValidator(parameters);
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
                throw new InvalidStringizingOperatorException(directiveEnd);
            }
            var nextToken = body.get(nextTokenIndex);
            if (!validator.test(nextToken)) {
                throw new InvalidStringizingOperatorException(nextToken);
            }
        }
    }

    private List<Token> getMacroBody(int bodyIndex, List<Token> tokens) {
        var macroBody = new ArrayList<Token>();
        for (var i = bodyIndex; i < tokens.size(); ++i) {
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

    private enum ParameterParseState {
        EXPECT_IDENTIFIER,
        EXPECT_COMMA_OR_PAREN,
    }
}
