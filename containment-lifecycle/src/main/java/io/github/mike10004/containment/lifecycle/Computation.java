package io.github.mike10004.containment.lifecycle;

import java.util.StringJoiner;

class Computation<D> implements Provision<D> {

    private final D provisioned;
    private final Throwable exception;

    protected Computation(D provisioned, Throwable exception) {
        if (provisioned != null && exception != null) {
            throw new IllegalArgumentException("either it was provisioned or an exception was thrown; make up your mind");
        }
        if (provisioned == null && exception == null) {
            throw new IllegalArgumentException("if computation succeeded, provisioned value must be non-null");
        }
        this.provisioned = provisioned;
        this.exception = exception;
    }

    public static <D> Computation<D> succeeded(D value) {
        return new Computation<>(value, null);
    }

    public static <D> Computation<D> failed(Throwable t) {
        return new Computation<>(null, t);
    }

    @Override
    public D value() {
        return provisioned;
    }

    @Override
    public Throwable exception() {
        return exception;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Computation.class.getSimpleName() + "[", "]")
                .add("provisioned=" + provisioned)
                .add("exception=" + exception)
                .toString();
    }
}
