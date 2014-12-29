package org.basepom.mojo.duplicatefinder;

import java.util.Map;

import org.junit.Test;

public class EnvironmentTest
{
    @Test
    public void testEnvironment()
    {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            System.err.println("Found '" + entry.getKey() + "' -> '" + entry.getValue() + "'");
        }
    }
}
