package com.maroontress.clione;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
    Represents a function-like preprocessor macro.
*/
public final class FunctionLikeMacro implements Macro {

    private static final MacroBehavior DEFAULT_BEHAVIOR
        = new DefaultMacroBehavior();
    private static final MacroBehavior VARIADIC_BEHAVIOR
        = new VariadicMacroBehavior();

    private final String name;
    private final List<String> parameters;
    private final List<Token> body;
    private final MacroBehavior behavior;

    /**
        Creates a new instance.

        @param name The name of the macro.
        @param parameters The list of parameter names.
        @param isVariadic {@code true} if the macro is variadic, {@code false}
        otherwise.
        @param body The list of tokens that form the macro's body.
    */
    public FunctionLikeMacro(String name,
                             List<String> parameters,
                             boolean isVariadic,
                             List<Token> body) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.body = List.copyOf(body);
        this.behavior = isVariadic
            ? VARIADIC_BEHAVIOR
            : DEFAULT_BEHAVIOR;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> parameters() {
        return parameters;
    }

    @Override
    public List<Token> body() {
        return body;
    }

    @Override
    public boolean apply(Preprocessor preprocessor, Token token)
            throws IOException {
        var openParen = preprocessor.lookAheadForParen();
        if (openParen.isPresent()) {
            var args = preprocessor.parseArguments(this);
            preprocessor.getExpandingMacros()
                .put(name(), token);
            preprocessor.getTokenQueue()
                .addFirst(new Preprocessor.MacroEndMarker(name()));
            preprocessor.prependTokens(preprocessor.substitute(this, token, args));
            return true;
        }
        return false;
    }

    @Override
    public Map<String, List<Token>> getSubstitutionMapping(
            List<List<Token>> args, Preprocessor preprocessor)
            throws PreprocessException {
        for (var tokenList : args) {
            if (tokenList.size() == 0) {
                continue;
            }
            var maybeFirst = tokenList.stream()
                    .filter(t -> t.getType() == TokenType.DIRECTIVE)
                    .findFirst();
            if (maybeFirst.isPresent()) {
                throw new DirectiveWithinMacroArgumentsException(
                    "embedding a directive within macro arguments has undefined behavior",
                    maybeFirst.get(),
                    List.copyOf(preprocessor.getExpandingMacros().values()));
            }
        }
        return behavior.getSubstitutionMapping(this, args, preprocessor);
    }

    @Override
    public List<List<Token>> parseArguments(Preprocessor preprocessor)
            throws IOException {
        var builder = behavior.createArgumentBuilder(this);
        while (builder.getParenLevel() > 0) {
            var nextToken = preprocessor.next();
            if (nextToken.isEmpty()) {
                break;
            }
            var token = nextToken.get();
            builder.addToken(token);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (FunctionLikeMacro) o;
        return Objects.equals(name, that.name)
            && Objects.equals(parameters, that.parameters)
            && Objects.equals(body, that.body)
            && Objects.equals(behavior, that.behavior);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, parameters, body, behavior);
    }

    @Override
    public String toString() {
        return "FunctionLikeMacro{"
            + "name='" + name + '\''
            + ", parameters=" + parameters
            + ", behavior=" + behavior.getClass().getSimpleName()
            + ", body=" + body
            + '}';
    }

    private Map<String, List<Token>> getVariadicSubstitutionMapping(
            List<List<Token>> args, Preprocessor preprocessor)
            throws PreprocessException {
        var params = parameters();
        var expectedSize = params.size();
        var actualSize = args.size();
        if (expectedSize > actualSize) {
            throw new MacroArgumentException(name(), expectedSize, actualSize,
                    List.copyOf(preprocessor.getExpandingMacros().values()));
        }
        var mapping = new HashMap<String, List<Token>>();
        for (var k = 0; k < expectedSize; ++k) {
            mapping.put(params.get(k), args.get(k));
        }
        // The last argument list contains all the variadic arguments.
        var vaArgs = (expectedSize < actualSize)
            ? args.get(actualSize - 1)
            : new ArrayList<Token>();
        mapping.put("__VA_ARGS__", vaArgs);
        return mapping;
    }

    private interface MacroBehavior {

        Map<String, List<Token>> getSubstitutionMapping(
                FunctionLikeMacro macro, List<List<Token>> args,
                Preprocessor preprocessor) throws PreprocessException;

        ArgumentBuilder createArgumentBuilder(FunctionLikeMacro macro);
    }

