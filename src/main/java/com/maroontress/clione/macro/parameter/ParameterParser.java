package com.maroontress.clione.macro.parameter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.macro.DefineHandler;
import com.maroontress.clione.macro.FunctionLikeMacro;
import com.maroontress.clione.macro.FunctionLikeMacroBehavior;
import com.maroontress.clione.macro.InvalidConcatenationOperatorException;
import com.maroontress.clione.macro.InvalidStringizingOperatorException;
import com.maroontress.clione.macro.Macro;
import com.maroontress.clione.macro.PreprocessException;
import com.maroontress.clione.macro.Tokens;

/**
    Parses parameters of a function-like macro.
*/
public final class ParameterParser {

    private final String macroName;
    private final Token directiveEnd;
    private final Deque<Token> queue;
    private final List<String> parameters = new ArrayList<>();
    private State parserState = State.INITIAL;

    /**
        Creates a new instance.

        @param macroName The name of the macro.
        @param queue The queue of tokens.
    */
    public ParameterParser(String macroName, Deque<Token> queue) {
        this.macroName = macroName;
        this.queue = queue;
        this.directiveEnd = queue.getLast();
    }

    /**
        Parses the parameters.

        @return The parsed macro.
        @throws PreprocessException if an error occurs.
    */
    public Macro parse() throws PreprocessException {
        while (!parserState.isCompleted()) {
            var token = queue.removeFirst();
            var tokenType = token.getType();
            if (tokenType == TokenType.DIRECTIVE_END) {
                throw new MissingParenInMacroParameterListException(token);
            }
            if (Tokens.isDelimiterOrComment(token)) {
                continue;
            }
            parserState = Tokens.isCloseParenthesis(token)
                ? parserState.onCloseParen(token)
                : parserState.nextState(token, parameters::add);
        }
        var behavior = parserState.getFunctionLikeMacroBehavior();
        var bodyList = new ArrayList<>(queue);
        var body = Tokens.findSignificantToken(bodyList, 0)
                .map(p -> DefineHandler.getMacroBody(p.index(), bodyList))
                .orElseGet(() -> List.<Token>of());
        validateMacroBody(body, behavior);
        return new FunctionLikeMacro(macroName, parameters, body, behavior);
    }

    private void validateMacroBody(List<Token> body,
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
        validateStringizingOperators(body, behavior);
        behavior.validateVaArgKeyword(body);
    }

    private void validateStringizingOperators(List<Token> body,
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
}
