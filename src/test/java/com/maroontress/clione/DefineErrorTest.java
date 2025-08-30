package com.maroontress.clione;

import static com.maroontress.clione.Parsers.test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.maroontress.clione.macro.InvalidConcatenationOperatorException;
import com.maroontress.clione.macro.InvalidMacroNameException;
import com.maroontress.clione.macro.InvalidStringizingOperatorException;
import com.maroontress.clione.macro.VaArgsKeywordMisusageException;
import com.maroontress.clione.macro.parameter.InvalidMacroParameterTokenException;
import com.maroontress.clione.macro.parameter.MissingCommaInMacroParameterListException;
import com.maroontress.clione.macro.parameter.MissingMacroParameterException;
import com.maroontress.clione.macro.parameter.MissingParenInMacroParameterListException;
import com.maroontress.clione.macro.MissingMacroNameException;
import com.maroontress.clione.macro.MissingWhitespaceAfterMacroName;

public final class DefineErrorTest {

    @Test
    public void invalidMacroParameterToken() {
        var s = """
            #define FOO(x,+)
            """;
        var m = "L1:15: error: invalid token in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(InvalidMacroParameterTokenException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("+"));
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
    public void vaArgsAsParameter() {
        var s = """
            #define FOO(__VA_ARGS__)
            """;
        var m = """
            L1:13: error: __VA_ARGS__ can only appear in the expansion of a C99 variadic macro""";
        test(s, parser -> {
            var e = assertThrows(VaArgsKeywordMisusageException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("__VA_ARGS__"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(13));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(23));
        });
    }

    @Test
    public void missingWhitespaceAfterMacroName() {
        var s = """
            #define FOO-1
            """;
        var m = """
            L1:12: error: ISO C99 requires whitespace after the macro name""";
        test(s, parser -> {
            var e = assertThrows(MissingWhitespaceAfterMacroName.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("-"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(12));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(12));
        });
    }

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
    public void invalidStringizingOperatorAtEndOfLine() {
        var s = """
            #define FOO(x) #
            """;
        var m = """
            L1:17: error: '#' is not followed by a macro parameter""";
        test(s, parser -> {
            var e = assertThrows(InvalidStringizingOperatorException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\n"));
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
            var e = assertThrows(MissingCommaInMacroParameterListException.class, parser::next);
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
    public void variadicMacroNotPrecededByComma() {
        var s = """
            #define FOO(x ...)
            """;
        var m = "L1:15: error: missing ',' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingCommaInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingMacroParameterException.class, parser::next);
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
            var e = assertThrows(MissingMacroParameterException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
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

    @Test
    public void vaArgsInObjectLikeMacro() {
        var s = """
            #define FOO __VA_ARGS__
            """;
        var m = """
            L1:13: error: __VA_ARGS__ can only appear in the expansion of a C99 variadic macro""";
        test(s, parser -> {
            var e = assertThrows(VaArgsKeywordMisusageException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("__VA_ARGS__"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(13));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(23));
        });
    }

    @Test
    public void vaArgsInFunctionLikeMacro() {
        var s = """
            #define FOO(x) __VA_ARGS__
            """;
        var m = """
            L1:16: error: __VA_ARGS__ can only appear in the expansion of a C99 variadic macro""";
        test(s, parser -> {
            var e = assertThrows(VaArgsKeywordMisusageException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("__VA_ARGS__"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(16));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(26));
        });
    }

    @Test
    public void macroNameIsVaArg() {
        var s = """
            #define __VA_ARGS__
            """;
        var m = """
            L1:9: error: __VA_ARGS__ can only appear in the expansion of a C99 variadic macro""";
        test(s, parser -> {
            var e = assertThrows(VaArgsKeywordMisusageException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("__VA_ARGS__"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(9));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(19));
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen4() {
        var s = """
            #define FOO(... ...)
            """;
        var m = "L1:17: error: missing ')' in macro parameter list";
        test(s, parser -> {
            var e = assertThrows(MissingParenInMacroParameterListException.class, parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("..."));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(17));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(19));
        });
    }
}
