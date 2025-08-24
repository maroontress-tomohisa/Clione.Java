package com.maroontress.clione;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import com.maroontress.clione.macro.InvalidPreprocessingDirectiveException;
import static com.maroontress.clione.Parsers.test;

public final class DirectiveErrorTest {

    @Test
    public void invalidDirectiveToken() {
        var s = """
            # "HELLO"
            """;
        var m = """
            L1:3: error: '"HELLO"' is invalid preprocessing directive""";
        test(s, parser -> {
            var e = assertThrows(InvalidPreprocessingDirectiveException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("\"HELLO\""));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(3));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(9));
        });
    }

    @Test
    public void invalidDirectiveName() {
        var s = """
            # hello
            """;
        var m = """
            L1:3: error: 'hello' is invalid preprocessing directive""";
        test(s, parser -> {
            var e = assertThrows(InvalidPreprocessingDirectiveException.class,
                    parser::next);
            assertThat(e.getMessage(), is(m));
            var token = e.getCauseToken();
            assertThat(token.getValue(), is("hello"));
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertThat(start.getLine(), is(1));
            assertThat(start.getColumn(), is(3));
            assertThat(end.getLine(), is(1));
            assertThat(end.getColumn(), is(7));
        });
    }
}
