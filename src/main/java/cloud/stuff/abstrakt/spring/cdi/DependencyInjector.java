package cloud.stuff.abstrakt.spring.cdi;

import cloud.stuff.abstrakt.spring.Trackable;
import cloud.stuff.abstrakt.spring.annotation.*;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Responsible for scanning Spring Framework all Beans in cdi.spring package.
 * This Injects of API Client and Groovy Scripts used for transforming messages services
 *
 *
 * @author Rod Santillan
 * @since 1.0
 */
public class DependencyInjector implements Trackable {
    private static AutowireCapableBeanFactory factory
            = new AnnotationConfigApplicationContext("cdi.spring")
            .getAutowireCapableBeanFactory();

    private static final Log logger = LogFactory.getLog(DependencyInjector.class);

    private String correlationId;
    private static DependencyInjector INSTANCE;

    public static DependencyInjector getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DependencyInjector();
        return INSTANCE;
    }
    private Object getAnnotatedFieldValue(Class<?> clazz, Class<? extends Annotation> annotation) throws IllegalArgumentException, IllegalAccessException {
        for (Field field : clazz.getFields()) {
            if(field.isAnnotationPresent(annotation))
                return field.get(field.getName());
        }
        return null;
    }

    /**
     * Injects of API Client and Groovy Scripts Shell
     *
     *
     * @author Rod Santillan
     * @since 1.0
     */
    public void inject(Object obj) {
        List<Field> fields = new ArrayList<>();
        Class clazz = obj.getClass();

        while (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        for (Field field : fields) {


            if (field.isAnnotationPresent(ApiClient.class)) {

                try {
                    ProxyApiClient client;
                    try {
                        String baseUrl = getAnnotatedFieldValue(field.getType(), BaseUrl.class).toString();
                        String path = field.getType().getAnnotation(Path.class).value();

                        if (field.getType().isAnnotationPresent(Oauth2.class)) {
                            String accessTokenUri = getAnnotatedFieldValue(field.getType(), AccessTokenUri.class).toString();
                            String clientId = getAnnotatedFieldValue(field.getType(), ClientId.class).toString();
                            String grantType = getAnnotatedFieldValue(field.getType(), GrantType.class).toString();
                            String scope = getAnnotatedFieldValue(field.getType(), Scope.class).toString();
                            String clientSecret = getAnnotatedFieldValue(field.getType(), ClientSecret.class).toString();
                            client = new ProxyApiClient(baseUrl + path
                                    , accessTokenUri, clientId
                                    , grantType, scope, clientSecret);
                        } else
                            client = new ProxyApiClient(baseUrl + path);
                    } catch (Exception ex) {
                        //In case of missing annotation above will result a null pointer and we inform people about it in the logs
                        logger.error(ex);
                        //Set default proxy with empty details
                        client = new ProxyApiClient();
                    }

                    field.setAccessible(true);
                    field.set(obj,
                            Proxy.newProxyInstance(field.getType().getClassLoader(),
                                    new Class[]{field.getType()},
                                    client));

                } catch (Exception ex) {
                    logger.error(ex);

                }

            }
            if (field.isAnnotationPresent(MessageTransformer.class)) {
                try {
                    ProxyTransformer proxy = new ProxyTransformer();
                    field.setAccessible(true);
                    field.set(obj,
                            Proxy.newProxyInstance(field.getType().getClassLoader(),
                                    new Class[]{field.getType()},
                                    proxy));

                } catch (Exception ex) {
                    logger.error(ex);
                }

            }
        }
        factory.autowireBean(obj);
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public void setCorrelationId(String id) {
        this.correlationId = id;
    }

    /**
     * Proxy Class for Groovy Shell Implementation
     *
     *
     * @author Rod Santillan
     * @since 1.0
     */
    private class ProxyTransformer implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws IllegalArgumentException {

            Binding binding = new Binding();
            String script = "[]";
            int i = 0;
            for (Parameter parm : method.getParameters()) {

                for (Annotation a : parm.getAnnotations()) {
                    if (a.annotationType().equals(MapperParam.class))
                        binding.setVariable(((MapperParam) a).value(), args[i]);

                    if (a.annotationType().equals(Mapper.class))
                        script = args[i].toString();
                }
                i++;
            }


            GroovyShell shell = new GroovyShell(binding);

            return shell.evaluate(script);
        }
    }

    /**
     * Proxy Class for Spring Framework Resttemplate and Oauth2Resttemplate
     *
     *
     * @author Rod Santillan
     * @since 1.0
     */
    private class ProxyApiClient implements InvocationHandler {
        private String url;
        private boolean usingOauth = false;
        private String accessTokenUri;
        private String clientId;
        private String grantType;
        private String scope;
        private String clientSecret;

        public ProxyApiClient() {
            this.url = "";
            this.accessTokenUri = "";
            this.clientId = "";
            this.grantType = "";
            this.scope = "";
            this.clientSecret = "";
            this.usingOauth = false;
        }

        public ProxyApiClient(String url) {
            this.url = url;
        }

        public ProxyApiClient(String url
                , String accessTokenUri
                , String clientId
                , String grantType
                , String scope
                , String clientSecret) {
            this.url = url;
            this.accessTokenUri = accessTokenUri;
            this.clientId = clientId;
            this.grantType = grantType;
            this.scope = scope;
            this.clientSecret = clientSecret;
            this.usingOauth = true;
        }

        @Override
        public ResponseEntity invoke(Object proxy, Method method, Object[] args)
                throws IllegalArgumentException {

            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            HttpHeaders headers = new HttpHeaders();
            Object payload = null;
            String contentType = method.getAnnotation(ContentType.class).value();

            headers.add(HttpHeaders.CONTENT_TYPE, contentType);

            HttpMethod httpMethod = HttpMethod.GET;
            if (method.isAnnotationPresent(PUT.class))
                httpMethod = HttpMethod.PUT;
            if (method.isAnnotationPresent(PATCH.class))
                httpMethod = HttpMethod.PATCH;
            if (method.isAnnotationPresent(POST.class))
                httpMethod = HttpMethod.POST;
            if (method.isAnnotationPresent(DELETE.class))
                httpMethod = HttpMethod.DELETE;
            if (method.isAnnotationPresent(OPTIONS.class))
                httpMethod = HttpMethod.OPTIONS;
            if (method.isAnnotationPresent(HEAD.class))
                httpMethod = HttpMethod.HEAD;

            int i = 0;
            for (Parameter parm : method.getParameters()) {

                for (Annotation a : parm.getAnnotations()) {
                    if (a.annotationType().equals(QueryParam.class))
                        params.put(((QueryParam) a).value(), args[i]);
                    if (a.annotationType().equals(PathParam.class))
                        params.put(((PathParam) a).value(), args[i]);
                    if (a.annotationType().equals(HeaderParam.class))
                        headers.add(((HeaderParam) a).value(), args[i].toString());
                    if (a.annotationType().equals(Payload.class))
                        payload = args[i];
                }
                i++;
            }

            String path = method.getAnnotation(Path.class).value();
            HttpEntity<Object> entity;
            if (httpMethod.equals(httpMethod.GET))
                entity = new HttpEntity<>(headers);
            else
                entity = new HttpEntity<>(payload, headers);
            if (usingOauth) {
                logger.debug("Sending to host with Oauth 2 " + this.url + path);
                return exchangeWithOauth(this.url + path, httpMethod, entity, params);
            }
            else {
                logger.debug("Sending to host " + this.url + path);
                return exchange(this.url + path, httpMethod, entity, params);
            }
        }

        private ResponseEntity exchangeWithOauth(String url, HttpMethod httpMethod, HttpEntity entity, LinkedHashMap<String, Object> params) {
            ClientCredentialsResourceDetails clientCredentialsResourceDetails = new ClientCredentialsResourceDetails();
            clientCredentialsResourceDetails.setAccessTokenUri(this.accessTokenUri);
            clientCredentialsResourceDetails.setClientId(this.clientId);
            clientCredentialsResourceDetails.setClientSecret(this.clientSecret);
            clientCredentialsResourceDetails.setGrantType(this.grantType);
            clientCredentialsResourceDetails.setScope(Arrays.asList(this.scope.split(",")));

            OAuth2RestTemplate template = new OAuth2RestTemplate(clientCredentialsResourceDetails);
            return template.exchange(url, httpMethod, entity, (Class<?>) Object.class, params);
        }

        private ResponseEntity exchange(String url, HttpMethod httpMethod, HttpEntity entity, LinkedHashMap<String, Object> params) {
            RestTemplate template = new RestTemplate();
            return template.exchange(url, httpMethod, entity, (Class<?>) Object.class, params);
        }
    }

}
