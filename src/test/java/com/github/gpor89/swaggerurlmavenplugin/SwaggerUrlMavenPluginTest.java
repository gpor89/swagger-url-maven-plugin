package com.github.gpor89.swaggerurlmavenplugin;


import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class SwaggerUrlMavenPluginTest {

    @Test
    public void testSampleSwaggerSpec() throws MojoExecutionException {

        File f = new File(
            SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("api-with-examples.json").getFile());
        File o = new File("target/testSampleSwaggerSpec.txt");

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f, o, "xxx:80",
            "-P {produces} -H {httpMethod} -U {url} -p  {formParams}");

        m.execute();
    }

    @Test
    public void testPetstoreExpandedSpec() throws MojoExecutionException {

        File f = new File(
            SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("petstore-expanded.json").getFile());
        File o = new File("target/testPetstoreExpandedSpec.txt");

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f, o, "xxx:80",
            "-P {produces} -H {httpMethod} -U {url} -p  {formParams}");

        m.execute();
    }

    @Ignore //just for demonstration
    @Test
    public void testStatSpec() throws MojoExecutionException {

        File f = new File(SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("stat.json").getFile());
        File o = new File("target/testStatSpec.txt");

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f, o, "test",
            "{produces} {httpMethod} {url} {formParams}");

        m.execute();
    }

}
