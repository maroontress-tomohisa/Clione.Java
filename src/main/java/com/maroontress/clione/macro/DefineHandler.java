package com.maroontress.clione.macro;

import java.util.ArrayDeque;
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
        // The directiveTokens should end with TokenType.DIRECTIVE_END.
        var payload = directiveTokens.subList(
                directiveNameIndex + 1, directiveTokens.size());
        var macro = parse(new ArrayDeque<>(payload));
        keeper.defineMacro(macro);
    }

    private Macro parse(Deque<Token> queue) throws PreprocessException {
        TokenKit.removeLeadingWhitespaces(queue);
        if (queue.peekFirst().isType(TokenType.DIRECTIVE_END)) {
            throw new MissingMacroNameException(queue.getLast());
        }
        var nameToken = queue.removeFirst();
        if (!nameToken.isType(TokenType.IDENTIFIER)) {
            throw new InvalidMacroNameException(nameToken);
        }
        if (nameToken.isValue(VaArgs.KEYWORD)) {
            throw new VaArgsKeywordMisusageException(nameToken);
        }
        var name = nameToken.getValue();
        // Here, there should be at least one token (DIRECTIVE_END).
        var nextToken = queue.removeFirst();
        if (nextToken.isType(TokenType.DIRECTIVE_END)) {
            return new ObjectLikeMacro(name, queue);
        }
        if (TokenKit.isOpenParenthesis(nextToken)) {
            return parseFunctionLikeMacro(name, queue);
        }
        if (!TokenKit.isDelimiterOrComment(nextToken)) {
            throw new MissingWhitespaceAfterMacroName(nextToken);
        }
        TokenKit.removeLeadingWhitespaces(queue);
        queue.removeLast();
        TokenKit.removeTrailingWhitespaces(queue);
        VaArgs.validateBody(queue);
        return new ObjectLikeMacro(name, queue);
    }

    private Macro parseFunctionLikeMacro(String name, Deque<Token> queue)
            throws PreprocessException {
        var parser = new ParameterParser(name, queue);
        return parser.parse();
    }
}