    private static final class DefaultMacroBehavior implements MacroBehavior {
        @Override
        public Map<String, List<Token>> getSubstitutionMapping(
                FunctionLikeMacro macro, List<List<Token>> args,
                Preprocessor preprocessor) throws PreprocessException {
            return macro.getDefaultSubstitutionMapping(args, preprocessor);
        }

        @Override
        public ArgumentBuilder createArgumentBuilder(
                FunctionLikeMacro macro) {
            return new DefaultArgumentBuilder(macro);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DefaultMacroBehavior;
        }

        @Override
        public int hashCode() {
            return DefaultMacroBehavior.class.hashCode();
        }
    }

    private static final class VariadicMacroBehavior implements MacroBehavior {
        @Override
        public Map<String, List<Token>> getSubstitutionMapping(
                FunctionLikeMacro macro, List<List<Token>> args,
                Preprocessor preprocessor) throws PreprocessException {
            return macro.getVariadicSubstitutionMapping(args, preprocessor);
        }

        @Override
        public ArgumentBuilder createArgumentBuilder(
                FunctionLikeMacro macro) {
            return new VariadicArgumentBuilder(macro);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof VariadicMacroBehavior;
        }

        @Override
        public int hashCode() {
            return VariadicMacroBehavior.class.hashCode();
        }
    }

    private abstract static class ArgumentBuilder {
        private final FunctionLikeMacro macro;
        private final List<List<Token>> args = new ArrayList<>();
        private final List<Token> currentArg = new ArrayList<>();
        private int parenLevel = 1;

        ArgumentBuilder(FunctionLikeMacro macro) {
            this.macro = macro;
        }

        protected FunctionLikeMacro getMacro() {
            return macro;
        }

        protected List<List<Token>> getArgs() {
            return args;
        }

        protected List<Token> getCurrentArg() {
            return currentArg;
        }

        public int getParenLevel() {
            return parenLevel;
        }

        protected void setParenLevel(int parenLevel) {
            this.parenLevel = parenLevel;
        }

        public abstract List<List<Token>> build();

        public abstract void addToken(Token token);

        protected boolean consumeParen(Token token) {
            if (!token.getValue().equals(")") || getParenLevel() != 1) {
                return false;
            }

            setParenLevel(getParenLevel() - 1);
            if (!getArgs().isEmpty() || !getCurrentArg().isEmpty()) {
                getArgs().add(new ArrayList<>(getCurrentArg()));
            }
            return true;
        }

        protected void updateParenLevel(Token token) {
            if (token.getType() != TokenType.PUNCTUATOR) {
                return;
            }
            switch (token.getValue()) {
            case "(":
                setParenLevel(getParenLevel() + 1);
                break;
            case ")":
                setParenLevel(getParenLevel() - 1);
                break;
            default:
                // do nothing
                break;
            }
        }
    }

    private static final class DefaultArgumentBuilder extends ArgumentBuilder {
        DefaultArgumentBuilder(FunctionLikeMacro macro) {
            super(macro);
        }

        @Override
        public List<List<Token>> build() {
            if (getMacro().parameters().size() == 1 && getArgs().isEmpty()) {
                getArgs().add(new ArrayList<>());
            }
            return getArgs();
        }

        @Override
        public void addToken(Token token) {
            if (consumeParen(token)) {
                return;
            }

            if (token.getValue().equals(",") && getParenLevel() == 1) {
                getArgs().add(new ArrayList<>(getCurrentArg()));
                getCurrentArg().clear();
                return;
            }

            updateParenLevel(token);
            getCurrentArg().add(token);
        }
    }

    private static final class VariadicArgumentBuilder extends ArgumentBuilder {
        VariadicArgumentBuilder(FunctionLikeMacro macro) {
            super(macro);
        }

        @Override
        public List<List<Token>> build() {
            if (getArgs().size() == getMacro().parameters().size()) {
                getArgs().add(new ArrayList<>());
            }
            return getArgs();
        }

        @Override
        public void addToken(Token token) {
            if (consumeParen(token)) {
                return;
            }

            if (getArgs().size() >= getMacro().parameters().size()) {
                getCurrentArg().add(token);
                return;
            }

            if (token.getValue().equals(",") && getParenLevel() == 1) {
                getArgs().add(new ArrayList<>(getCurrentArg()));
                getCurrentArg().clear();
                return;
            }

            updateParenLevel(token);
            getCurrentArg().add(token);
        }
    }
}
