package com.maroontress.clione.macro;

import java.util.function.Supplier;

import com.maroontress.clione.Token;

/**
    Provides a base implementation of the {@link WrappedToken} interface.

    <p>This abstract class simplifies the creation of new wrapped token types
    by providing a default implementation for most of the methods defined in
    the {@code WrappedToken} and {@link MacroToken} interfaces. Subclasses are
    only required to implement the {@link #isOriginatingFromParameter()}
    method.</p>
*/
public abstract class AbstractWrappedToken implements WrappedToken {

    private final Token token;

    /**
        Constructs a new instance.

        @param token The token to be wrapped.
    */
    public AbstractWrappedToken(Token token) {
        this.token = token;
    }

    /** {@inheritDoc} */
    @Override
    public final Token unwrap() {
        return token;
    }

    /** {@inheritDoc} */
    @Override
    public abstract boolean isOriginatingFromParameter();

    /** {@inheritDoc} */
    @Override
    public boolean addToArguments(MacroArgumentBuilder builder,
            Supplier<PreprocessException> ignored) {
        return builder.addToken(unwrap());
    }

    @Override
    public <T> T apply(MacroTokenVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
