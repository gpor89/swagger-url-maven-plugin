package com.github.gpor89.swaggerurlmavenplugin;


import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SwaggerUrlMavenPluginTest {

    @Test
    public void testSampleSwaggerSpec() throws MojoExecutionException {

        File f = new File(SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("api-with-examples.json").getFile());

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f);

        m.execute();
    }

    @Test
    public void testPetstoreExpandedSpec() throws MojoExecutionException {

        File f = new File(SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("petstore-expanded.json").getFile());

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f);

        m.execute();
    }

    @Test
    public void testStatSpec() throws MojoExecutionException {

        File f = new File(SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("stat.json").getFile());

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f);

        m.execute();
    }

}
