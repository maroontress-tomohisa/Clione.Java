package com.maroontress.clione.macro;

import java.util.List;

import com.maroontress.clione.Token;

/**
    Represents the arguments of a function-like macro invocation.
*/
public final class MacroArgument {
    private Token openParen;
    private Token closeParen;
    private List<List<Token>> argumentList;
    private List<Token> commaList;

    /**
        Constructs a new instance.

        @param openParen The opening parenthesis of the argument list.
        @param argumentList The list of arguments. Each argument is a list of
        tokens.
        @param commaList The list of commas that separate arguments.
        @param closeParen The closing parenthesis of the argument list.
    */
    public MacroArgument(
            Token openParen,
            List<List<Token>> argumentList,
            List<Token> commaList,
            Token closeParen) {
        this.openParen = openParen;
        this.argumentList = argumentList;
        this.commaList = commaList;
        this.closeParen = closeParen;
    }

    /**
        Returns the number of arguments.

        @return The number of arguments.
    */
    public int size() {
        return argumentList.size();
    }

    /**
        Returns the argument at the specified position in this list.

        @param index The index of the argument to return.
        @return The argument at the specified position in this list.
    */
    public List<Token> get(int index) {
        return argumentList.get(index);
    }

    /**
        Returns the comma token at the specified position in this list.

        @param index The index of the comma token to return.
        @return The comma token at the specified position in this list.
    */
    public Token getComma(int index) {
        return commaList.get(index);
    }

    /**
        Returns the opening parenthesis of the argument list.

        @return The opening parenthesis of the argument list.
    */
    public Token getOpenParen() {
        return openParen;
    }

    /**
        Returns the closing parenthesis of the argument list.

        @return The closing parenthesis of the argument list.
    */
    public Token getCloseParen() {
        return closeParen;
    }
}
