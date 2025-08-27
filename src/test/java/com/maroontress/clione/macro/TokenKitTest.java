package com.maroontress.clione.macro;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maroontress.clione.LexicalParser;
import com.maroontress.clione.Token;
import com.maroontress.clione.TokenType;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TokenKitTest {

    @Test
    void findSignificantTokenEmpty() {
        var tokens = List.<Token>of();
        var actual = TokenKit.findSignificantToken(tokens, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void findSignificantTokenOnlyDelimiters() {
        var tokenList = newTokenList(" /* comment */");
        // (" ", 1, 1, TokenType.DELIMITER)
        // ("/* comment */", 1, 2, TokenType.COMMENT)
        var actual = TokenKit.findSignificantToken(tokenList, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void findSignificantTokenAtStart() {
        var tokenList = newTokenList("hello");
        var actual = TokenKit.findSignificantToken(tokenList, 0);
        assertTrue(actual.isPresent());
        assertThat(actual.get().token(), is(tokenList.get(0)));
        assertThat(actual.get().index(), is(0));
    }

    @Test
    void findSignificantTokenInMiddle() {
        //                            12345678901234567890
        var tokenList = newTokenList(" hello/* comment */world");
        // (" ", 1, 1, TokenType.DELIMITER)
        // ("hello", 1, 2, TokenType.IDENTIFIER)
        // ("/* comment */", 1, 7, TokenType.COMMENT)
        // ("world", 1, 20, TokenType.IDENTIFIER)
        var actual = TokenKit.findSignificantToken(tokenList, 0);
        assertTrue(actual.isPresent());
        assertThat(actual.get().token(), is(notNullValue()));
        assertThat(actual.get().token().getValue(), is("hello"));
        assertThat(actual.get().index(), is(1));
    }

    @Test
    void findSignificantTokenNotFound() {
        var tokenList = newTokenList(" /* comment */");
        // (" ", 1, 1, TokenType.DELIMITER)
        // ("/* comment */", 1, 2, TokenType.COMMENT)
        var actual = TokenKit.findSignificantToken(tokenList, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void findSignificantTokenDirectiveEnd() {
        var tokenList = newTokenList("#");
        var children = tokenList.get(0).getChildren();
        var actual = TokenKit.findSignificantToken(children, 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    void isDelimiterOrComment() {
        assertTrue(TokenKit.isDelimiterOrComment(newSingleToken(" ")));
        assertTrue(TokenKit.isDelimiterOrComment(newSingleToken("/**/")));
        assertFalse(TokenKit.isDelimiterOrComment(newSingleToken("a")));
    }

    @Test
    void isStringizingOperator() {
        var stringize = newSingleToken("#").withType(TokenType.OPERATOR);
        assertTrue(TokenKit.isStringizingOperator(stringize));
        assertFalse(TokenKit.isStringizingOperator(
                newSingleToken("##").withType(TokenType.OPERATOR)));
        assertFalse(TokenKit.isStringizingOperator(newSingleToken("#")));
    }

    @Test
    void isConcatenatingOperator() {
        var concat = newSingleToken("##").withType(TokenType.OPERATOR);
        assertTrue(TokenKit.isConcatenatingOperator(concat));
        assertFalse(TokenKit.isConcatenatingOperator(
                newSingleToken("#").withType(TokenType.OPERATOR)));
        assertFalse(TokenKit.isConcatenatingOperator(newSingleToken("##")));
    }

    @Test
    void isOpenParenthesis() {
        var openParen = newSingleToken("(");
        assertTrue(TokenKit.isOpenParenthesis(openParen));
        assertFalse(TokenKit.isOpenParenthesis(newSingleToken(")")));
        assertFalse(TokenKit.isOpenParenthesis(
                openParen.withType(TokenType.IDENTIFIER)));
    }

    @Test
    void isCloseParenthesis() {
        var closeParen = newSingleToken(")");
        assertTrue(TokenKit.isCloseParenthesis(closeParen));
        assertFalse(TokenKit.isCloseParenthesis(newSingleToken("(")));
        assertFalse(TokenKit.isCloseParenthesis(
                closeParen.withType(TokenType.IDENTIFIER)));
    }

    @Test
    void isEllipsis() {
        var ellipsis = newSingleToken("...");
        assertTrue(TokenKit.isEllipsis(ellipsis));
        assertFalse(TokenKit.isEllipsis(newSingleToken(".")));
        assertFalse(TokenKit.isEllipsis(
                ellipsis.withType(TokenType.IDENTIFIER)));
    }

    @Test
    void isComma() {
        var comma = newSingleToken(",");
        assertTrue(TokenKit.isComma(comma));
        assertFalse(TokenKit.isComma(newSingleToken(";")));
        assertFalse(TokenKit.isComma(comma.withType(TokenType.IDENTIFIER)));
    }

    private Token newSingleToken(String value) {
        try {
            return LexicalParser.of(new StringReader(value))
                    .next()
                    .orElseThrow();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Token> newTokenList(String s) {
        var parser = LexicalParser.of(new StringReader(s));
        return Stream.generate(() -> getToken(parser))
                .takeWhile(t -> t.isPresent())
                .map(t -> t.get())
                .toList();
    }

    private Optional<Token> getToken(LexicalParser parser) {
        try {
            return parser.next();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
