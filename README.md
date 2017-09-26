# swagger-url-maven-plugin
Swagger url maven plugin is a plugin that allows existing swagger json specification to be exported to text file format with custom path and query parameters with all possible parameter permutations.

Plugin is particulary useful to generate urls used by tools like curl or many performance testing softwares.

Plugin is currently in beta. Contributions are welcomed.

## Example

```
<plugin>
  <groupId>com.github.gpor89</groupId>
  <artifactId>swagger-url-maven-plugin</artifactId>
  <version>1.0-beta1</version>
  <executions>
    <execution>
      <id>prepare-url-file</id>
      <phase>test</phase>
      <goals>
        <goal>generateurls</goal>
      </goals>
      <configuration>
        <swaggerSpec>${basedir}/target/swagger.json</swaggerSpec>
        <outputFile>${basedir}/target/consumer-url-list.txt</outputFile>
        <template>curl --header "Accept: {produces}" -X {httpMethod} {url}</template>
      </configuration>
    </execution>
  </executions>
</plugin>
```

example configuration using swaggers pet store specification generates the following text file:

```
curl --header "Accept: application/json" -X DELETE http://petstore.swagger.io/api/pets/{id}
curl --header "Accept: application/json" -X GET http://petstore.swagger.io/api/pets
curl --header "Accept: application/json" -X GET http://petstore.swagger.io/api/pets/{id}
curl --header "Accept: application/json" -X GET http://petstore.swagger.io/api/pets?limit={limit}
curl --header "Accept: application/json" -X GET http://petstore.swagger.io/api/pets?limit={limit}&tags={tags}
curl --header "Accept: application/json" -X GET http://petstore.swagger.io/api/pets?tags={tags}
curl --header "Accept: application/json" -X POST http://petstore.swagger.io/api/pets
```

Placeholders may be replaced with example values. TBD.
