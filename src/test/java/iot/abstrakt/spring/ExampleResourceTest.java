package iot.abstrakt.spring;

import io.quarkus.test.junit.QuarkusTest;
import iot.abstrakt.spring.annotation.Mapper;
import iot.abstrakt.spring.annotation.MapperParam;
import iot.abstrakt.spring.annotation.MessageTransformer;
import iot.abstrakt.spring.azure.function.AbstractFunction;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@QuarkusTest
public class ExampleResourceTest {

    @Test
    public void testHelloEndpoint() {
        class Function extends AbstractFunction {

            SampleService service;

            public void process(){ //service.find(1);
                 }

        }
        Function m = new Function();
        m.process();
    }

    @Test
    public void testMapper() {
        class Function extends AbstractFunction {

            @MessageTransformer
            Transformer transformer;

            public void process(){
                transformer.transform(1, "id");
            }

        }
        Function m = new Function();
        m.process();
    }

    public interface Transformer {

        Object transform(@MapperParam("id") Integer id, @Mapper String mapper);
    }

    @Path("/v1/api")
    @RegisterRestClient
    public interface SampleService {

        String url= "http://localhost:9080/movieReviewService";

        @GET
        @Path("{id}")
        @Produces(MediaType.APPLICATION_JSON)
        String find(@PathParam("id") int id);
    }
}