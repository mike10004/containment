package io.github.mike10004.containment;

public interface ContainerRunner extends AutoCloseable {

    default RunningContainer run(ContainerParametry parametry) throws ContainmentException {
        return run(parametry, CreationObserver.inactive());
    }

    RunningContainer run(ContainerParametry parametry, CreationObserver observer) throws ContainmentException;

    @Override
    void close() throws ContainmentException;

    interface CreationObserver {

        /**
         * Receives a warning from the creation result.
         * May be invoked multiple times.
         * @param warning the warning message
         */
        void warn(String warning);

        /**
         * Performs steps
         */
        void prepare(String containerId);

        static CreationObserver inactive() {
            return new CreationObserver() {
                @Override
                public void warn(String warning) {
                    // no op
                }

                @Override
                public void prepare(String containerId) {
                    // no op
                }

                @Override
                public String toString() {
                    return "CreationObserver{INACTIVE}";
                }
            };
        }
    }
}
