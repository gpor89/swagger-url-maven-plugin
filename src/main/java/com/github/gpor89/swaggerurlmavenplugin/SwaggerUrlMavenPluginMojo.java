package com.github.gpor89.swaggerurlmavenplugin;

import com.github.gpor89.swaggerurlmavenplugin.SwaggerUrlMavenPluginMojo.ApiParameter.ParamType;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(name = "generateurls")
public class SwaggerUrlMavenPluginMojo extends AbstractMojo {

    private static Pattern PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

    /**
     * The greeting to display.
     */
    @Parameter(defaultValue = "target/consumerApi.yaml")
    private File swaggerSpec;

    @Parameter(defaultValue = "target/requests.txt")
    private File outputFile;

    @Parameter(defaultValue = "{myHost}")
    private String host;

    @Parameter(defaultValue = "{produces} {httpMethod} {url} {formParams}")
    private String template;

    public SwaggerUrlMavenPluginMojo() {
    }

    protected SwaggerUrlMavenPluginMojo(final File swaggerSpec, final File outputFile, final String host,
        final String template) {
        this.swaggerSpec = swaggerSpec;
        this.outputFile = outputFile;
        this.host = host;
        this.template = template;
    }


    public static Set<Set<String>> queryParamOptions(Set<String> originalSet) {
        Set<Set<String>> sets = new HashSet<Set<String>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }

        //all
        sets.add(originalSet);

        for (String q : originalSet) {
            Set<String> singleQp = new HashSet<String>();
            singleQp.add(q);
            sets.add(singleQp);
        }

