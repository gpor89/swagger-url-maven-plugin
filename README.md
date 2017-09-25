# swagger-url-maven-plugin
Swagger url maven plugin is a plugin that allows existing swagger json specification to be exported to text file format with custom path and query parameters with all possible parameter permutations.

Plugin is particulary useful to generate urls used by tools like curl or many performance testing softwares.

Plugin is currently in beta. Contributions are welcomed.

##Example

example pet store swagger specification generates the following text file:

```
application/json DELETE http://petstore.swagger.io/api/pets/{id}
application/json GET http://petstore.swagger.io/api/pets
application/json GET http://petstore.swagger.io/api/pets/{id}
application/json GET http://petstore.swagger.io/api/pets?limit={limit}
application/json GET http://petstore.swagger.io/api/pets?limit={limit}&tags={tags}
application/json GET http://petstore.swagger.io/api/pets?tags={tags}
application/json POST http://petstore.swagger.io/api/pets
```
