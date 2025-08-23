package com.maroontress.clione;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.maroontress.clione.macro.InvalidConcatenationOperatorException;
import com.maroontress.clione.macro.InvalidMacroNameException;
import com.maroontress.clione.macro.InvalidStringizingOperatorException;
import com.maroontress.clione.macro.MissingCommaException;
import com.maroontress.clione.macro.MissingIdentifierException;
import com.maroontress.clione.macro.MissingMacroNameException;
import com.maroontress.clione.macro.MissingParenException;
import static com.maroontress.clione.Parsers.pair;
import static com.maroontress.clione.Parsers.test;

public final class DefineErrorTest {

    @Test
    public void invalidConcatenationOperatorAtStart() {
        var s = """
            #define FOO(x) /**/ ## x
            """;
        var m = """
            L1:21: error: '##' cannot appear at start of macro expansion""";
        test(s, parser -> {
            var e = assertThrows(InvalidConcatenationOperatorException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("##"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(21));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(22));
        });
    }

    @Test
    public void invalidConcatenationOperatorAtEnd() {
        var s = """
            #define FOO(x) x ## /**/
            """;
        var m = """
            L1:18: error: '##' cannot appear at end of macro expansion""";
        test(s, parser -> {
            var e = assertThrows(InvalidConcatenationOperatorException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("##"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(18));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(19));
        });
    }

    @Test
    public void invalidStringizingOperator() {
        var s = """
            #define FOO(x) #X
            """;
        var m = """
            L1:17: error: '#' is not followed by a macro parameter""";
        test(s, parser -> {
            var e = assertThrows(InvalidStringizingOperatorException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("X"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(17));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(17));
        });
    }

    @Test
    public void missingCommaInMacroParameterList() {
        var s = """
            #define FOO(a b)
            """;
        var m = "L1:15: error: missing ',' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingCommaException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("b"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(15));
        });
    }

    @Test
    public void commentsInMacroParameterList() {
        var s = """
            #define FOO(/**/a/**/,/**/b/**/)
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("/**/", TokenType.COMMENT),
                pair("a", TokenType.IDENTIFIER),
                pair("/**/", TokenType.COMMENT),
                pair(",", TokenType.PUNCTUATOR),
                pair("/**/", TokenType.COMMENT),
                pair("b", TokenType.IDENTIFIER),
                pair("/**/", TokenType.COMMENT),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo));
        test(s, list);
    }

    @Test
    public void variadicMacroNotPrecededByComma() {
        var s = """
            #define FOO(x ...)
            """;
        var m = "L1:15: error: missing ',' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingCommaException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("..."));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(17));
        });
    }

    @Test
    public void missingIdentifierInMacroParameterList() {
        var s = """
            #define FOO(x,)
            """;
        var m = "L1:15: error: missing identifier in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingIdentifierException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(")"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(15));
        });
    }

    @Test
    public void invalidTokenInMacroParameterList() {
        var s = """
            #define FOO(,)
            """;
        var m = "L1:13: error: missing identifier in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingIdentifierException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(","));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(13));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(13));
        });
    }

    @Test
    public void missingParen1() {
        var s = """
            #define FOO(
            """;
        var m = "L1:13: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(13));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(13));
        });
    }

    @Test
    public void missingParen2() {
        var s = """
            #define FOO(x,
            """;
        var m = "L1:15: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(15));
        });
    }

    @Test
    public void missingParen3() {
        var s = """
            #define FOO(x
            """;
        var m = "L1:14: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(14));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(14));
        });
    }

    @Test
    public void variadicMacroMissingParen() {
        var s = """
            #define FOO(...
            """;
        var m = "L1:16: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(16));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(16));
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen1() {
        var s = """
            #define FOO(..., x)
            """;
        var m = "L1:16: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(","));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(16));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(16));
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen2() {
        var s = """
            #define FOO(... foo)
            """;
        var m = "L1:17: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("foo"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(17));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(19));
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen3() {
        var s = """
            #define FOO(... /**/ foo)
            """;
        var m = "L1:22: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("foo"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(22));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(24));
        });
    }

    @Test
    public void missingDefineMacroName() {
        var s = """
            #define
            """;
        var m = "L1:8: error: macro name missing";
        test(s, parser -> {
            var e = assertThrows(MissingMacroNameException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            assertThat(token.getType(), is(TokenType.DIRECTIVE_END));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(8));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(8));
        });
    }

    @Test
    public void missingUndefMacroName() {
        var s = """
            #undef
            """;
        var m = "L1:7: error: macro name missing";
        test(s, parser -> {
            var e = assertThrows(MissingMacroNameException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
            assertThat(token.getType(), is(TokenType.DIRECTIVE_END));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(7));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(7));
        });
    }

    @Test
    public void defineInvalidMacroName() {
        var s = """
            #define "FOO"
            """;
        var m = "L1:9: error: macro name must be an identifier";
        test(s, parser -> {
            var e = assertThrows(InvalidMacroNameException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\"FOO\""));
        });
    }

    @Test
    public void undefInvalidMacroName() {
        var s = """
            #undef "FOO"
            """;
        var m = "L1:8: error: macro name must be an identifier";
        test(s, parser -> {
            var e = assertThrows(InvalidMacroNameException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\"FOO\""));
        });
    }
}
