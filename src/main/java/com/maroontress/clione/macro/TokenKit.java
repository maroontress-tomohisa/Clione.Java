package com.maroontress.clione.macro;

import java.util.List;
import java.util.Optional;

import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

// TokenBuilder, SourceCharsを使わないように作り直す（stringize()をclioneに移す）
/**
    The utility class for operations on token sequence.
*/
public final class TokenKit {

    private TokenKit() {
    }

    /**
        Finds the first significant token in a list of tokens.

        @param tokenList The list of tokens to search.
        @param startIndex The starting index of the search.
        @return An optional containing the first significant token and its
            index, or an empty optional if no significant token is found.
    */
    public static Optional<TokenIndexPair> findSignificantToken(
            List<Token> tokenList, int startIndex) {
        var size = tokenList.size();
        var index = startIndex;
        while (index < size
                && TokenKit.isDelimiterOrComment(tokenList.get(index))) {
            ++index;
        }
        if (index >= size) {
            return Optional.empty();
        }
        var token = tokenList.get(index);
        if (token.isType(TokenType.DIRECTIVE_END)) {
            return Optional.empty();
        }
        return Optional.of(new TokenIndexPair(token, index));
    }

    /**
        Checks whether the given token is a delimiter or a comment.

        @param token The token to be checked.
        @return {@code true} if the token is a delimiter or a comment,
            otherwise {@code false}.
    */
    public static boolean isDelimiterOrComment(Token token) {
        var type = token.getType();
        return type == TokenType.DELIMITER
                || type == TokenType.COMMENT;
    }

    /**
        Checks whether the given token is a stringizing operator.

        @param token The token to be checked.
        @return {@code true} if the token is a stringizing operator,
            otherwise {@code false}.
    */
    public static boolean isStringizingOperator(Token token) {
        return isTypeAndValue(token, TokenType.OPERATOR, "#");
    }

    /**
        Checks whether the given token is a concatenating operator.

        @param token The token to be checked.
        @return {@code true} if the token is a concatenating operator,
            otherwise {@code false}.
    */
    public static boolean isConcatenatingOperator(Token token) {
        return isTypeAndValue(token, TokenType.OPERATOR, "##");
    }

    /**
        Checks whether the given token is an opening parenthesis.

        @param token The token to be checked.
        @return {@code true} if the token is an opening parenthesis,
            otherwise {@code false}.
    */
    public static boolean isOpenParenthesis(Token token) {
        return isTypeAndValue(token, TokenType.PUNCTUATOR, "(");
    }

    /**
        Checks whether the given token is a closing parenthesis.

        @param token The token to be checked.
        @return {@code true} if the token is a closing parenthesis,
            otherwise {@code false}.
    */
    public static boolean isCloseParenthesis(Token token) {
        return isTypeAndValue(token, TokenType.PUNCTUATOR, ")");
    }

    /**
        Checks whether the given token is an ellipsis.

        @param token The token to be checked.
        @return {@code true} if the token is an ellipsis,
            otherwise {@code false}.
    */
    public static boolean isEllipsis(Token token) {
        return isTypeAndValue(token, TokenType.PUNCTUATOR, "...");
    }

    /**
        Checks whether the given token is a comma.

        @param token The token to be checked.
        @return {@code true} if the token is a comma,
            otherwise {@code false}.
    */
    public static boolean isComma(Token token) {
        return isTypeAndValue(token, TokenType.PUNCTUATOR, ",");
    }

    private static boolean isTypeAndValue(
            Token token, TokenType type, String value) {
        return token.isType(type) && token.isValue(value);
    }
}
