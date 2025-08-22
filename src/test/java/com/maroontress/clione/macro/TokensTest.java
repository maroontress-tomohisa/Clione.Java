package com.maroontress.clione.macro;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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

class TokensTest {

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
        var actual = Tokens.stringize(tokens, span);

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
        var actual = Tokens.stringize(tokens, span);

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
        var actual = Tokens.stringize(tokens, span);

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
        var actual = Tokens.stringize(tokens, span);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getType(), is(TokenType.STRING));
        assertThat(actual.getValue(), is(equalTo("""
            "\\"hello\\\\nworld\\""
            """.trim())));
    }
}
