package com.maroontress.clione.macro.parameter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.macro.FunctionLikeMacro;
import com.maroontress.clione.macro.FunctionLikeMacroBehavior;
import com.maroontress.clione.macro.InvalidConcatenationOperatorException;
import com.maroontress.clione.macro.InvalidStringizingOperatorException;
import com.maroontress.clione.macro.Macro;
import com.maroontress.clione.macro.PreprocessException;
import com.maroontress.clione.macro.TokenKit;

/**
    Parses parameters of a function-like macro.
*/
public final class ParameterParser {

    private final String macroName;
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
    }

    /**
        Parses the parameters.

        @return The parsed macro.
        @throws PreprocessException if an error occurs.
    */
    public Macro parse() throws PreprocessException {
        while (!parserState.isCompleted()) {
            var token = queue.removeFirst();
            if (token.isType(TokenType.DIRECTIVE_END)) {
                throw new MissingParenInMacroParameterListException(token);
            }
            if (TokenKit.isDelimiterOrComment(token)) {
                continue;
            }
            parserState = TokenKit.isCloseParenthesis(token)
                ? parserState.onCloseParen(token)
                : parserState.nextState(token, parameters::add);
        }
        TokenKit.removeLeadingWhitespaces(queue);
        var behavior = parserState.getFunctionLikeMacroBehavior();
        validateStringizingOperators(queue, behavior);

        queue.removeLast();
        TokenKit.removeTrailingWhitespaces(queue);
        validateConcatenatingOperators(queue);
        behavior.validateVaArgKeyword(queue);

        return new FunctionLikeMacro(macroName, parameters, queue, behavior);
    }

    private void validateConcatenatingOperators(Deque<Token> body)
            throws PreprocessException {
        if (body.isEmpty()) {
            return;
        }
        var first = body.getFirst();
        if (TokenKit.isConcatenatingOperator(first)) {
            throw new InvalidConcatenationOperatorException(first, true);
        }
        var last = body.getLast();
        if (TokenKit.isConcatenatingOperator(last)) {
            throw new InvalidConcatenationOperatorException(last, false);
        }
    }

    private void validateStringizingOperators(Deque<Token> body,
            FunctionLikeMacroBehavior behavior) throws PreprocessException {
        var directiveEnd = body.getLast();
        var validator = behavior.newStringizingOperandValidator(parameters);
        var iter = body.iterator();
        while (iter.hasNext()) {
            if (!TokenKit.isStringizingOperator(iter.next())) {
                continue;
            }
            for (;;) {
                if (!iter.hasNext()) {
                    throw new InvalidStringizingOperatorException(directiveEnd);
                }
                var token = iter.next();
                if (TokenKit.isDelimiterOrComment(token)) {
                    continue;
                }
                if (validator.test(token)) {
                    break;
                }
                throw new InvalidStringizingOperatorException(token);
            }
        }
    }
}
