package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of a container lifecycle monitor that requires manual
 * invocation of stop and remove actions. This monitor keeps track of
 * containers that are created or started
 */
public class DjManualContainerMonitor implements DjContainerMonitor {

    private final Set<String> started, created;
    private final boolean removeWithForce;

    public DjManualContainerMonitor() {
        this.created = Collections.synchronizedSet(new LinkedHashSet<>());
        this.started = Collections.synchronizedSet(new LinkedHashSet<>());
        removeWithForce = true;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected <T> T execSyncCommand(DockerClient client, Function<DockerClient, SyncDockerCmd<T>> command) {
        return command.apply(client).exec();
    }

    private abstract static class CommandSupplier<T> implements Function<DockerClient, SyncDockerCmd<T>> {

        private final String description;
        private final String containerId;

        private CommandSupplier(String description, String containerId) {
            this.containerId = containerId;
            this.description = description;
        }

        @Override
        public final SyncDockerCmd<T> apply(DockerClient dockerClient) {
            return apply(dockerClient, containerId);
        }

        protected abstract SyncDockerCmd<T> apply(DockerClient client, String containerId);

        @Override
        public String toString() {
            return new StringJoiner(", ", CommandSupplier.class.getSimpleName() + "[", "]")
                    .add("description='" + description + "'")
                    .add("containerId='" + containerId + "'")
                    .toString();
        }
    }

    protected void stopContainer(DockerClient client, String containerId) {
        execSyncCommand(client, new CommandSupplier<Void>("stop", containerId) {
            @Override
            protected SyncDockerCmd<Void> apply(DockerClient client, String containerId) {
                return client.stopContainerCmd(containerId);
            }
        });
        started.remove(containerId);
    }

    protected void removeContainer(DockerClient client, String containerId) {
        execSyncCommand(client, new CommandSupplier<Void>("rm", containerId) {
            @Override
            protected SyncDockerCmd<Void> apply(DockerClient client, String containerId) {
                return client.removeContainerCmd(containerId).withForce(removeWithForce);
            }
        });
        created.remove(containerId);
    }

    public interface ContainerActionErrorListener extends BiConsumer<String, Exception> {}

    protected interface ContainerAction {
        void perform(DockerClient client, String containerId) throws Exception;
    }

    protected void performChecked(DockerClient client, String containerId, ContainerActionErrorListener errorListener, ContainerAction action) {
        try {
            action.perform(client, containerId);
        } catch (Exception e) {
            errorListener.accept(containerId, e);
        }
    }

    public void removeAll(DockerClient client, ContainerActionErrorListener errorListener) {
        List<String> created = Lists.reverse(Arrays.asList(this.created.toArray(new String[0])));
        for (String containerId : created) {
            performChecked(client, containerId, errorListener, this::removeContainer);
        }
    }

    public void stopAll(DockerClient client, ContainerActionErrorListener errorListener) {
        List<String> started = Lists.reverse(Arrays.asList(this.started.toArray(new String[0])));
        for (String containerId : started) {
            performChecked(client, containerId, errorListener, this::stopContainer);
        }
    }

    @Override
    public void created(String containerId) {
        checkArgument(containerId != null && !containerId.trim().isEmpty());
        created.add(containerId);
    }

    @Override
    public void started(String containerId) {
        checkArgument(containerId != null && !containerId.trim().isEmpty());
        started.add(containerId);
    }

    @Override
    public void stopped(String containerId) {
        checkArgument(containerId != null && !containerId.trim().isEmpty());
        started.remove(containerId);
    }

    @Override
    public void removed(String containerId) {
        checkArgument(containerId != null && !containerId.trim().isEmpty());
        created.remove(containerId);
    }
}
