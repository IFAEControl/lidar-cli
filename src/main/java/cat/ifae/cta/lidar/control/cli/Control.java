package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.commands.*;
import picocli.CommandLine;


@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {Arm.class, Dac.class, Drivers.class, HotWind.class,
        Laser.class, Licel.class, Micro.class, Monitoring.class, Motors.class, Relay.class, Sensors.class, Telescope.class})
public class Control implements Runnable {
    public static gRPCControl grpc;

    private Control() {
        Helpers.configureGRPCLog();

        // XXX
        var password = System.getenv("LIDAR_PASSWORD");
        if(password == null || password.isBlank())
            throw new RuntimeException("LIDAR_PASSWORD env variable is not set");

        var ip = System.getenv("LIDAR_ADDR");
        if(ip == null || ip.isBlank())
            throw new RuntimeException("LIDAR_ADDR env variable is not set");

        grpc = new gRPCControl(ip, 50051, password);
    }

    public static void main(String[] args) throws InterruptedException {
        Control client = CommandLine.populateCommand(new Control(), args);

        CommandLine.run(client, System.err, args);
        grpc.shutdown();
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}