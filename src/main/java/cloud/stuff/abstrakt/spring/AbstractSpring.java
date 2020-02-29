package cloud.stuff.abstrakt.spring;

import cloud.stuff.abstrakt.spring.cdi.DependencyInjector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.UUID;

/**
 * Abstract Spring Framework base class for resources that would like to use spring framework and other services made specifically for this framework
 * such as @ApiClient and @MessageTransformer.
 *
 * note: All Spring Framework related Beans should be under the 'cdi.spring' package
 *
 * @author Rod Santillan
 * @since 1.0
 */
public abstract class AbstractSpring implements Trackable {

    private String uuid = UUID.randomUUID().toString();
    private static final Log logger = LogFactory.getLog(DependencyInjector.class);

    public AbstractSpring(){
        try {
            DependencyInjector.getInstance().inject(this);
        }catch (Exception ex){
            logger.debug(ex);
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
