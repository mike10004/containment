package io.github.mike10004.containment;

class UnitTestRunnableContainer implements RunnableContainer {

    private final ContainerInfo info;

    UnitTestRunnableContainer(ContainerInfo info) {
        this.info = info;
    }

    @Override
    public ContainerInfo info() {
        return info;
    }

    @Override
    public void execute(PreStartAction preStartAction) throws ContainmentException {
        System.out.format("%s.execute(%s)%n", getClass().getSimpleName(), preStartAction);
    }

    @Override
    public RunningContainer start() throws ContainmentException {
        return new UnitTestRunningContainer(info);
    }

    @Override
    public void close() throws ContainmentException {
    }
}
