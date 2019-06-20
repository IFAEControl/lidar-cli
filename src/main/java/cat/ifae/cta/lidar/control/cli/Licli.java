package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.commands.*;
import cat.ifae.cta.lidar.control.cli.session.SessionManager;
import picocli.CommandLine;


@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {LLCArm.class, LLCDac.class, LLCDrivers.class, LLCHotWind.class,
        LLCLaser.class, Licel.class, Monitoring.class, Motors.class, LLCRelay.class, LLCSensors.class, Operation.class,
        Alarms.class})
public class Licli implements Runnable {
    public static SessionManager sm = new SessionManager();

    private Licli() {
        Helpers.configureGRPCLog();
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            var client = CommandLine.populateCommand(new Licli(), args);

            CommandLine.run(client, System.err, args);
        } finally {
            sm.shutdown();
        }
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}