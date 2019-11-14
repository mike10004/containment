package io.github.mike10004.containment.lifecycle;

interface ExceptionReaction {

    void react(Exception exception);

    ExceptionReaction WRAP_WITH_RUNNABLE = e -> {
        throw new WrappedAutocloseException(e);
    };
}
