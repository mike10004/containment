package io.github.mike10004.containment.core;

import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public abstract class DjManagedTestBase {

    protected DockerClientConfig defaultClientConfig;
    protected DjDockerManager dockerManager;

    @Before
    public void setUp() {
        defaultClientConfig = TestDockerManager.createClientConfig();
        dockerManager = TestDockerManager.getInstance();
    }
}