        return sets;
    }

    public void execute() throws MojoExecutionException {

        getLog().info(String
            .format("Executing swagger-url-maven-plugin with parameters\n %s\n %s\n %s\n %s", swaggerSpec, outputFile,
                host, template));

        if (swaggerSpec == null || !swaggerSpec.exists() || !swaggerSpec.isFile()) {
            getLog().error("Could not find swagger spec file " + swaggerSpec);
            return;
        }

        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(swaggerSpec);
            Map<String, Object> swaggerSpecNode = yaml.load(inputStream);

            //todo check spec version
            final String swaggerVer = (String) swaggerSpecNode.get("openapi");
            if (swaggerVer == null || !swaggerVer.startsWith("3.")) {
                getLog().warn("Unknown swagger spec version " + swaggerVer);
            }

            final List<Api> apiList = new LinkedList<>();

            List<String> globalSchemes = Arrays.asList("http");
            String globalHost = host;
            if (globalHost == null) {
                final List servers = (List) swaggerSpecNode.get("servers");
                if (servers != null && !servers.isEmpty()) {
                    final Map serv = (Map) servers.get(0);
                    if (serv != null) {
                        globalHost = (String) serv.get("url");
                    }
                }
            }

            Map<String, Map<String, Map<String, Object>>> paths
                = (Map<String, Map<String, Map<String, Object>>>) swaggerSpecNode.get("paths");
            for (Entry<String, Map<String, Map<String, Object>>> path : paths.entrySet()) {
                String apiPath = path.getKey();
                final Map<String, Map<String, Object>> methods = path.getValue();

                for (Entry<String, Map<String, Object>> method : methods.entrySet()) {
                    String httpMethod = method.getKey();
                    final List<Map<String, Object>> parameters = (List<Map<String, Object>>) method.getValue().get(
                        "parameters");

                    Optional<Entry<Object, Map<String, Object>>> response = ((Map<Object, Map<String, Object>>) method
                        .getValue().get("responses")).entrySet().stream().filter(
                        k -> k.getKey().toString().equalsIgnoreCase("default") || (Integer.valueOf(
                            k.getKey().toString()) < 300 && Integer.valueOf(k.getKey().toString()) >= 200)).findFirst();

                    List<String> produces = new LinkedList<>();
                    if (response.isPresent()) {
                        produces = ((Map<String, Object>) response.get().getValue().get("content")).entrySet().stream()
                            .map(k -> k.getKey()).collect(Collectors.toList());
                    }

                    if (produces.isEmpty()) {
                        produces.add("*/*");
                    }

                    final Api api = new Api(globalSchemes, produces, globalHost, "", apiPath, httpMethod.toUpperCase());

                    api.setParameters(parseApiParameters(parameters));

                    apiList.add(api);
                }
            }

            Set<ApiEntry> apiSet = new TreeSet<ApiEntry>();

            for (Api api : apiList) {
                apiSet.addAll(api.getUrlOptions());
            }

            getLog().info("Generating urls...");
            PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
            for (ApiEntry api : apiSet) {
                getLog().info(api.getAsLine(template));
                writer.println(api.getAsLine(template));
            }
            writer.close();

            getLog().info("Urls generated");
        } catch (IOException e) {

        }

    }

    private Set<ApiParameter> parseApiParameters(final List<Map<String, Object>> parameters) {

        Set<ApiParameter> result = new HashSet<>();

        if (parameters == null) {
            return result;
        }

        for (Map<String, Object> param : parameters) {
            final String paramName = (String) param.get("name");
            final ParamType type = ApiParameter.parseParamType((String) param.get("in"));
            final boolean required = param.get("required") == null ? false : (Boolean) param.get("required");
            final String exampleValue = param.get("example") == null ? null : param.get("example").toString();

            final Map<String, Object> schema = (Map<String, Object>) param.get("schema");
            List<String> enumValList = new LinkedList<>();

            if (schema != null && schema.get("enum") != null) {
                ((List<Object>) schema.get("enum")).stream().map(k -> k.toString()).forEach(enumValList::add);
            }

            if (enumValList.isEmpty() && exampleValue != null) {
                //try to get X-example and add
                enumValList.add(exampleValue);
            }

            result.add(new ApiParameter(paramName, type, required, enumValList));
        }

        return result;
    }


    public static class ApiParameter {

        private String paramName;
        private ParamType paramType;
        private boolean required;
        private List<String> values = new LinkedList<>();

        public ApiParameter(String paramName, ParamType type, boolean required, List<String> enumValList) {
            this.paramName = paramName;
            this.paramType = type;
            this.required = required;

            if (enumValList != null) {
                this.values = enumValList;
            }
        }

        public static ParamType parseParamType(String paramType) {
            if ("path".equalsIgnoreCase(paramType)) {
                return ParamType.PATH;
            }
            if ("query".equalsIgnoreCase(paramType)) {
                return ParamType.QUERY;
            }
            if ("formData".equalsIgnoreCase(paramType)) {
                return ParamType.FORM_DATA;
            }

            //todo implement other...
            return null;
        }

        public enum ParamType {
            PATH, QUERY, FORM_DATA;
        }

    }

    public class ApiEntry implements Comparable<ApiEntry> {

        private String url;
        private String httpMethod;
        private String produces;
        private String formParams;

        public ApiEntry(String url) {
            this.url = url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public void setProduces(String produces) {
            this.produces = produces;
        }

        public void setFormParams(final String formParams) {
            this.formParams = formParams;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ApiEntry apiEntry = (ApiEntry) o;

            if (!url.equals(apiEntry.url)) {
                return false;
            }
            if (!httpMethod.equals(apiEntry.httpMethod)) {
                return false;
            }
            return produces != null ? produces.equals(apiEntry.produces) : apiEntry.produces == null;
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + httpMethod.hashCode();
            result = 31 * result + (produces != null ? produces.hashCode() : 0);
            return result;
        }

        public int compareTo(ApiEntry o) {

            int i = this.url.compareTo(o.url);

            if (i == 0) {
                i = this.httpMethod.compareTo(o.httpMethod);
            }

            if (i == 0) {
                i = this.produces == null ? i : this.produces.compareTo(o.produces);
            }

            return i;
        }

        public String getAsLine(String template) {

            String s = template.replaceAll("\\{produces\\}", produces);
            s = s.replaceAll("\\{httpMethod\\}", httpMethod);
            s = s.replaceAll("\\{url\\}", url);
            if (formParams != null) {
                s = s.replaceAll("\\{formParams\\}", formParams);
            } else {
                s = s.replaceAll("\\{formParams\\}", "");
            }

            return s;
        }
    }

    public class Api {

        //contains scheme host and global base paths
        private Set<String> baseUrlSet = new TreeSet<String>();
        //contains produce media types
        private Set<String> produceSet = new TreeSet<String>();
        //contains path params
        private String apiPath;
        //contains method (POST,PUT,GET...)
        private String httpMethod;

        //path param map
        private Map<String, ApiParameter> pathParameterMap = new HashMap<String, ApiParameter>();
        //query param map
        private Map<String, ApiParameter> queryParameterMap = new HashMap<String, ApiParameter>();
        //form param map
        private Map<String, ApiParameter> formParameterMap = new HashMap<String, ApiParameter>();


        public Api(List<String> globalSchemes, List<String> globalProduces, String globalHost, String globalBasePath,
            String apiPath, String httpMethod) {

            if (globalSchemes.isEmpty()) {
                baseUrlSet.add("http://" + globalHost + globalBasePath);
            }

            for (String scheme : globalSchemes) {
                baseUrlSet.add(scheme + "://" + globalHost + globalBasePath);
            }

            if (globalProduces != null) {
                this.produceSet = new TreeSet<String>(globalProduces);
            }
            this.apiPath = apiPath;
            this.httpMethod = httpMethod;
        }

        public void overrideGlobalProduceSetWith(Set<String> produceSet) {
            this.produceSet = produceSet;
        }

        public void setParameters(Set<ApiParameter> apiParameters) {
            for (ApiParameter apiParameter : apiParameters) {
                if (apiParameter.paramType == ParamType.PATH) {
                    pathParameterMap.put(apiParameter.paramName, apiParameter);
                } else if (apiParameter.paramType == ParamType.QUERY) {
                    queryParameterMap.put(apiParameter.paramName, apiParameter);
                } else if (apiParameter.paramType == ParamType.FORM_DATA) {
                    formParameterMap.put(apiParameter.paramName, apiParameter);
                }
            }
        }

        public Set<ApiEntry> getUrlOptions() {

            Set<ApiEntry> methodCallSet = new TreeSet<ApiEntry>();

            //initial set are urls with required query params
            StringBuilder sb = new StringBuilder();
            for (ApiParameter parameter : queryParameterMap.values()) {
                if (parameter.required) {
                    sb.append(parameter.paramName + "={" + parameter.paramName + "}&");
                }
            }
            if (sb.length() > 0) {
                sb.delete(sb.length() - 1, sb.length());
            }

            String queryParam = sb.length() > 0 ? "?" + sb.toString() : "";
            for (String url : baseUrlSet) {
                ApiEntry e = new ApiEntry(url + apiPath + queryParam);
                e.setHttpMethod(httpMethod.toUpperCase());
                methodCallSet.add(e);
            }

            //enrich with produce
            Set<ApiEntry> tmpSet = new HashSet<ApiEntry>();
            for (String produce : produceSet) {
                for (ApiEntry apiEntry : methodCallSet) {
                    apiEntry.setProduces(produce);
                    tmpSet.add(apiEntry);
                }
            }
            if (!produceSet.isEmpty()) {
                methodCallSet = new TreeSet<ApiEntry>(tmpSet);
            }

            //now we multiply set power of optional query params
            Set<String> optionalQueryParams = new TreeSet<String>();
            for (ApiParameter apiParameter : queryParameterMap.values()) {
                if (!apiParameter.required) {
                    optionalQueryParams.add(apiParameter.paramName);
                }
            }

            final Set<Set<String>> qpOptions = queryParamOptions(optionalQueryParams);

            Set<ApiEntry> methodCallSetTmp = new HashSet<ApiEntry>();
            for (Set<String> qpPermutation : qpOptions) {
                StringBuilder qsb = new StringBuilder();
                for (String paramName : qpPermutation) {
                    qsb.append(paramName + "={" + paramName + "}&");
                }
                if (qsb.length() > 0) {
                    qsb.delete(qsb.length() - 1, qsb.length());

                    for (ApiEntry origEn : methodCallSet) {
                        ApiEntry en = new ApiEntry(origEn.url + "?" + qsb.toString());
                        en.setProduces(origEn.produces);
                        en.setHttpMethod(origEn.httpMethod);
                        methodCallSetTmp.add(en);
                    }
                }
            }

            //at this point we have all urls with required params and optional query param permutation
            methodCallSet.addAll(methodCallSetTmp);

            //try to find enum set by param and multiply...
            List<ApiEntry> apiParamStack = new LinkedList<ApiEntry>();
            for (ApiEntry apiEntry : methodCallSet) {
                String apiUrl = apiEntry.url;
                Matcher matcher = PARAM_PATTERN.matcher(apiUrl);
                while (matcher.find()) {
                    String paramName = matcher.group(1);
                    ApiParameter apiParameter;
                    if (queryParameterMap.containsKey(paramName)) {
                        apiParameter = queryParameterMap.get(paramName);
                    } else if (pathParameterMap.containsKey(paramName)) {
                        apiParameter = pathParameterMap.get(paramName);
                    } else {
                        continue;
                    }

                    if (!formParameterMap.isEmpty()) {
                        ApiParameter parameter = formParameterMap.get(formParameterMap.keySet().toArray()[0]);
                        String formParamStr = parameter.paramName + "=" + parameter.values.get(0);
                        apiEntry.setFormParams(formParamStr);
                        //todo support multiple...
                    }

                    for (String enumValue : apiParameter.values) {
                        apiUrl = apiUrl.replaceAll("\\{" + paramName + "\\}", enumValue);

                        ApiEntry e = new ApiEntry(apiUrl);
                        e.setHttpMethod(apiEntry.httpMethod);
                        e.setProduces(apiEntry.produces);
                        e.setFormParams(apiEntry.formParams);
                        if (!apiUrl.contains("{")) {
                            apiParamStack.add(e);
                        }
                    }
                }

                ApiEntry e = new ApiEntry(apiUrl);
                e.setHttpMethod(apiEntry.httpMethod);
                e.setProduces(apiEntry.produces);
                e.setFormParams(apiEntry.formParams);
                if (!apiUrl.contains("{")) {
                    apiParamStack.add(e);
                }


            }

            return new TreeSet<ApiEntry>(apiParamStack);
        }
    }

}
