package iot.abstrakt.spring.management;

public interface Trackable {

    String getCorrelationId();
    void setCorrelationId(String id);
}
