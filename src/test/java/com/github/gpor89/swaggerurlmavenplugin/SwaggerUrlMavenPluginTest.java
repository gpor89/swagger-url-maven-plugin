package com.github.gpor89.swaggerurlmavenplugin;


import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class SwaggerUrlMavenPluginTest {


    //@Ignore //just for demonstration
    @Test
    public void testYaml() throws MojoExecutionException {

        File f = new File(SwaggerUrlMavenPluginTest.class.getClassLoader().getResource("petstore.yaml").getFile());
        File o = new File("target/testStatSpec.txt");

        SwaggerUrlMavenPluginMojo m = new SwaggerUrlMavenPluginMojo(f, o, "test",
            "{produces} {httpMethod} {url} {formParams}");

        m.execute();
    }

}
