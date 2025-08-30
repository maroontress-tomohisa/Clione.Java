package com.maroontress.clione;

import static com.maroontress.clione.Parsers.test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.maroontress.clione.macro.DirectiveWithinMacroArgumentsException;
import com.maroontress.clione.macro.InvalidPreprocessingTokenException;
import com.maroontress.clione.macro.InvalidVariadicArgumentException;
import com.maroontress.clione.macro.MacroArgumentException;
import com.maroontress.clione.macro.UnterminatedMacroInvocationException;

public final class ExpansionErrorTest {

    @Test
    public void functionLikeMacroTooFewArguments() {
        var s = """
            #define ADD(a,b) (a+b)
            int x = ADD(1);
            """;
        test(s, parser -> {
            // Skips the tokens before "ADD":
            // 1. int
            // 2. <space>
            // 3. x
            // 4. <space>
            // 5. =
            // 6. <space>
            IntStream.range(0, 6).forEach(k -> {
                try {
                    parser.next();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            var e = assertThrows(MacroArgumentException.class, parser::next);
            var macroNameToken = e.getExpandingTokens().getFirst();
            assertThat(macroNameToken.getValue(), is("ADD"));
            {
                var span = macroNameToken.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(2));
                assertThat(start.getColumn(), is(9));
                assertThat(end.getLine(), is(2));
                assertThat(end.getColumn(), is(11));
            }
            assertThat(e.getExpected(), is(2));
            assertThat(e.getActual(), is(1));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(")"));
            {
                var span = token.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(2));
                assertThat(start.getColumn(), is(14));
                assertThat(end.getLine(), is(2));
                assertThat(end.getColumn(), is(14));
            }
        });
    }

    @Test
    public void functionLikeMacroTooManyArguments() {
        var s = """
            #define ADD(a,b) (a+b)
            int x = ADD(1,2,3);
            """;
        test(s, parser -> {
            // Skips the tokens before "ADD":
            // 1. int
            // 2. <space>
            // 3. x
            // 4. <space>
            // 5. =
            // 6. <space>
            IntStream.range(0, 6).forEach(k -> {
                try {
                    parser.next();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            var e = assertThrows(MacroArgumentException.class, parser::next);
            var macroNameToken = e.getExpandingTokens().getFirst();
            assertThat(macroNameToken.getValue(), is("ADD"));
            {
                var span = macroNameToken.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(2));
                assertThat(start.getColumn(), is(9));
                assertThat(end.getLine(), is(2));
                assertThat(end.getColumn(), is(11));
            }
            assertThat(e.getExpected(), is(2));
            assertThat(e.getActual(), is(3));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(","));
            {
                var span = token.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(2));
                assertThat(start.getColumn(), is(16));
                assertThat(end.getLine(), is(2));
                assertThat(end.getColumn(), is(16));
            }
        });
    }

    @Test
    public void variadicMacroEmpty() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            LOG("no value");
            """;
        test(s, parser -> {
            // Skips the tokens before "LOG":

            var e = assertThrows(InvalidVariadicArgumentException.class,
                parser::next);
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(")"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(2));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(2));
            assertThat(end.getColumn(), is(15));
        });
    }

    @Test
    public void variadicMacroWithTooFewArguments() {
        var s = """
            #define LOG(stream, format, ...) printf(stream, format, __VA_ARGS__)
            LOG(stdout);
            """;
        test(s, parser -> {
            // Skips the tokens before "LOG":

            var e = assertThrows(MacroArgumentException.class, parser::next);
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(")"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(2));
            assertThat(start.getColumn(), is(11));
            assertThat(end.getLine(), is(2));
            assertThat(end.getColumn(), is(11));
        });
    }

    @Test
    public void variadicMacroEmptyWithoutVaArgs() {
        var s = """
            #define LOG(format, ...) printf(format)
            LOG("no value");
            """;
        var m = """
            L2:15: error: passing no argument for the '...' parameter of a \
            variadic macro is a C23 extension\
            """;
        test(s, parser -> {
            // Skips the tokens before "LOG":

            var e = assertThrows(InvalidVariadicArgumentException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is(")"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(2));
            assertThat(start.getColumn(), is(15));
            assertThat(end.getLine(), is(2));
            assertThat(end.getColumn(), is(15));

            assertThat(e.getInvalidToken(), is(token));
        });
    }

    @Test
    public void nestedVariadicMacroEmpty() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            #define ERROR LOG("no value")
            ERROR
            """;
        var m = """
            L3:1: error: passing no argument for the '...' parameter of a \
            variadic macro is a C23 extension\
            """;
        test(s, parser -> {
            // Skips the tokens before "ERROR":

            var e = assertThrows(InvalidVariadicArgumentException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            {
                var token = e.getCauseToken();
                assertThat(token.getValue(), is("ERROR"));
                var span = token.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(3));
                assertThat(start.getColumn(), is(1));
                assertThat(end.getLine(), is(3));
                assertThat(end.getColumn(), is(5));
            }
            {
                var token = e.getInvalidToken();
                assertThat(token.getValue(), is(")"));
                var span = token.getSpan();
                var start = span.getStart();
                var end = span.getEnd();
                assertThat(start.getLine(), is(2));
                assertThat(start.getColumn(), is(29));
                assertThat(end.getLine(), is(2));
                assertThat(end.getColumn(), is(29));
            }
        });
    }

    @Test
    public void concatenationOperatorInvalidToken1() {
        var s = """
            #define CAT(a,b) a##b
            CAT(+, -)
            """;
        var m = """
            L2:1: error: pasting forms '+-', an invalid preprocessing token\
            """;
        test(s, parser -> {
            var e = assertThrows(InvalidPreprocessingTokenException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("CAT"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void concatenationOperatorInvalidToken2() {
        var s = """
            #define CAT(a,b) a##b
            CAT(#, define)
            """;
        var m = """
            L2:1: error: pasting forms '#define', an invalid preprocessing \
            token\
            """;
        test(s, parser -> {
            var e = assertThrows(InvalidPreprocessingTokenException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("CAT"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void concatenationOperatorInvalidToken3() {
        var s = """
            #define CAT(a,b) a##b
            CAT(123, !)
            """;
        var m = """
            L2:1: error: pasting forms '123!', an invalid preprocessing token\
            """;
        test(s, parser -> {
            var e = assertThrows(InvalidPreprocessingTokenException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("CAT"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void embeddingDirectiveWithinMacroArguments() {
        var s = """
            #define IGNORE(x)
            IGNORE(
            #
            )
            """;
        var m = """
            L3:1: error: embedding a directive within macro arguments has \
            undefined behavior\
            """;
        test(s, parser -> {
            var e = assertThrows(DirectiveWithinMacroArgumentsException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("#"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void unterminatedFunctionLikeMacro() {
        var s = """
            #define LOG(...) printf(__VA_ARGS__)
            LOG(
            """;
        var m = """
            L2:1: error: unterminated function-like macro invocation\
            """;
        test(s, parser -> {
            var e = assertThrows(UnterminatedMacroInvocationException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("LOG"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void recursiveUnterminatedFunctionLikeMacro() {
        var s = """
            #define LOG(...) printf(__VA_ARGS__)
            #define ERROR LOG(
            ERROR
            """;
        var m = """
            L3:1: error: unterminated function-like macro invocation\
            """;
        test(s, parser -> {
            var e = assertThrows(UnterminatedMacroInvocationException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("ERROR"));
            assertThat(e.getMessage(), is(m));
        });
    }
}
