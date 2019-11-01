package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of a container lifecycle monitor that requires manual
 * invocation of stop and remove actions. This monitor keeps track of
 * containers that are created or started
 */
public class ManualContainerMonitor implements ContainerMonitor {

    private final DockerClient client;
    private final Set<String> started, created;
    private final boolean removeWithForce;

    public ManualContainerMonitor(DockerClient client) {
        this.client = checkNotNull(client, "client");
        this.created = Collections.synchronizedSet(new LinkedHashSet<>());
        this.started = Collections.synchronizedSet(new LinkedHashSet<>());
        removeWithForce = true;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected <T> T execSyncCommand(Function<DockerClient, SyncDockerCmd<T>> command) {
        return command.apply(client).exec();
    }

    protected void stopContainer(String containerId) {
        execSyncCommand(client -> client.stopContainerCmd(containerId));
        started.remove(containerId);
    }

    protected void removeContainer(String containerId) {
        execSyncCommand(client -> client.removeContainerCmd(containerId).withForce(removeWithForce));
        created.remove(containerId);
    }

    public interface ContainerActionErrorListener extends BiConsumer<String, Exception> {}

    protected interface ContainerAction {
        void perform(String containerId) throws Exception;
    }

    protected void performChecked(String containerId, ContainerActionErrorListener errorListener, ContainerAction action) {
        try {
            action.perform(containerId);
        } catch (Exception e) {
            errorListener.accept(containerId, e);
        }
    }

    public void removeAll(ContainerActionErrorListener errorListener) {
        List<String> created = Lists.reverse(Arrays.asList(this.created.toArray(new String[0])));
        for (String containerId : created) {
            performChecked(containerId, errorListener, this::removeContainer);
        }
    }

    public void stopAll(ContainerActionErrorListener errorListener) {
        List<String> started = Lists.reverse(Arrays.asList(this.started.toArray(new String[0])));
        for (String containerId : started) {
            performChecked(containerId, errorListener, this::stopContainer);
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
