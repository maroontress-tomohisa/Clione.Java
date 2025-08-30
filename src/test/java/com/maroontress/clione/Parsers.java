package com.maroontress.clione;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
    Provides utility methods for parser tests.
*/
public final class Parsers {

    /**
        Returns a new consumer that asserts that a token has the specified
        value and type.

        @param value The specified value.
        @param type The specified type.
        @return A new consumer.
    */
    public static Consumer<Token> pair(String value, TokenType type) {
        return t -> {
            var actualValue = t.getValue();
            var actualType = t.getType();
            if (actualType != type || !actualValue.equals(value)) {
                System.out.println("expected (" + value + "," + type + ") but ("
                    + actualValue + "," + actualType + ")");
            }
            assertThat(actualValue, is(value));
            assertThat(actualType, is(type));
        };
    }

    /**
        Returns a new consumer that asserts that a token has the specified
        value, type, and children.

        @param value The specified value.
        @param type The specified type.
        @param childList The list of consumers for child tokens.
        @return A new consumer.
    */
    public static Consumer<Token> pair(String value, TokenType type,
            List<Consumer<Token>> childList) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
            var children = t.getChildren();
            var size = children.size();
            assertThat(size, is(childList.size()));
            for (var k = 0; k < size; ++k) {
                childList.get(k).accept(children.get(k));
            }
        };
    }

    /**
        Tests the preprocessor with the specified string and the list of
        token consumers.

        @param s The specified string.
        @param list The list of token consumers.
    */
    public static void test(String s, List<Consumer<Token>> list) {
        var cpp = new Preprocessor(LexicalParser.of(new StringReader(s)));
        var actualList = new ArrayList<Token>();
        try {
            for (;;) {
                var maybe = cpp.next();
                if (!maybe.isPresent()) {
                    break;
                }
                actualList.add(maybe.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        test(s, parser -> {
            for (var c : list) {
                var maybeToken = parser.next();
                assertThat("Expected a token, but found EOF.",
                        maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                c.accept(token);
            }
            assertThat("Expected EOF, but found more tokens.",
                    parser.next().isEmpty(), is(true));
        });
    }

    /**
        Tests the preprocessor with the specified string and the parser
        consumer.

        @param s The specified string.
        @param consumer The parser consumer.
    */
    public static void test(String s, ParserConsumer consumer) {
        var source = new StringReader(s);
        var parser = LexicalParser.of(source);
        test(consumer, () -> new Preprocessor(parser));
    }

    private static void test(
            ParserConsumer consumer, Supplier<LexicalParser> supplier) {
        try (var parser = supplier.get()) {
            consumer.accept(parser);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
        The functional interface that can accept a lexical parser and
        throw an exception.
    */
    @FunctionalInterface
    public interface ParserConsumer {
        /**
            Accepts a lexical parser.

            @param parser The lexical parser.
            @throws IOException if an I/O error occurs.
        */
        void accept(LexicalParser parser) throws IOException;
    }
}
