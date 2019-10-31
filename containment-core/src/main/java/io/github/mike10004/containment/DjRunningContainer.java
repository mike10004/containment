package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DjRunningContainer implements RunningContainer {

    private final DockerClient client;
    private final String containerId;
    private final LoadingCache<Datum, String> cache;

    public DjRunningContainer(DockerClient client, String containerId) {
        this.client = client;
        this.containerId = containerId;
        cache = CacheBuilder.newBuilder().build(new CacheLoader<Datum, String>() {
            @Override
            public String load(@SuppressWarnings("NullableProblems") Datum key) throws ContainmentException {
                return execute(key);
            }
        });
    }

    private enum Datum {
        PS,
        INSPECT
    }

    @Override
    public String id() {
        return containerId;
    }

    @Override
    public List<PortMapping> fetchPorts() throws ContainmentException {
        try {
            String json = cache.get(Datum.PS);
            return DockerPsContent.of(json).parsePortMappings();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ContainmentException) {
                throw (ContainmentException) e.getCause();
            }
            throw new ContainmentException(e);
        }
    }

    private String execute(Datum d) throws ContainmentException {
        switch (d) {
            case PS:
                return executePs_(id());
            case INSPECT:
                throw new UnsupportedOperationException("inspect is not yet implemented");
            default:
                throw new IllegalArgumentException(String.valueOf(d));
        }
    }

    protected DockerSubprocessExecutorBase.SubprocessConfig getSubprocessConfig() {
        return DockerSubprocessExecutorBase.emptySubprocessConfig();
    }

    private String executePs_(final String containerId) throws ContainmentException {
        return new DockerPsExecutor(getSubprocessConfig()).describeProcess(containerId);
    }

    @Override
    public void close() throws ContainmentException {
        try {
            try {
                client.stopContainerCmd(id())
                        .withTimeout(1)
                        .exec();
            } catch (NotFoundException e) {
                // probably means container was terminated through other means
            }
        } catch (com.github.dockerjava.api.exception.DockerException e) {
            throw new ContainmentException(e);
        }
    }

    @Override
    public <C extends Consumer<? super byte[]>> C followStdout(C consumer) throws ContainmentException {
        return followStream(ProcessOutputStreamType.stdout, consumer);
    }

    @Override
    public <C extends Consumer<? super byte[]>> C followStderr(C consumer) throws ContainmentException {
        return followStream(ProcessOutputStreamType.stderr, consumer);
    }

    private <C extends Consumer<? super byte[]>> C followStream(ProcessOutputStreamType stream, C consumer) throws ContainmentException {
        LogContainerCmd logStdoutCmd = client.logContainerCmd(id())
                .withFollowStream(true)
                .withStdOut(stream.isStdout())
                .withStdErr(stream.isStderr())
                .withTailAll();
        try {
            logStdoutCmd.exec(new LogCallback(consumer));
            return consumer;
        } catch (DockerException e) {
            throw new ContainmentException(e);
        }
    }

    private static class LogCallback extends ResultCallbackTemplate<ResultCallback<Frame>, Frame> {

        private final Consumer<? super byte[]> consumer;

        private LogCallback(Consumer<? super byte[]> consumer) {
            this.consumer = requireNonNull(consumer, "byte consumer");
        }

        @Override
        public void onNext(Frame object) {
            consumer.accept(object.getPayload());
        }
    }
}
