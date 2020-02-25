package iot.abstrakt.spring.azure.function;

import iot.abstrakt.spring.management.Trackable;

import iot.abstrakt.spring.management.DependencyInjector;

import java.util.UUID;

public abstract class AbstractFunction  implements Trackable {

    private String uuid = UUID.randomUUID().toString();

    public AbstractFunction(){

        DependencyInjector.getInstance().inject(this);
    }

    @Override
    public String getCorrelationId(){
        return uuid;
    }
}
