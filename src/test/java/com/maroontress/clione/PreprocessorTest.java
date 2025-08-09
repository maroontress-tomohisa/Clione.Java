package com.maroontress.clione;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public final class PreprocessorTest {

    @Test
    public void simpleMacro() {
        var s = """
            #define FOO 123
            int x = FOO;
            #undef FOO
            int y = FOO;
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var undefFoo = List.of(
                pair("undef", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER),
                pair("#", TokenType.DIRECTIVE, undefFoo),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("y", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void simpleRecursiveMacro() {
        var s = """
            #define FOO BAR
            #define BAR 123
            int x = FOO;
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineBar = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("#", TokenType.DIRECTIVE, defineBar),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacro() {
        var s = """
            #define ADD(a,b) (a+b)
            int x = ADD(1,2);
            """;
        var defineAdd = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("ADD", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair("+", TokenType.OPERATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineAdd),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
                pair("+", TokenType.OPERATOR),
                pair("2", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeRecursiveMacro() {
        var s = """
            #define FOO(a,b) BAR((a)+(b),(a)*(b))
            #define BAR(x,y) ((x)+(y))
            int x = FOO(1,2);
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(",", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("*", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineBar = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("#", TokenType.DIRECTIVE, defineBar),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("2", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("*", TokenType.OPERATOR),
                pair("(", TokenType.PUNCTUATOR),
                pair("2", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroNoArguments() {
        var s = """
            #define F() 1
            int x = F();
            """;
        var defineF = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("F", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("1", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineF),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("1", TokenType.NUMBER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroNotExpanded() {
        var s = """
            #define F() 1
            int F = 2;
            """;
        var defineF = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("F", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("1", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineF),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("F", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("2", TokenType.NUMBER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void stringizingOperator() {
        var s = """
            #define STRINGIZE(x) #x
            const char *s = STRINGIZE(foo bar);
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("STRINGIZE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.OPERATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("const", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("s", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"foo bar\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationOperator() {
        var s = """
            #define CAT(a,b) a##b
            int foobar = 123;
            int x = CAT(foo, bar);
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("CAT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("a", TokenType.IDENTIFIER),
                pair("##", TokenType.OPERATOR),
                pair("b", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("foobar", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("foobar", TokenType.IDENTIFIER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    private static void test(final String s, final List<Consumer<Token>> list) {
        test(s, parser -> {
            for (var c : list) {
                var maybeToken = parser.next();
                assertThat("Expected a token, but found EOF.", maybeToken.isPresent(), is(true));
                var token = maybeToken.get();
                c.accept(token);
            }
            assertThat("Expected EOF, but found more tokens.", parser.next().isEmpty(), is(true));
        });
    }

    private static void test(final String s, final ParserConsumer consumer) {
        var source = new StringReader(s);
        test(consumer, () -> LexicalParser.preprocessorOf(source));
    }

    private static void test(final ParserConsumer consumer,
                             final Supplier<LexicalParser> supplier) {
        try (var parser = supplier.get()) {
            consumer.accept(parser);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Consumer<Token> pair(final String value, final TokenType type) {
        return t -> {
            assertThat(t.getValue(), is(value));
            assertThat(t.getType(), is(type));
        };
    }

    private static Consumer<Token> pair(final String value, final TokenType type,
                                        final List<Consumer<Token>> childList) {
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

    @FunctionalInterface
    public interface ParserConsumer {
        void accept(LexicalParser parser) throws IOException;
    }
}
