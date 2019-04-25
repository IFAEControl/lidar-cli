package cat.ifae.cta.lidar.control.cli.commands;

import cat.ifae.cta.lidar.Data;
import cat.ifae.cta.lidar.Null;
import cat.ifae.cta.lidar.Raw;
import cat.ifae.cta.lidar.SensorsGrpc;
import cat.ifae.cta.lidar.control.cli.Licli;
import picocli.CommandLine;

@CommandLine.Command(name = "sensors", mixinStandardHelpOptions = true)
public
class Sensors implements Runnable {
    private SensorsGrpc.SensorsBlockingStub stub;

    @CommandLine.Option(names = "-raw", description = "If true get raw data, otherwise get converted data")
    private boolean get_raw = false;

    @CommandLine.Option(names = "-converted", description = "If true get raw data, otherwise get converted data")
    private boolean get_conv = false;

    @CommandLine.ParentCommand
    private Licli parent;

    @Override
    public void run() {
        stub = SensorsGrpc.newBlockingStub(parent.sm.getCh());
        stub = parent.sm.addMetadata(stub);

        CommandLine.populateCommand(this);

        try {
            if(get_raw) getRawData();
            else if(get_conv) getConvertedData();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    private void getRawData() {
        Null req = Null.newBuilder().build();
        Raw resp = stub.getRawData(req);
        System.out.println(resp);

    }

    private void getConvertedData() {
        Null req = Null.newBuilder().build();
        Data resp = stub.getConvertedData(req);
        System.out.println(resp);
    }
}