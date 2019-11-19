package io.github.mike10004.containment.lifecycle;

class ContainerRunnables {

    private ContainerRunnables() {}

    public static ContainerInitialPostStartAction<Void> asInitialAction(ContainerPostStartRunnable runnable) {
        return container -> {
            runnable.perform(container);
            return (Void) null;
        };
    }

    public static <T> ContainerPostStartAction<T, T> asPassThru(ContainerPostStartRunnable runnable) {
        return (container, requirement) -> {
            runnable.perform(container);
            return requirement;
        };
    }

    public static ContainerInitialPreStartAction<Void> asInitialAction(ContainerPreStartRunnable runnable) {
        return container -> {
            runnable.perform(container);
            return (Void) null;
        };
    }

    public static <T> ContainerPreStartAction<T, T> asPassThru(ContainerPreStartRunnable self) {
        return (container, requirement) -> {
            self.perform(container);
            return requirement;
        };
    }
}
