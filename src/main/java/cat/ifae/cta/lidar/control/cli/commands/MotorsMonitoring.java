package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.MotorsMonitoringGrpc;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "motors_monitoring", description = "Motors monitoring commands", mixinStandardHelpOptions = true)
public class MotorsMonitoring implements Runnable {
    private MotorsMonitoringGrpc.MotorsMonitoringBlockingStub blockingStub;

    @Override
    public final void run() {
        blockingStub = MotorsMonitoringGrpc.newBlockingStub(Licli.sm.getCh());
        blockingStub = Licli.sm.addMetadata(blockingStub);

        CommandLine.populateCommand(this);

        try {
            var req = Null.newBuilder().build();
            System.out.println(blockingStub.readEncoders(req));
        } catch(RuntimeException e) {
            System.out.println(e.toString());
        }
    }
}
