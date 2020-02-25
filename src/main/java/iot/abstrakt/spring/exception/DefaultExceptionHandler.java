package iot.abstrakt.spring.exception;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        String response = "Unexpected error occurred";
        Error error = new Error();
        error.setId(UUID. randomUUID().toString());
        error.setMessage(response);
        ObjectMapper mapper = new ObjectMapper();

        try{
            response = mapper.writeValueAsString(error);
        }catch(Exception ex){

        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response).type(MediaType.APPLICATION_JSON)
                .build();
    }

    class Error {

        private String id;
        private String message;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}