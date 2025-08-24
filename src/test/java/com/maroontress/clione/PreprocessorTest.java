package com.maroontress.clione;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.maroontress.clione.macro.DirectiveWithinMacroArgumentsException;
import com.maroontress.clione.macro.InvalidPreprocessingTokenException;
import com.maroontress.clione.macro.InvalidVariadicArgumentException;
import com.maroontress.clione.macro.MacroArgumentException;
import com.maroontress.clione.macro.UnterminatedMacroInvocationException;
import static com.maroontress.clione.Parsers.pair;
import static com.maroontress.clione.Parsers.test;

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
    public void concatenationWithEmptyArgument() {
        var s = """
            #define CAT(a,b) a ## b
            CAT(foo,)
            CAT(,bar)
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
                pair(" ", TokenType.DELIMITER),
                pair("##", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("b", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("foo", TokenType.IDENTIFIER),
                pair("\n", TokenType.DELIMITER),
                pair("bar", TokenType.IDENTIFIER),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void stringizingAndExpansionOrder() {
        var s = """
            #define FOO BAR
            #define BAR 0
            #define STR(x) #x
            #define X(x) STR(x)
            X(FOO)
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
                pair("0", TokenType.NUMBER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineStr = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("STR", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.OPERATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineX = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("X", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("STR", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("#", TokenType.DIRECTIVE, defineBar),
                pair("#", TokenType.DIRECTIVE, defineStr),
                pair("#", TokenType.DIRECTIVE, defineX),
                pair("\"0\"", TokenType.STRING),
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
    public void circularObjectLikeMacros() {
        var s = """
            #define FOO BAR
            #define BAR FOO
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
                pair("FOO", TokenType.IDENTIFIER),
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
                pair("FOO", TokenType.IDENTIFIER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void circularObjectAndFunctionLikeMacros() {
        var s = """
            #define FOO(x) BAR
            #define BAR FOO(1)
            int x = FOO(0);
            """;
        var defineFoo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineBar = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
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
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("1", TokenType.NUMBER),
                pair(")", TokenType.PUNCTUATOR),
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
    public void stringizingOperatorWithComment() {
        var s = """
            #define FOO(x) # /**/ x
            FOO(bar)
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("\"bar\"", TokenType.STRING),
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
    public void concatenationWithComments() {
        var s = """
            #define FOO(x) foo_ /**/ ## /**/ x
            FOO(bar)
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("foo_", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("##", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("foo_bar", TokenType.IDENTIFIER),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationAndExpansionOrder1() {
        var s = """
            #define HE HI
            #define LLO _THERE
            #define HELLO "HI THERE"
            #define CAT(a,b) a##b
            CAT(HE, LLO);
            """;
        var defineHe = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HE", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("HI", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineLlo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LLO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("_THERE", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineHello = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HELLO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("\"HI THERE\"", TokenType.STRING),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineCat = List.of(
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
                pair("#", TokenType.DIRECTIVE, defineHe),
                pair("#", TokenType.DIRECTIVE, defineLlo),
                pair("#", TokenType.DIRECTIVE, defineHello),
                pair("#", TokenType.DIRECTIVE, defineCat),
                pair("\"HI THERE\"", TokenType.STRING),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationAndExpansionOrder2() {
        var s = """
            #define HE HI
            #define LLO _THERE
            #define CAT(a,b) a##b
            #define XCAT(a,b) CAT(a,b)
            XCAT(HE, LLO);
            """;
        var defineHe = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HE", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("HI", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineLlo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LLO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("_THERE", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineCat = List.of(
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
        var defineXcat = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("XCAT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("CAT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineHe),
                pair("#", TokenType.DIRECTIVE, defineLlo),
                pair("#", TokenType.DIRECTIVE, defineCat),
                pair("#", TokenType.DIRECTIVE, defineXcat),
                pair("HI_THERE", TokenType.IDENTIFIER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationAndExpansionOrder3() {
        var s = """
            #define HE(x) HI##x
            #define LLO(x) _THERE##x
            #define CAT(a,b) a##b
            #define XCAT(a,b) CAT(a,b)
            XCAT(HE(1), LLO(2));
            """;
        var defineHe = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HE", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("HI", TokenType.IDENTIFIER),
                pair("##", TokenType.OPERATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineLlo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LLO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("_THERE", TokenType.IDENTIFIER),
                pair("##", TokenType.OPERATOR),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineCat = List.of(
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
        var defineXcat = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("XCAT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("CAT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("a", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("b", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineHe),
                pair("#", TokenType.DIRECTIVE, defineLlo),
                pair("#", TokenType.DIRECTIVE, defineCat),
                pair("#", TokenType.DIRECTIVE, defineXcat),
                pair("HI1_THERE2", TokenType.IDENTIFIER),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void concatenationAndExpansionOrder4() {
        var s = """
            #define HE HI
            #define LLO _THERE
            #define HELLO "HI THERE"
            #define CAT(a,b) a##b
            #define CALL(fn) fn(HE,LLO)
            CALL(CAT);
            """;
        var defineHe = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HE", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("HI", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineLlo = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("LLO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("_THERE", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineHello = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("HELLO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("\"HI THERE\"", TokenType.STRING),
                pair("\n", TokenType.DIRECTIVE_END));
        var defineCat = List.of(
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
        var defineCall = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("CALL", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("fn", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("fn", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("HE", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair("LLO", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, defineHe),
                pair("#", TokenType.DIRECTIVE, defineLlo),
                pair("#", TokenType.DIRECTIVE, defineHello),
                pair("#", TokenType.DIRECTIVE, defineCat),
                pair("#", TokenType.DIRECTIVE, defineCall),
                pair("\"HI THERE\"", TokenType.STRING),
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
            // 1. #define LOG(format, ...) printf(format, __VA_ARGS__)
            for (int i = 0; i < 1; i++) {
                parser.next();
            }

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
        // CHECKSTYLE:OFF LineLength
        var m = """
            L2:15: error: passing no argument for the '...' parameter of a variadic macro is a C23 extension""";
        // CHECKSTYLE:ON LineLength
        test(s, parser -> {
            // Skips the tokens before "LOG":
            // 1. #define LOG(format, ...) printf(format)
            for (int i = 0; i < 1; i++) {
                parser.next();
            }

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
        // CHECKSTYLE:OFF LineLength
        var m = """
            L3:1: error: passing no argument for the '...' parameter of a variadic macro is a C23 extension""";
        // CHECKSTYLE:ON LineLength
        test(s, parser -> {
            // Skips the tokens before "ERROR":
            // 1. #define LOG(...)
            // 2. #define ERROR ...
            for (int i = 0; i < 2; i++) {
                parser.next();
            }

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
    public void variadicMacroOnlyWithVaArgs() {
        var s = """
            #define FOO(...) __VA_ARGS__
            FOO()
            int main(FOO(int ac, char **av)) {}
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("__VA_ARGS__", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("\n", TokenType.DELIMITER),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("main", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("ac", TokenType.IDENTIFIER),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("char", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("*", TokenType.OPERATOR),
                pair("*", TokenType.OPERATOR),
                pair("av", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("{", TokenType.PUNCTUATOR),
                pair("}", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void variadicMacroOnlyWithoutVaArgs() {
        var s = """
            #define FOO(...) void
            FOO() func(void);
            int main(FOO(int ac, char **av)) {}
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("...", TokenType.PUNCTUATOR),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("void", TokenType.RESERVED),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("void", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("func", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("void", TokenType.RESERVED),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER),
                pair("int", TokenType.RESERVED),
                pair(" ", TokenType.DELIMITER),
                pair("main", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("void", TokenType.RESERVED),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("{", TokenType.PUNCTUATOR),
                pair("}", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
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
        var m = """
            L2:1: error: pasting forms '+-', an invalid preprocessing token""";
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

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
            L2:1: error: pasting forms '#define', an invalid preprocessing token""";
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

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
            L2:1: error: pasting forms '123!', an invalid preprocessing token""";
        test(s, parser -> {
            // Skip #define CAT(a,b) a##b
            parser.next();

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
            L3:1: error: embedding a directive within macro arguments has undefined behavior""";
        test(s, parser -> {
            // Skip #define IGNORE(x)
            parser.next();

            var e = assertThrows(DirectiveWithinMacroArgumentsException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("#"));
            assertThat(e.getMessage(), is(m));
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

    @Test
    public void unterminatedFunctionLikeMacro() {
        var s = """
            #define LOG(...) printf(__VA_ARGS__)
            LOG(
            """;
        var m = """
            L2:1: error: unterminated function-like macro invocation""";
        test(s, parser -> {
            // Skips the tokens before "LOG":
            // 1. #define LOG(...) printf(__VA_ARGS__)
            parser.next();

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
            L3:1: error: unterminated function-like macro invocation""";
        test(s, parser -> {
            // Skips the tokens before "ERROR":
            // 1. #define LOG(...) printf(__VA_ARGS__)
            // 2. #define ERROR LOG(
            for (var k = 0; k < 2; ++k) {
                parser.next();
            }
            var e = assertThrows(UnterminatedMacroInvocationException.class,
                    parser::next);
            assertThat(e.getCauseToken().getValue(), is("ERROR"));
            assertThat(e.getMessage(), is(m));
        });
    }

    @Test
    public void stringizingWithObjectLikeMacro() {
        var s = """
            /**/ # /**/ define /**/ FOO /**/ BAR /**/ ( /**/ 0 /**/ ) /**/
            /**/ # /**/ define /**/ BAR( /**/ x /**/ ) /**/ # /**/ x /**/
            FOO
            """;
        var defineFoo = List.of(
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("FOO", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("0", TokenType.NUMBER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair("\n", TokenType.DIRECTIVE_END));

        var defineBar = List.of(
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("BAR", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.OPERATOR),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("/**/", TokenType.COMMENT),
                pair("\n", TokenType.DIRECTIVE_END));

        var list = List.of(
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.DIRECTIVE, defineFoo),
                pair("/**/", TokenType.COMMENT),
                pair(" ", TokenType.DELIMITER),
                pair("#", TokenType.DIRECTIVE, defineBar),
                pair("\"0\"", TokenType.STRING),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }

    @Test
    public void functionLikeMacroWithParenthesizedArgument() {
        var s = """
            #define PRINT(x) printf x
            PRINT(("%s", "hello"));
            """;
        var define = List.of(
                pair("define", TokenType.DIRECTIVE_NAME),
                pair(" ", TokenType.DELIMITER),
                pair("PRINT", TokenType.IDENTIFIER),
                pair("(", TokenType.PUNCTUATOR),
                pair("x", TokenType.IDENTIFIER),
                pair(")", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("printf", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("x", TokenType.IDENTIFIER),
                pair("\n", TokenType.DIRECTIVE_END));
        var list = List.of(
                pair("#", TokenType.DIRECTIVE, define),
                pair("printf", TokenType.IDENTIFIER),
                pair(" ", TokenType.DELIMITER),
                pair("(", TokenType.PUNCTUATOR),
                pair("\"%s\"", TokenType.STRING),
                pair(",", TokenType.PUNCTUATOR),
                pair(" ", TokenType.DELIMITER),
                pair("\"hello\"", TokenType.STRING),
                pair(")", TokenType.PUNCTUATOR),
                pair(";", TokenType.PUNCTUATOR),
                pair("\n", TokenType.DELIMITER));
        test(s, list);
    }
}
