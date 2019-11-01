package io.github.mike10004.containment;

import com.github.dockerjava.api.DockerClient;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of a container monitor that adds a JVM shutdown hook
 * to stop and remove containers. Instantiation of this class should
 * be kept to a minimum
 */
public class ShutdownHookContainerMonitor extends ManualContainerMonitor {

    public static final String SYSPROP_VERBOSE = "containment-core.globalContainerMonitor.verbose";

    private static final Logger log = Logger.getLogger(ShutdownHookContainerMonitor.class.getName());

    private final ShutdownHook shutdownHook;
    private volatile boolean shutdownHookAdded;
    private static final Object shutdownHookAddLock = new Object();
    private final ContainerActionErrorListener errorListener;

    public ShutdownHookContainerMonitor(DockerClient client) {
        super(client);
        this.shutdownHook = new ShutdownHook();
        errorListener = (containerId, e) -> {
            log.log(Level.SEVERE, e, () -> String.format("failed on action involving container %s", containerId));
            if (isVerbose()) {
                System.err.format("failed on action involving container %s%n", containerId);
                e.printStackTrace(System.err);
            }
        };
    }

    private static boolean isVerbose() {
        return Boolean.parseBoolean(System.getProperty(SYSPROP_VERBOSE));
    }

    private void maybeAddShutdownHook() {
        if (shutdownHookAdded) {
            return;
        }
        synchronized (shutdownHookAddLock) {
            if (!shutdownHookAdded) {
                addShutdownHookToRuntime(Runtime.getRuntime(), new Thread(shutdownHook));
                shutdownHookAdded = true;
            }
        }
    }

    protected void addShutdownHookToRuntime(Runtime runtime, Thread thread) {
        runtime.addShutdownHook(thread);
    }

    private final class ShutdownHook implements Runnable {

        @Override
        public void run() {
            stopAll(errorListener);
            removeAll(errorListener);
        }
    }

    @Override
    public void created(String containerId) {
        super.created(containerId);
        maybeAddShutdownHook();
    }

    @Override
    public void started(String containerId) {
        super.started(containerId);
        maybeAddShutdownHook();
    }

}
