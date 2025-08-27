package com.maroontress.clione.macro;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maroontress.clione.SourceChar;
import com.maroontress.clione.SourceLocation;
import com.maroontress.clione.SourceSpan;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;
import com.maroontress.clione.impl.DefaultToken;
import com.maroontress.clione.impl.SourceChars;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// DefaultToken, SourceCharsを使わないように作り直す
class TokenKitTest {

    private SourceChar newChar(char c, int line, int column) {
        return SourceChars.of(c, column, line);
    }

    private Token newIdentifierToken(String value, int line, int column) {
        return newToken(value, line, column, TokenType.IDENTIFIER);
    }

    private Token newToken(String value, int line, int column, TokenType type) {
        var chars = new ArrayList<SourceChar>();
        for (var i = 0; i < value.length(); ++i) {
            chars.add(newChar(value.charAt(i), line, column + i));
        }
        return new DefaultToken(chars, type);
    }

    private SourceSpan newSpan(int startLine, int startColumn,
        int endLine, int endColumn) {
        var start = new SourceLocation(startLine, startColumn);
        var end = new SourceLocation(endLine, endColumn);
        return new SourceSpan(start, end);
    }

    @Test
    void testStringizeEmpty() {
        var tokens = List.<Token>of();
        var span = newSpan(1, 1, 1, 1);
        var actual = TokenKit.stringize(tokens, span);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getType(), is(TokenType.STRING));
        assertThat(actual.getValue(), is(equalTo("""
            ""
            """.trim())));
    }

    @Test
    void testStringizeSingleToken() {
        var tokens = List.of(newIdentifierToken("hello", 1, 1));
        var span = newSpan(1, 1, 1, 1);
        var actual = TokenKit.stringize(tokens, span);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getType(), is(TokenType.STRING));
        assertThat(actual.getValue(), is(equalTo("""
            "hello"
            """.trim())));
    }

    @Test
    void testStringizeMultiTokens() {
        var tokens = List.of(
            newIdentifierToken("hello", 1, 1),
            newToken(" ", 1, 6, TokenType.DELIMITER),
            newIdentifierToken("world", 1, 7));
        var span = newSpan(1, 1, 1, 1);
        var actual = TokenKit.stringize(tokens, span);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getType(), is(TokenType.STRING));
        assertThat(actual.getValue(), is(equalTo("""
            "hello world"
            """.trim())));
    }

    @Test
    void testStringizeWithEscape() {
        var tokens = List.of(newIdentifierToken("""
                "hello\\nworld"
                """.trim(), 1, 1));
        var span = newSpan(1, 1, 1, 1);
        var actual = TokenKit.stringize(tokens, span);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getType(), is(TokenType.STRING));
        assertThat(actual.getValue(), is(equalTo("""
            "\\"hello\\\\nworld\\""
            """.trim())));
    }

    @Test
    void testFindSignificantTokenEmpty() {
        var tokens = List.<Token>of();
        var actual = TokenKit.findSignificantToken(tokens, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void testFindSignificantTokenOnlyDelimiters() {
        var tokens = List.of(
            newToken(" ", 1, 1, TokenType.DELIMITER),
            newToken("/* comment */", 1, 2, TokenType.COMMENT));
        var actual = TokenKit.findSignificantToken(tokens, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void testFindSignificantTokenAtStart() {
        var id = newIdentifierToken("hello", 1, 1);
        var tokens = List.of(id);
        var actual = TokenKit.findSignificantToken(tokens, 0);
        assertTrue(actual.isPresent());
        assertThat(actual.get().token(), is(id));
        assertThat(actual.get().index(), is(0));
    }

    @Test
    void testFindSignificantTokenInMiddle() {
        var id = newIdentifierToken("world", 1, 15);
        var tokens = List.of(
            newToken(" ", 1, 1, TokenType.DELIMITER),
            newIdentifierToken("hello", 1, 2),
            newToken("/* comment */", 1, 7, TokenType.COMMENT),
            id);
        var actual = TokenKit.findSignificantToken(tokens, 1);
        assertTrue(actual.isPresent());
        assertThat(actual.get().token(), is(notNullValue()));
        assertThat(actual.get().token().getValue(), is("hello"));
        assertThat(actual.get().index(), is(1));
    }

    @Test
    void testFindSignificantTokenNotFound() {
        var tokens = List.of(
            newToken(" ", 1, 1, TokenType.DELIMITER),
            newToken("/* comment */", 1, 2, TokenType.COMMENT));
        var actual = TokenKit.findSignificantToken(tokens, 2);
        assertTrue(actual.isEmpty());
    }

    @Test
    void testFindSignificantTokenDirectiveEnd() {
        var tokens = List.of(
            newToken(" ", 1, 1, TokenType.DELIMITER),
            newToken("\\n", 1, 2, TokenType.DIRECTIVE_END));
        var actual = TokenKit.findSignificantToken(tokens, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void testIsDelimiterOrComment() {
        assertTrue(TokenKit.isDelimiterOrComment(newToken(" ", 1, 1, TokenType.DELIMITER)));
        assertTrue(TokenKit.isDelimiterOrComment(newToken("/**/", 1, 1, TokenType.COMMENT)));
        assertFalse(TokenKit.isDelimiterOrComment(newIdentifierToken("a", 1, 1)));
    }

    @Test
    void testIsStringizingOperator() {
        assertTrue(TokenKit.isStringizingOperator(newToken("#", 1, 1, TokenType.OPERATOR)));
        assertFalse(TokenKit.isStringizingOperator(newToken("##", 1, 1, TokenType.OPERATOR)));
        assertFalse(TokenKit.isStringizingOperator(newIdentifierToken("#", 1, 1)));
    }

    @Test
    void testIsConcatenatingOperator() {
        assertTrue(TokenKit.isConcatenatingOperator(newToken("##", 1, 1, TokenType.OPERATOR)));
        assertFalse(TokenKit.isConcatenatingOperator(newToken("#", 1, 1, TokenType.OPERATOR)));
        assertFalse(TokenKit.isConcatenatingOperator(newIdentifierToken("##", 1, 1)));
    }

    @Test
    void testIsOpenParenthesis() {
        assertTrue(TokenKit.isOpenParenthesis(newToken("(", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isOpenParenthesis(newToken(")", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isOpenParenthesis(newIdentifierToken("(", 1, 1)));
    }

    @Test
    void testIsCloseParenthesis() {
        assertTrue(TokenKit.isCloseParenthesis(newToken(")", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isCloseParenthesis(newToken("(", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isCloseParenthesis(newIdentifierToken(")", 1, 1)));
    }

    @Test
    void testIsEllipsis() {
        assertTrue(TokenKit.isEllipsis(newToken("...", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isEllipsis(newToken(".", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isEllipsis(newIdentifierToken("...", 1, 1)));
    }

    @Test
    void testIsComma() {
        assertTrue(TokenKit.isComma(newToken(",", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isComma(newToken(";", 1, 1, TokenType.PUNCTUATOR)));
        assertFalse(TokenKit.isComma(newIdentifierToken(",", 1, 1)));
    }

    // @Test
    // void testConcatenate() {
    //     var left = newIdentifierToken("a", 1, 1);
    //     var right = newIdentifierToken("b", 1, 2);
    //     var actual = TokenKit.concatenate(left, right, Set.of());
    //     assertThat(actual.getValue(), is("ab"));
    //     assertThat(actual.getType(), is(TokenType.IDENTIFIER));
    // }

    // @Test
    // void testConcatenateToReserved() {
    //     var left = newIdentifierToken("if", 1, 1);
    //     var right = newIdentifierToken("def", 1, 3);
    //     var actual = TokenKit.concatenate(left, right, Set.of("ifdef"));
    //     assertThat(actual.getValue(), is("ifdef"));
    //     assertThat(actual.getType(), is(TokenType.RESERVED));
    // }

    // @Test
    // void testConcatenateToOperator() {
    //     var left = newToken("+", 1, 1, TokenType.OPERATOR);
    //     var right = newToken("+", 1, 2, TokenType.OPERATOR);
    //     var actual = TokenKit.concatenate(left, right, Set.of());
    //     assertThat(actual.getValue(), is("++"));
    //     assertThat(actual.getType(), is(TokenType.OPERATOR));
    // }

    // @Test
    // void testConcatenateToUnknown() {
    //     var left = newIdentifierToken("a", 1, 1);
    //     var right = newToken(".", 1, 2, TokenType.PUNCTUATOR);
    //     var actual = TokenKit.concatenate(left, right, Set.of());
    //     assertThat(actual.getValue(), is("a."));
    //     assertThat(actual.getType(), is(TokenType.UNKNOWN));
    // }
}
