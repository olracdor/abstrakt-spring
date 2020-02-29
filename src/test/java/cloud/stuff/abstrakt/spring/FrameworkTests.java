package cloud.stuff.abstrakt.spring;

import cdi.spring.TestComponent;

import cloud.stuff.abstrakt.spring.annotation.*;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.*;

import org.mockserver.model.Header;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

public class FrameworkTests {


    private static ClientAndServer mockServer;

    @BeforeClass
    public static void startServer() {
        mockServer = startClientAndServer(8080);
    }


    @Test
    public void testFramework(){

        startMockForRest();
        startMockForOauth();
        class Function extends AbstractSpring {

            @ApiClient
            SampleService service;

            @MessageTransformer
            Transformer transformer;

            //Test Spring DI
            @Autowired
            TestComponent testComponent;

            public void process() {

                assertEquals(testComponent.getTestResponse(), "TestResult");
                assertEquals(transformer.transform(1, "id"), 1);
                ResponseEntity response = service.findAll("{}");
                LinkedHashMap entity = (LinkedHashMap) response.getBody();
                assertEquals(entity.get("name"), "value");
            }


        }
        Function function = new Function();
        function.process();

    }

    private void startMockForRest(){
        new MockServerClient("127.0.0.1", 8080)
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/api/v1/findall"),
                        exactly(1)
                )

                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(Header.header("Content-Type","application/json"))
                                .withBody("{ \"name\": \"value\" }")
                                .withDelay(TimeUnit.SECONDS, 1)
                );
    }

    private void startMockForOauth() {
        new MockServerClient("127.0.0.1", 8080)
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/oauth")
                )

                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(Header.header("Content-Type", "application/json"))
                                .withBody("{\n" +
                                        "  \"access_token\":\"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3\",\n" +
                                        "  \"token_type\":\"bearer\",\n" +
                                        "  \"expires_in\":3600,\n" +
                                        "  \"refresh_token\":\"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk\",\n" +
                                        "  \"scope\":\"create\"\n" +
                                        "}")
                                .withDelay(TimeUnit.SECONDS, 1)
                );
    }

    public interface Transformer {

        int transform(@MapperParam("id") Integer id, @Mapper String mapper);
    }

    @Path("/api/v1")
    public interface SampleService {

        @BaseUrl
        String baseUrl = "http://127.0.0.1:8080";

        @POST
        @Path("/findall")
        @ContentType(javax.ws.rs.core.MediaType.APPLICATION_JSON)
        ResponseEntity findAll(@Payload String payload);
    }


    @AfterClass
    public static void stopServer() {
        mockServer.stop();
    }
}