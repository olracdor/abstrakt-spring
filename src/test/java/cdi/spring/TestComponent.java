package cdi.spring;

import org.springframework.stereotype.Component;

@Component
public class TestComponent {

    public String getTestResponse(){
        return "TestResult";
    }
}
