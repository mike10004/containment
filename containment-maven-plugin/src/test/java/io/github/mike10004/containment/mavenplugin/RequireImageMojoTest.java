package io.github.mike10004.containment.mavenplugin;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RequireImageMojoTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable 
        {
        }

        @Override
        protected void after()
        {
        }
    };

    @Test
    public void test_minimal_DefaultValuesOk() throws Exception {
        File pom = new File( getClass().getResource("/test-projects/minimal/pom.xml" ).toURI()).getParentFile();
        assertTrue(pom.isDirectory());
        RequireImageMojo mojo = (RequireImageMojo) rule.lookupConfiguredMojo(pom, RequireImageMojo.GOAL);
        assertNotNull(mojo);
        assertNotNull("project", mojo.getProject());
        String imageName = (String) rule.getVariableValueFromObject(mojo, "imageName");
        assertNotNull("imageName", imageName);
        String absentImageActionValue = ( String ) rule.getVariableValueFromObject( mojo, "absentImageAction");
        assertNotNull("absentImageAction value", absentImageActionValue );
        AbsentImageAction action = AbsentImageAction.valueOf(absentImageActionValue);
        AbsentImageDirective directive = AbsentImageDirective.parse(absentImageActionValue);
        assertEquals(action, directive.action);
        System.out.format("getAbsentImageAction: %s%n", mojo.getAbsentImageAction());
        assertEquals(absentImageActionValue, mojo.getAbsentImageAction());
    }

    @Test
    public void test_skipIfAny_oneTrue() throws Exception {
        RequireImageMojo mojo = test_skipIf("skip-if-any-1", true);
        assertNull(mojo.getSkipIfAll());
        assertNotNull(mojo.getSkipIfAny());
    }

    @Test
    public void test_skipIfAny_zeroTrue() throws Exception {
        RequireImageMojo mojo = test_skipIf("skip-if-any-2", false);
        assertNull(mojo.getSkipIfAll());
        assertNotNull(mojo.getSkipIfAny());
    }

    @Test
    public void test_skipIfAll_allTrue() throws Exception {
        RequireImageMojo mojo = test_skipIf("skip-if-all-1", true);
        assertNotNull(mojo.getSkipIfAll());
        assertNull(mojo.getSkipIfAny());
    }

    @Test
    public void test_skipIfAll_oneFalse() throws Exception {
        RequireImageMojo mojo = test_skipIf("skip-if-all-2", false);
        assertNotNull(mojo.getSkipIfAll());
        assertNull(mojo.getSkipIfAny());
    }

    private RequireImageMojo test_skipIf(String artifactId, boolean expected) throws Exception {
        String resourcePath = String.format("/test-projects/%s/pom.xml", artifactId);
        File pom = new File( getClass().getResource(resourcePath).toURI()).getParentFile();
        assertTrue(pom.isDirectory());
        RequireImageMojo mojo = (RequireImageMojo) rule.lookupConfiguredMojo(pom, RequireImageMojo.GOAL);
        assertNotNull(mojo);
        assertNotNull("project", mojo.getProject());
        assertEquals("skip", expected, mojo.isSkipMojoExecution());
        return mojo;
    }

    @Test
    public void test_allParametersAssigned_buildParametry() throws Exception {
        File pom = new File( getClass().getResource("/test-projects/all-parameters-assigned/pom.xml" ).toURI()).getParentFile();
        assertTrue(pom.isDirectory());
        RequireImageMojo mojo = (RequireImageMojo) rule.lookupConfiguredMojo(pom, RequireImageMojo.GOAL);
        assertNotNull("mojo", mojo);
        RequireImageParametry p = mojo.buildParametry();
        assertEquals("name", "oogabooga/not-a-real-docker-image:latest", p.name);
        assertEquals("buildTimeout", Duration.ofMinutes(5), p.buildTimeout);
        assertEquals("pullTimeout", Duration.ofSeconds(30), p.pullTimeout);
        assertEquals("buildArgs", toMap("FOO", "BAR"), p.buildArgs);
        assertEquals("labels", toMap("foo", "bar", "baz", "gaw"), p.labels);
    }

    private static Map<String, String> toMap(String...namesAndValues) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i+=2) {
            m.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return m;
    }


}

