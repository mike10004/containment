package io.github.mike10004.containment.lifecycle;

import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.RunningContainer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ContainerResourceBuilderTest {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void buildLocalDependency() throws Exception {
        List<LifecycleEvent> events = Collections.synchronizedList(new ArrayList<>());
        ContainerParametry parametry = ContainerParametry.builder(Tests.getAlpineImage())
                .commandToWaitIndefinitely()
                .build();
        File preStartActionFile = File.createTempFile("ContainerDependencyBuilderTest", ".tmp", temporaryFolder.getRoot());
        String expectedFile1Pathname = "/tmp/" + preStartActionFile.getName();
        // TODO consider revising to avoid creating a globally-managed container;
        //      we we explicitly finish the lifecycle, so we're probably fine with an unmanaged container
        Lifecycle<RunningContainer> stack = ContainerLifecycles.builderOfLifecyclesOfGloballyManagedContainers()
                .creating(parametry)
                .runPre(container -> container.copier().copyToContainer(preStartActionFile, "/tmp/"))
                .runPost(container -> container.executor().execute("touch", "/tmp/file2.tmp"))
                .finishWithContainer();
        LifecycledResource<RunningContainer> dependency = LifecycledResource.builder()
                .eventListener(events::add)
                .buildResource(stack);
        RunningContainer container = dependency.request().require();
        try {
            ContainerSubprocessResult<String> result1 = container.executor().execute("ls", "-l", expectedFile1Pathname);
            ContainerSubprocessResult<String> result2 = container.executor().execute("ls", "-l", "/tmp/file2.tmp");
            checkClean("result1", result1);
            checkClean("result2", result2);
        } finally {
            dependency.finishLifecycle();
            // TODO check for actual stoppage and removal
        }
        Set<LifecycleEvent.Category> eventCats = events.stream().map(LifecycleEvent::getCategory).collect(Collectors.toSet());
        assertEquals("events", EnumSet.of(
                LifecycleEvent.Category.COMMISSION_STARTED,
                LifecycleEvent.Category.COMMISSION_SUCCEEDED,
                LifecycleEvent.Category.PROVIDE_STARTED,
                LifecycleEvent.Category.PROVIDE_COMPLETED,
                LifecycleEvent.Category.FINISH_STARTED,
                LifecycleEvent.Category.FINISH_COMPLETED
                ), eventCats);
    }

    private void checkClean(String message1, ContainerSubprocessResult<String> result) {
        System.out.println(result);
        assertEquals(message1 + ": exit code from " + result, 0, result.exitCode());
    }
}