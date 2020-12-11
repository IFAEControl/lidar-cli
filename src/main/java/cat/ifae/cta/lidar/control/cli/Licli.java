package cat.ifae.cta.lidar.control.cli;

import cat.ifae.cta.lidar.Helpers;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.SystemGrpc;
import cat.ifae.cta.lidar.control.cli.commands.*;
import cat.ifae.cta.lidar.control.cli.session.SessionManager;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(version = Configuration.VERSION, mixinStandardHelpOptions = true, subcommands =
        {Admin.class, LLControl.class, Licel.class, SystemInfo.class,
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

        try {
            var stub = SystemGrpc.newBlockingStub(Licli.sm.getCh());
            stub = Licli.sm.addMetadata(stub);
            var req = Null.newBuilder().build();
            stub.checkCommunication(req);
        } catch(StatusRuntimeException e) {
            if(e.getStatus().getCode().equals(Status.Code.UNAVAILABLE))
                System.err.println("Error: Could not connect to server. Note that the " +
                                           "server can take up to 10 minutes to start. ");
            System.exit(1);
        }

    }

    public static void main(String[] args) throws InterruptedException {
        try {
            var client = CommandLine.populateCommand(new Licli(), args);

            CommandLine.run(client, System.err, args);
        } catch (CommandLine.MissingParameterException | CommandLine.UnmatchedArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("To gather debug information please run: cd " +
                                       "/path/to/licli/repo/misc && bash ./retrieve_debug_info.sh");
        } finally {
            sm.shutdown();
        }
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}