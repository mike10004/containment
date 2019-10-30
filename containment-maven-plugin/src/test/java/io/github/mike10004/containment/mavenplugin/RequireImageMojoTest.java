package io.github.mike10004.containment.mavenplugin;


import com.google.common.collect.Maps;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RequireImageMojoTest
{
    @Rule
    public MojoRule rule = new MojoRule()
    {
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
        File pom = new File( getClass().getResource("/test-projects/project-to-test/pom.xml" ).toURI()).getParentFile();
        assertTrue(pom.isDirectory());
        RequireImageMojo mojo = (RequireImageMojo) rule.lookupConfiguredMojo(pom, RequireImageMojo.GOAL);
        assertNotNull(mojo);
        assertNotNull("project", mojo.getProject());
        String absentImageActionValue = ( String ) rule.getVariableValueFromObject( mojo, "absentImageAction");
        assertNotNull("absentImageAction value", absentImageActionValue );
        AbsentImageAction action = AbsentImageAction.valueOf(absentImageActionValue);
        assertNotNull(AbsentImageDirective.parse(absentImageActionValue));
        System.out.format("getAbsentImageAction: %s%n", mojo.getAbsentImageAction());
        assertEquals(absentImageActionValue, mojo.getAbsentImageAction());
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
        assertEquals("buildArgs", toProperties("FOO", "BAR"), p.buildArgs);
        assertEquals("labels", toProperties("foo", "bar"), p.labels);
    }

    private static Properties toProperties(String...namesAndValues) {
        Properties p = new Properties();
        for (int i = 0; i < namesAndValues.length; i+=2) {
            p.setProperty(namesAndValues[i], namesAndValues[i + 1]);
        }
        return p;
    }


}

