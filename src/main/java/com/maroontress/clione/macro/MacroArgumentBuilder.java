package com.maroontress.clione.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

/**
    Builds arguments for function-like macros.
*/
public abstract class MacroArgumentBuilder {

    private static Map<String, Integer> parenLevelMap = newParenLevelMap();
    private final FunctionLikeMacro macro;
    private final List<List<Token>> args = new ArrayList<>();
    private final List<Token> currentArg = new ArrayList<>();
    private final Token openParen;
    private Token closeParen;
    private int parenLevel = 1;

    /**
        Constructs a new instance.

        @param macro The macro for which the builder is created.
        @param openParen The opening parenthesis of the argument list.
    */
    public MacroArgumentBuilder(FunctionLikeMacro macro, Token openParen) {
        this.macro = macro;
        this.openParen = openParen;
    }

    /**
        Returns the number of macro parameters.

        @return The number of macro parameters.
    */
    protected final int getMacroParameterSize() {
        return macro.parameters().size();
    }

    /**
        Returns the list of arguments.

        @return The list of arguments.
    */
    protected final List<List<Token>> getArgs() {
        return args;
    }

    /**
        Returns the opening parenthesis of the argument list.

        @return The opening parenthesis of the argument list.
    */
    protected final Token getOpenParen() {
        return openParen;
    }

    /**
        Returns the closing parenthesis of the argument list.

        @return The closing parenthesis of the argument list.
    */
    protected final Token getCloseParen() {
        return closeParen;
    }

    /**
        Returns whether to skip a comma.

        @return {@code true} if a comma should be skipped, {@code false}
        otherwise.
    */
    protected abstract boolean skipsComma();

    /**
        Builds a new macro argument.

        @return A new macro argument.
    */
    public final MacroArgument build() {
        var builtArgs = getArgs();
        if (macro.parameters().size() == 1 && builtArgs.isEmpty()) {
            builtArgs.add(List.of());
        }
        return new MacroArgument(getOpenParen(), builtArgs, getCloseParen());
    }

    /**
        Adds a token to the argument list.

        @param token The token to be added.
        @return {@code true} if the token is the closing parenthesis of the
        argument list, {@code false} otherwise.
    */
    public final boolean addToken(Token token) {
        if (token.getType() == TokenType.PUNCTUATOR
                && token.getValue().equals(")")
                && parenLevel == 1) {
            --parenLevel;
            closeParen = token;
            if (!args.isEmpty() || !currentArg.isEmpty()) {
                args.add(List.copyOf(currentArg));
            }
            return true;
        }
        addArgumentToken(token);
        return false;
    }

    private void addArgumentToken(Token token) {
        if (!skipsComma()
                && token.getValue().equals(",")
                && parenLevel == 1) {
            args.add(List.copyOf(currentArg));
            currentArg.clear();
            return;
        }
        updateParenLevel(token);
        currentArg.add(token);
    }

    private void updateParenLevel(Token token) {
        if (token.getType() != TokenType.PUNCTUATOR) {
            return;
        }
        var levelDelta = parenLevelMap.get(token.getValue());
        if (levelDelta == null) {
            return;
        }
        parenLevel += levelDelta;
    }

    private static Map<String, Integer> newParenLevelMap() {
        var map = new HashMap<String, Integer>();
        map.put("(", 1);
        map.put(")", -1);
        return map;
    }
}
