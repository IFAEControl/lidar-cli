package cat.ifae.cta.lidar.control.config;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;


public class Config {

    private final static Logger logger = LogManager.getLogger(Config.class);
    private final static String version = "0.1";
    private final static AppDirs appDirs = AppDirsFactory.getInstance();

    private Properties prop = new Properties();
    private Properties default_prop = new Properties();

    private String properties_file_path;
    private String properties_file_name;

    public Config(Program p, Component c) {
        var cfg_dir = appDirs.getUserConfigDir("lidar", version, "ifae");

        properties_file_name = MessageFormat.format("{0}.properties", c.name().toLowerCase());
        properties_file_path = MessageFormat.format("{0}/{1}/{2}",
                cfg_dir, p.name().toLowerCase(), properties_file_name);
    }

    public void load() throws IOException {
        var l = getClass().getClassLoader();
        try (var inputStream = l.getResourceAsStream(properties_file_name)) {
            logger.trace("Loading default properties");
            if (inputStream == null) {
                logger.warn(properties_file_name + ": not found in the classpath");
                //throw new FileNotFoundException(properties_file_name + ": not found in the classpath");
            } else {
                default_prop.load(inputStream);
            }
        }

        try (var reader = new FileReader(new File(properties_file_path))) {
            logger.trace("Loading user properties: " + properties_file_path);
            prop.load(reader);
        } catch (FileNotFoundException e) {
            logger.warn(e);
        }
    }

    public String getRaw(String name) {
        var user_val = prop.getProperty(name);
        if(user_val != null)
            return user_val;

        logger.trace(name + " not found in user properties file, trying default properties");
        var default_val = default_prop.getProperty(name);
        if(default_val == null)
            throw new RuntimeException(name + ": is not defined anywhere");

        return default_val;
    }

    public String getString(String name) {
        return getRaw(name);
    }

    public Float getFloat(String name) {
        var v = getString(name);
        return Float.valueOf(v);
    }

    public Integer getInteger(String name) {
        var v = getString(name);
        return Integer.valueOf(v);
    }
}