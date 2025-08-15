package com.maroontress.clione;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void circularMacro() {
        var s = """
            #define FOO BAR
            #define BAR FOO
            int x = FOO;
            """;
        test(s, parser -> {
            // Skips the tokens before "FOO":
            // 1. #define FOO BAR
            // 2. #define BAR FOO
            // 3. int
            // 4. <space>
            // 5. x
            // 6. <space>
            // 7. =
            // 8. <space>
            for (int i = 0; i < 8; i++) {
                parser.next();
            }

            var e = assertThrows(CircularMacroException.class, parser::next);

            assertEquals("FOO", e.getMacroName());
            var token = e.getCauseToken();
            assertEquals("FOO", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(3, start.getLine());
            assertEquals(9, start.getColumn());
            assertEquals(3, end.getLine());
            assertEquals(11, end.getColumn());
            assertEquals("FOO -> BAR -> FOO", e.getCyclePath());
        });
    }

    @Test
    public void missingCommaInMacroParameterList() {
        var s = """
            #define FOO(a b)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingCommaException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("b", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(15, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(15, end.getColumn());
        });
    }

    @Test
    public void variadicMacroNotPrecededByComma() {
        var s = """
            #define FOO(x ...)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingCommaException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("...", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(15, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(17, end.getColumn());
        });
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
    public void functionLikeMacroEmptyArgument() {
        var s = """
            #define FOO(x) (x+1)
            int x = FOO();
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
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

    @Test
    public void functionLikeMacroEmptyFirstArgument() {
        var s = """
            #define BAR(x,y) (x+y)
            int x = BAR(,2);
            """;
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
                pair("x", TokenType.IDENTIFIER),
                pair("+", TokenType.OPERATOR),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineBar),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("+", TokenType.OPERATOR),
                pair("2", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroEmptySecondArgument() {
        var s = """
            #define BAZ(x,y) (x y)
            int x = BAZ(3,);
            """;
        var defineBaz = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("BAZ", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("y", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineBaz),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("=", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("3", TokenType.NUMBER),
                pair(" ", TokenType.DELIMITER),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroTooFewArguments() {
        var s = """
            #define ADD(a,b) (a+b)
            int x = ADD(1);
            """;
        test(s, parser -> {
            // Skips the tokens before "ADD":
            // 1. #define ADD(a,b) (a+b)
            // 2. int
            // 3. <space>
            // 4. x
            // 5. <space>
            // 6. =
            // 7. <space>
            for (int i = 0; i < 7; i++) {
                parser.next();
            }

            var e = assertThrows(MacroArgumentException.class, parser::next);
            assertEquals("ADD", e.getMacroName());
            assertEquals(2, e.getExpected());
            assertEquals(1, e.getActual());
            var token = e.getCauseToken();
            assertEquals("ADD", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(2, start.getLine());
            assertEquals(9, start.getColumn());
            assertEquals(2, end.getLine());
            assertEquals(11, end.getColumn());
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
            // 1. #define ADD(a,b) (a+b)
            // 2. int
            // 3. <space>
            // 4. x
            // 5. <space>
            // 6. =
            // 7. <space>
            for (int i = 0; i < 7; i++) {
                parser.next();
            }

            var e = assertThrows(MacroArgumentException.class, parser::next);
            assertEquals("ADD", e.getMacroName());
            assertEquals(2, e.getExpected());
            assertEquals(3, e.getActual());
            var token = e.getCauseToken();
            assertEquals("ADD", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(2, start.getLine());
            assertEquals(9, start.getColumn());
            assertEquals(2, end.getLine());
            assertEquals(11, end.getColumn());
        });
    }

    @Test
    public void variadicMacro() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            LOG("value is %d", 123);
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LOG", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("format", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("format", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                // (*)
                pair(" ", TokenType.DELIMITER),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"value is %d\"", TokenType.STRING),
                pair(",", TokenType.PUNCTUATOR),
                // (*)
                pair(" ", TokenType.DELIMITER),
                // This DELIMITER is contained in the arguments
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void variadicMacroStringify() {
        var s = """
            #define showlist(...) puts(#__VA_ARGS__)
            showlist(1, "x", int);
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("showlist", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("puts", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("#", TokenType.OPERATOR),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("puts", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"1, \\\"x\\\", int\"", TokenType.STRING),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void variadicMacroStringifyEmpty() {
        var s = """
            #define showlist(...) puts(#__VA_ARGS__)
            showlist();
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("showlist", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("puts", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("#", TokenType.OPERATOR),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("puts", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"\"", TokenType.STRING),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void missingIdentifierInMacroParameterList() {
        var s = """
            #define FOO(x,)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingIdentifierException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals(")", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(15, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(15, end.getColumn());
        });
    }

    @Test
    public void invalidTokenInMacroParameterList() {
        var s = """
            #define FOO(,)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingIdentifierException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals(",", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(13, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(13, end.getColumn());
        });
    }

    @Test
    public void missingParen1() {
        var s = """
            #define FOO(
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("\n", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(13, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(13, end.getColumn());
        });
    }

    @Test
    public void missingParen2() {
        var s = """
            #define FOO(x,
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("\n", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(15, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(15, end.getColumn());
        });
    }

    @Test
    public void missingParen3() {
        var s = """
            #define FOO(x
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("\n", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(14, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(14, end.getColumn());
        });
    }

    @Test
    public void variadicMacroMissingParen() {
        var s = """
            #define FOO(...
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("\n", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(16, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(16, end.getColumn());
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen1() {
        var s = """
            #define FOO(..., x)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals(",", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(16, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(16, end.getColumn());
        });
    }

    @Test
    public void variadicMacroNotFollowedByParen2() {
        var s = """
            #define FOO(... foo)
            """;
        test(s, parser -> {
            var e = assertThrows(MissingParenException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals("foo", token.getValue());
            var span = token.getSpan();
            var start = span.getStart();
            var end = span.getEnd();
            assertEquals(1, start.getLine());
            assertEquals(17, start.getColumn());
            assertEquals(1, end.getLine());
            assertEquals(19, end.getColumn());
        });
    }

    @Test
    public void variadicMacro2() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            LOG("value is %d (%s)", 123, "NUMBER");
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LOG", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("format", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("format", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                // (*)
                pair(" ", TokenType.DELIMITER),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"value is %d (%s)\"", TokenType.STRING),
                pair(",", TokenType.PUNCTUATOR),
                // (*)
                pair(" ", TokenType.DELIMITER),
                // This DELIMITER is contained in the arguments
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"NUMBER\"", TokenType.STRING),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void variadicMacroEmpty() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            LOG("no value");
            """;
        test(s, parser -> {
            // Skips the tokens before "LOG":
            // 1. #define LOG(format, ...) printf(format, __VA_ARGS__)
            for (int i = 0; i < 1; i++) {
                parser.next();
            }

            var e = assertThrows(InvalidVariadicArgumentException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals(",", token.getValue());
        });
    }

    @Test
    public void nestedVariadicMacroEmpty() {
        var s = """
            #define LOG(format, ...) printf(format, __VA_ARGS__)
            #define ERROR LOG("no value")
            ERROR
            """;
        test(s, parser -> {
            // Skips the tokens before "ERROR":
            // 1. #define LOG(...)
            // 2. #define ERROR ...
            for (int i = 0; i < 2; i++) {
                parser.next();
            }

            var e = assertThrows(InvalidVariadicArgumentException.class, parser::next);
            var token = e.getCauseToken();
            assertEquals(",", token.getValue());
        });
    }

    @Test
    public void variadicMacroOnly() {
        var s = """
            #define PRINTF(...) printf(__VA_ARGS__)
            PRINTF("%d", 123);
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("PRINTF", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("printf", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"%d\"", TokenType.STRING),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("123", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void macroParen() {
        var s = """
            #define LEFT (
            #define RIGHT )
            int main LEFT RIGHT {}
            """;
        var defineLeft = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LEFT", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineRight = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("RIGHT", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineLeft),
                pair("#", TokenType.DIRECTIVE, defineRight),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("main", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("{", TokenType.PUNCTUATOR),
                pair("}", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroWithSpace() {
        var s = """
            #define FOO(x) (x+1)
            FOO (1)
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationOperatorInvalidToken1() {
        var s = """
            #define CAT(a,b) a##b
            CAT(+, -)
            """;
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

            var e = assertThrows(InvalidPreprocessingTokenException.class, parser::next);
            assertEquals("CAT", e.getCauseToken().getValue());
            assertEquals("""
                L2:1: error: pasting forms '+-', an invalid preprocessing token""",
                e.getMessage());
        });
    }

    @Test
    public void concatenationOperatorInvalidToken2() {
        var s = """
            #define CAT(a,b) a##b
            CAT(#, define)
            """;
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

            var e = assertThrows(InvalidPreprocessingTokenException.class, parser::next);
            assertEquals("CAT", e.getCauseToken().getValue());
            assertEquals("""
                L2:1: error: pasting forms '#define', an invalid preprocessing token""",
                e.getMessage());
        });
    }

    @Test
    public void concatenationOperatorInvalidToken3() {
        var s = """
            #define CAT(a,b) a##b
            CAT(123, !)
            """;
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

            var e = assertThrows(InvalidPreprocessingTokenException.class, parser::next);
            assertEquals("CAT", e.getCauseToken().getValue());
            assertEquals("""
                L2:1: error: pasting forms '123!', an invalid preprocessing token""",
                e.getMessage());
        });
    }

    @Test
    public void functionLikeMacroWithComment() {
        var s = """
            #define FOO(x) (x+1)
            FOO/**/(2)
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("(", TokenType.PUNCTUATOR),
                pair("2", TokenType.NUMBER),
                pair("+", TokenType.OPERATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
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
        var parser = LexicalParser.of(source);
        test(consumer, () -> new Preprocessor(parser));
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
            var actualValue = t.getValue();
            var actualType = t.getType();
            if (actualType != type || !actualValue.equals(value)) {
                System.out.println("expected (" + value + "," + type + ") but ("
                    + actualValue + "," + actualType + ")");
            }
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
