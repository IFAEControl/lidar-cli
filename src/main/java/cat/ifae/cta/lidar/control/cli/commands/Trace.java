package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.PastHours;
import cat.ifae.cta.lidar.TraceGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "trace", description = "Trace", mixinStandardHelpOptions = true)
public class Trace implements Runnable {
    private TraceGrpc.TraceBlockingStub stub;

    @CommandLine.Option(names = "--last-hours")
    int last_hours = 0;

    @Override
    public void run(){
        stub = TraceGrpc.newBlockingStub(Licli.sm.getCh());
        stub = Licli.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(last_hours != 0) {
                PastHours req = PastHours.newBuilder().setHours(last_hours).build();
                System.out.println(stub.getLastTraces(req).getDataList());
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
