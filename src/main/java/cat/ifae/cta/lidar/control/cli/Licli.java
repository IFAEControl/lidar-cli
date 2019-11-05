package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.control.cli.commands.*;
import cat.ifae.cta.lidar.control.cli.session.SessionManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;


@CommandLine.Command(version = "0.0.1", mixinStandardHelpOptions = true, subcommands =
        {LLControl.class,
        Licel.class,
        Motors.class, Monitoring.class, Operation.class, Alarms.class, Config.class, Trace.class})
public class Licli implements Runnable {
    public static SessionManager sm = new SessionManager();

    private Licli() {
        var log_level = Level.ERROR;
        try {
            var level = Integer.parseInt(Helpers.getEnv("LIDAR_VERBOSITY"));
            if (level == 1)
                log_level = Level.WARN;
            else if (level == 2)
                log_level = Level.INFO;
            else if (level == 3)
                log_level = Level.DEBUG;
            else if (level == 4)
                log_level = Level.TRACE;
        } catch (RuntimeException e) {
            // ignore
        }

        Configurator.setAllLevels(LogManager.getRootLogger().getName(), log_level);

        Helpers.configureGRPCLog();

        // If configuration file contains errors we want to make it fail fail ASAP
        new Configuration().checkConfiguration();
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            var client = CommandLine.populateCommand(new Licli(), args);

            CommandLine.run(client, System.err, args);
        } catch (CommandLine.MissingParameterException | CommandLine.UnmatchedArgumentException e) {
            System.err.println(e.getMessage());
        } finally {
            sm.shutdown();
        }
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}