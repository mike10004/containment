package io.github.mike10004.containment.core;

import io.github.mike10004.containment.dockerjava.DjDockerManager;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public abstract class DjManagedTestBase {

    protected DjDockerManager dockerManager;

    @Before
    public void setUp() {
        dockerManager = TestDockerManager.getInstance();
    }
}
