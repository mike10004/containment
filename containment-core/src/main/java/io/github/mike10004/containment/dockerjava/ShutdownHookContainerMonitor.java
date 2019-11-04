package io.github.mike10004.containment.dockerjava;

import com.github.dockerjava.api.DockerClient;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of a container monitor that adds a JVM shutdown hook
 * to stop and remove containers. Instantiation of this class should
 * be kept to a minimum
 */
public class ShutdownHookContainerMonitor extends ManualContainerMonitor {

    public static final String SYSPROP_VERBOSE = "containment-core.shutdownHook.verbose";

    private static final Logger log = Logger.getLogger(ShutdownHookContainerMonitor.class.getName());

    private final Supplier<DockerClient> clientConstructor;
    private final ShutdownHook shutdownHook;
    private volatile boolean shutdownHookAdded;
    private static final Object shutdownHookAddLock = new Object();
    private final ContainerActionErrorListener errorListener;

    public ShutdownHookContainerMonitor(Supplier<DockerClient> clientConstructor) {
        super();
        this.clientConstructor = requireNonNull(clientConstructor);
        this.shutdownHook = new ShutdownHook();
        errorListener = (containerId, e) -> {
            String message = String.format("failed on action involving container %s", containerId);
            log.log(Level.SEVERE, message, e);
            report(message, e);
        };
    }

    private static void report(@Nullable String message, @Nullable Throwable e) {
        if (isVerbose()) {
            if (message != null) {
                System.err.format("ShutdownHookContainerMonitor report: %s%n", message);
            }
            if (e != null) {
                System.err.format("ShutdownHookContainerMonitor exception%n");
                e.printStackTrace(System.err);
            }
        }
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
            try (DockerClient client = clientConstructor.get()) {
                stopAll(client, errorListener);
                removeAll(client, errorListener);
            } catch (IOException e) {
                report(this + " failed to close DockerClient", e);
            }
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
