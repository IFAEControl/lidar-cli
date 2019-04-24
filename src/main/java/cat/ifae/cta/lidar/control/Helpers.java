package cat.ifae.cta.lidar.control;

import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

public class Helpers {
    public static String[] split(String s, int num) {
        return split(s, num, ":");
    }

    public static String[] split(String s, int num, String sep) {
        if (num <= 0)
            throw new RuntimeException("Invalid number");

        String[] components = s.split(sep);
        if (components.length != num)
            throw new RuntimeException("Invalid format");

        return components;
    }

    public static void configureGRPCLog() {
        try {
            var prop = new Properties();
            var in = Helpers.class.getClassLoader().getResourceAsStream("grpc.properties");
            prop.load(in);

            PropertyConfigurator.configure(prop);

            in.close();
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }

}