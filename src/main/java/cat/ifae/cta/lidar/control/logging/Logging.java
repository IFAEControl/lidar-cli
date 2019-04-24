package cat.ifae.cta.lidar.control.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Logging {

    private Logger logger;

    public Logging(Class c) {

        logger = LogManager.getLogger(c);

    }

    public void error(String message) {
        logger.error(message);
    }

    public void warn(String m) {
        logger.warn(m);
    }

    public void info(String m) {
        logger.info(m);
    }

    public void debug(String m) {
        logger.info(m);
    }

    public void trace(String m) {
        logger.trace(m);
    }
}

