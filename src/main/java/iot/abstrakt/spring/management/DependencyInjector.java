package iot.abstrakt.spring.management;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import iot.abstrakt.spring.annotation.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class DependencyInjector implements Trackable {

    private String correlationId;
    private static DependencyInjector INSTANCE;

    public static DependencyInjector getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DependencyInjector();
        return INSTANCE;
    }

    public void inject(Object obj) {
        List<Field> fields = new ArrayList<>();
        Class clazz = obj.getClass();

        while (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        for (Field field : fields) {
            if (field.isAnnotationPresent(ClientApiService.class)) {
                try {
                    Field service = field.getType().getField("BASE_URL");
                    URI apiUri = new URI(service.get(field).toString());
                    if (obj instanceof Trackable) {

                    }
                    ProxyApiClient client = new ProxyApiClient();

                    field.setAccessible(true);
                    field.set(obj,
                            Proxy.newProxyInstance(field.getType().getClassLoader(),
                                    new Class[]{field.getType()},
                                    client));

                } catch (Exception ex) {
                    System.out.println(ex);
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
                    System.out.println(ex);
                }

            }
        }
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public void setCorrelationId(String id) {
        this.correlationId = id;
    }

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

    private class ProxyApiClient implements InvocationHandler {


        LinkedHashMap<String, HttpMethod> methodMap = null;

        public ProxyApiClient() {

        }


        @Override
        public ResponseEntity invoke(Object proxy, Method method, Object[] args)
                throws IllegalArgumentException {

            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            HttpHeaders headers = new HttpHeaders();
            Object payload = null;

            HttpMethod httpMethod = HttpMethod.GET;
            if(method.isAnnotationPresent(PUT.class))
                httpMethod = HttpMethod.PUT;
            if(method.isAnnotationPresent(PATCH.class))
                httpMethod = HttpMethod.PATCH;
            if(method.isAnnotationPresent(POST.class))
                httpMethod = HttpMethod.POST;
            if(method.isAnnotationPresent(DELETE.class))
                httpMethod = HttpMethod.DELETE;
            if(method.isAnnotationPresent(OPTIONS.class))
                httpMethod = HttpMethod.OPTIONS;
            if(method.isAnnotationPresent(HEAD.class))
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

            RestTemplate template = new RestTemplate();
            HttpEntity<Object> entity;
            if(httpMethod.equals(httpMethod.GET))
                entity = new HttpEntity<>(headers);
            else
                entity = new HttpEntity<>(payload, headers);

            return template.exchange("", httpMethod, entity, (Class<?>) Object.class, params);
        }
    }

}
