package cloud.stuff.abstrakt.spring;

public interface Trackable {

    String getCorrelationId();
    void setCorrelationId(String id);
}
