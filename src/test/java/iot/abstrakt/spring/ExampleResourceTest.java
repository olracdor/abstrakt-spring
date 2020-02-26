package iot.abstrakt.spring;

import iot.abstrakt.spring.annotation.Mapper;
import iot.abstrakt.spring.annotation.MapperParam;
import iot.abstrakt.spring.annotation.MessageTransformer;
import iot.abstrakt.spring.azure.function.AbstractFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


public class ExampleResourceTest {


    public void testHelloEndpoint() {

        @Component
        class Function extends AbstractFunction {

            @Autowired
            SampleService service;

            public void process(){

                service.findAll(1);
                 }


        }
        Function m = new Function();
       // m.process();
    }


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

    @Component
    public class SampleService {


        String findAll(int id){
            return "test";
        }
    }
}