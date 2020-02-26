package iot.abstrakt.spring.azure.function;

import iot.abstrakt.spring.management.Trackable;

import iot.abstrakt.spring.management.DependencyInjector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

public abstract class AbstractFunction  implements Trackable {
    private String uuid = UUID.randomUUID().toString();

    public AbstractFunction(){
        try {
            DependencyInjector.getInstance().inject(this);
        }catch (Exception ex){
            System.out.println(ex);
        }
    }

    @Override
    public String getCorrelationId(){
        return uuid;
    }

    @Override
    public void setCorrelationId(String id) {
        this.uuid = id;
    }

}
