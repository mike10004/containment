package io.github.mike10004.containment.junit4;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

public class SharedGlobalContainerRule2Test extends SharedGlobalContainerRuleTestBase {

    @ClassRule
    public static final TestRule checkRule = new ExternalResource() {
        @Override
        protected void after() {
            checkEventsOnTearDown();
        }
    };

}
